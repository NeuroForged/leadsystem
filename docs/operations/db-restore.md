# PostgreSQL backup + restore runbook

**Ticket:** LSB-163
**Last verified:** TODO — run the test-restore the first time, then update this date

This document covers backing up and restoring the four PostgreSQL databases
on the Hetzner Coolify host:

- `leadsystem-prod` (the lead system's main DB at `api.alchemizeiq.com`)
- `leadsystem-dev`
- `alchemize-chatbot-prod-db` (UUID `g973nc4r64y3j6p6cgwom1k9` in Coolify)
- `alchemize-chatbot-dev-db` (UUID `g8mppp4ft97xzlhtikchsfvz`)

## Backup architecture

```
                  ┌─────────────────────┐
                  │  Coolify Scheduled  │   one task per DB
                  │       Task          │   (cron: 0 3 * * *)
                  └──────────┬──────────┘
                             │ runs
                             ▼
                  ┌─────────────────────┐
                  │  scripts/pg-backup  │   pg_dump --format=custom
                  │        .sh          │   → gpg --symmetric AES256
                  └──────────┬──────────┘   → sftp upload
                             │
                             ▼
        ┌────────────────────────────────────┐
        │  Hetzner Storage Box (separate     │
        │  failure domain from CX42 server)  │
        │                                    │
        │  backups/                          │
        │    leadsystem-prod-*.dump.gpg      │
        │    leadsystem-dev-*.dump.gpg       │
        │    chatbot-prod-*.dump.gpg         │
        │    chatbot-dev-*.dump.gpg          │
        └────────────────────────────────────┘
```

- Each DB backed up daily at 03:00 UTC.
- Encrypted at rest using AES-256 symmetric (passphrase = `BACKUP_PASSPHRASE`).
- Retention: 30 days (configurable via `RETENTION_DAYS`).
- Stored on a Hetzner Storage Box — separate failure domain from the CX42
  host. If the server is wiped, backups survive.

## First-time setup (one-off)

These steps were not yet performed at the time this doc was written. Do them
once, then never again.

### 1. Create a Hetzner Storage Box

In the Hetzner Cloud console:

1. Storage → Storage Boxes → Create. 100 GB plan is plenty for our needs.
2. Name it `alchemize-backups`. Region: same as the CX42 (Helsinki).
3. After creation, note the hostname (`uXXXXXX.your-storagebox.de`) and your
   username (`uXXXXXX`).
4. Sub-accounts → Add sub-account for the backup script. Restrict to a single
   SSH key (no password login).
5. SSH → Add public key. Generate a dedicated keypair for backups
   (`ssh-keygen -t ed25519 -f backup_id_ed25519 -C "alchemize-backups"`).

Save:
- Storage Box hostname: ___________________
- Backup user: ___________________
- Public key (in Storage Box): ___________________
- Private key (will go into Coolify secrets in step 4): see local `~/.ssh/`

### 2. Generate the encryption passphrase

```bash
openssl rand -base64 48
```

Save this somewhere you can find it again — **without it the backups are
worthless**. Recommend 1Password under "Alchemize / Infrastructure".

### 3. Mount the SSH key into Coolify

Coolify Scheduled Tasks can reference Docker secrets. For each DB service:

1. In Coolify, open the DB service.
2. Settings → Storage → Add new Storage. Mount path: `/run/secrets/backup_ssh_key`.
3. Paste the private key content into the storage value. Mode: 0600.

(Alternative: add the private key as an env var and have the script write it
to disk on each run. Less clean but works without storage mounts.)

### 4. Create a Scheduled Task per DB

For each of the four databases, in Coolify:

1. Open the DB service → Scheduled Tasks → Add Scheduled Task.
2. **Cron**: `0 3 * * *` (daily at 03:00 UTC). Stagger by 15 min if you'd
   rather not have all four run at once: leadsystem-prod 03:00,
   leadsystem-dev 03:15, chatbot-prod 03:30, chatbot-dev 03:45.
3. **Container**: choose `postgres-client:16` from Docker Hub (small image
   with `pg_dump`, `gpg`, `openssh-client` pre-installed). Or run on the same
   container as the DB if Coolify allows.
4. **Command**: `bash /backup/pg-backup.sh`. Mount the
   `scripts/pg-backup.sh` from this repo at `/backup/pg-backup.sh`.
5. **Environment variables** (per-task):

   ```
   PG_HOST=<internal-postgres-service-hostname>
   PG_PORT=5432
   PG_USER=postgres
   PG_PASSWORD=<from Coolify DB secrets>
   PG_DATABASE=<DB name>
   BACKUP_NAME_PREFIX=leadsystem-prod   # change per DB
   STORAGE_BOX_HOST=uXXXXXX.your-storagebox.de
   STORAGE_BOX_USER=uXXXXXX
   SSH_KEY_PATH=/run/secrets/backup_ssh_key
   ENCRYPTION_PASSPHRASE=<the openssl rand value from step 2>
   RETENTION_DAYS=30
   ```

6. **Save + Run now** to verify. Tail the task log — should finish in well
   under 60 seconds for any of our four DBs.

### 5. Verify the first backup

After the first scheduled run completes, SSH into the Storage Box from your
laptop and list the files:

```bash
ssh -i ~/.ssh/backup_id_ed25519 uXXXXXX@uXXXXXX.your-storagebox.de "ls -lh"
```

You should see one `.dump.gpg` per DB, sized between 100 KB and a few MB
depending on the dataset.

## Restoring a backup

This is the procedure you'll follow in an actual incident. Worth running
through it manually once during setup to make sure the SSH key, passphrase,
and pg_restore version all work together.

### 1. Pull the backup from the Storage Box

From a laptop or any throwaway VM with SSH access:

```bash
sftp -i ~/.ssh/backup_id_ed25519 uXXXXXX@uXXXXXX.your-storagebox.de <<'EOF'
ls -l leadsystem-prod-*.dump.gpg
get leadsystem-prod-20260511T030000Z.dump.gpg   # pick the right timestamp
EOF
```

### 2. Decrypt

```bash
gpg --decrypt \
    --passphrase "$ENCRYPTION_PASSPHRASE" \
    --output leadsystem-prod.dump \
    leadsystem-prod-20260511T030000Z.dump.gpg
```

(The passphrase is stored in 1Password under "Alchemize / Infrastructure /
backup_passphrase".)

### 3. Restore against a fresh empty database

**NEVER restore on top of the live prod DB without a manual signoff.**
Always restore to a sibling database first and confirm the data looks right.

```bash
# Create a sibling DB
PGPASSWORD=$POSTGRES_PASSWORD psql \
  -h api.alchemizeiq.com -U postgres -d postgres \
  -c "CREATE DATABASE leadsystem_restore_test;"

# Restore the dump into it
PGPASSWORD=$POSTGRES_PASSWORD pg_restore \
  -h api.alchemizeiq.com -U postgres \
  -d leadsystem_restore_test \
  --no-owner --no-privileges \
  --jobs=4 \
  leadsystem-prod.dump

# Verify row counts match what you expected
PGPASSWORD=$POSTGRES_PASSWORD psql \
  -h api.alchemizeiq.com -U postgres -d leadsystem_restore_test \
  -c "SELECT count(*) FROM lead;"
```

### 4. Cutover (if doing a real restore)

This is the destructive step. Only proceed after the sibling DB checks out.

```bash
# 1. Stop the app
#    (in Coolify: leadsystem-prod service → Stop)

# 2. Rename databases
PGPASSWORD=$POSTGRES_PASSWORD psql \
  -h api.alchemizeiq.com -U postgres -d postgres <<'EOF'
ALTER DATABASE leadsystem RENAME TO leadsystem_broken;
ALTER DATABASE leadsystem_restore_test RENAME TO leadsystem;
EOF

# 3. Start the app again
#    (Coolify: leadsystem-prod service → Start)

# 4. Smoke-test (login, list leads, check Calendly connection)

# 5. When confident, drop the broken DB
PGPASSWORD=$POSTGRES_PASSWORD psql \
  -h api.alchemizeiq.com -U postgres -d postgres \
  -c "DROP DATABASE leadsystem_broken;"
```

## Annual restore drill

Add a calendar reminder for the first Wednesday of every quarter:

> Run `scripts/pg-backup.sh` manually against a test DB, then restore the
> latest backup into a throwaway database following the steps above.
> Verify row counts match. Update "Last verified" date at the top of this doc.

If you skip the drill, you don't have backups — you have files. The first
real restore is not when you want to discover the keys don't match.

## What's NOT covered

- **Point-in-time recovery (PITR)**: this setup gives you ≤24h RPO (whatever
  changed since 03:00 UTC). For PITR you'd add WAL archiving to a Hetzner
  Storage Box bucket. Out of scope for the first cut — defer until we have
  customers whose data loss tolerance is < 1 day.
- **Cross-region replication**: Storage Box and CX42 are both in Helsinki.
  If Helsinki goes down, we have no backups. Acceptable for current scale;
  reconsider when SOC-2 Type II becomes a real customer ask.
- **Encryption-at-rest verification**: this script encrypts before upload,
  but doesn't verify the file decrypts. The annual drill is the verification.

## References

- [Hetzner Storage Box docs](https://docs.hetzner.com/storage/storage-box/)
- [pg_dump custom format](https://www.postgresql.org/docs/current/app-pgdump.html)
- [pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html)
- LSB-163 (this ticket): https://alchemizeiq.atlassian.net/browse/LSB-163

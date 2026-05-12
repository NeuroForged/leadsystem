# PostgreSQL backup + restore runbook

**Ticket:** LSB-163 (DB backups) / LSB-169 (log backups)
**Last verified:** 2026-05-12 — end-to-end decrypt round-trip on `leadsystem-prod` (PGDMP magic + 145 TOC entries restored cleanly)
**Storage Box:** `u592336.your-storagebox.de` (port 23), user `u592336`

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

## Deployed shape

Coolify v4's built-in database backups only target S3-compatible storage,
which Hetzner Storage Box is not (SFTP only). The pragmatic deployment we
shipped is **host-level cron on the CX23**, using `docker exec` to run
`pg_dump` inside each Postgres container and SFTP'ing the encrypted dumps
to the Storage Box.

```
/etc/cron.d/alchemize-backups          host crontab (5 entries)
/usr/local/bin/pg-backup.sh            DB backup script (LSB-163)
/usr/local/bin/log-backup.sh           Log archive script (LSB-169)
/root/.alchemize-backup/id_ed25519     Private SSH key for the Storage Box
/root/.alchemize-backup/passphrase     AES-256 passphrase
/var/log/alchemize-backup.log          DB-backup cron output
/var/log/alchemize-log-backup.log      Log-backup cron output
```

Schedule (UTC, staggered to spread Storage Box upload load):
- `03:00` leadsystem-db-prod → `leadsystem-prod-*.dump.gpg`
- `03:15` leadsystem-db-dev → `leadsystem-dev-*.dump.gpg`
- `03:30` alchemize-chatbot-prod-db → `chatbot-prod-db-*.dump.gpg`
- `03:45` alchemize-chatbot-dev-db → `chatbot-dev-db-*.dump.gpg`
- `04:00` log archive → `logs/logs-YYYY-MM-DD.tar.gpg` (Loki + Coolify state)

DB retention: 30 days. Log retention: 14 days.

## First-time setup (one-off — already done 2026-05-12)

Kept here as a runbook for disaster recovery / new host migration.

### 1. Create the Hetzner Storage Box

In the Hetzner Cloud console:

1. Storage → Storage Boxes → Create. **BX11** (1 TB, ~€4/mo) is plenty.
2. Name `alchemize-backups`. Location: Falkenstein (matches the CX23).
3. Access: SSH key only (paste your ed25519 public key). Leave password blank.
4. Additional settings:
   - **SSH Support: ON** — required for SFTP
   - **External Reachability: ON** — required (Cloud server ↔ Storage Box is public network)
   - SMB Support: OFF, WebDAV Support: OFF
5. After creation, note the hostname (`uXXXXXX.your-storagebox.de`) and
   username (`uXXXXXX`). The SSH port is `23`, not 22.

### 2. Generate the encryption passphrase + backup SSH key

```bash
openssl rand -base64 48 > BACKUP_PASSPHRASE.txt
ssh-keygen -t ed25519 -f backup_id_ed25519 -N "" -C "alchemize-backups"
```

Save **both** in 1Password under "Alchemize / Infrastructure / Backups".
Without the passphrase the backups are worthless. Without the private key
the cron can't reach the Storage Box.

Add the public key (`backup_id_ed25519.pub`) to the Hetzner Storage Box's
SSH keys list.

### 3. Place secrets on the CX23

SSH or use Coolify's host Terminal (Terminal → select `localhost`).

```bash
mkdir -p /root/.alchemize-backup && chmod 700 /root/.alchemize-backup

# Paste the private key + passphrase into these files (or scp them up)
cat > /root/.alchemize-backup/id_ed25519 <<'EOF'
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
EOF
chmod 600 /root/.alchemize-backup/id_ed25519

echo 'YOUR_PASSPHRASE_HERE' > /root/.alchemize-backup/passphrase
chmod 600 /root/.alchemize-backup/passphrase
```

### 4. Drop the backup scripts in place

Copy `scripts/pg-backup.sh` and `scripts/log-backup.sh` from this repo into
`/usr/local/bin/` on the host and make them executable. Naming on host is
`alchemize-pg-backup.sh` / `alchemize-log-backup.sh` to be unambiguous in
process lists.

```bash
install -m 755 scripts/pg-backup.sh  /usr/local/bin/alchemize-pg-backup.sh
install -m 755 scripts/log-backup.sh /usr/local/bin/alchemize-log-backup.sh
```

### 5. Install the cron file

Write `/etc/cron.d/alchemize-backups`:

```
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
MAILTO=""

# LSB-163: Postgres dumps
0  3 * * * root DB_CONTAINER=<prod-db-uuid> DB_USER=leadsystem_user DB_NAME=leadsystem    BACKUP_NAME_PREFIX=leadsystem-prod  /usr/local/bin/alchemize-pg-backup.sh >> /var/log/alchemize-backup.log 2>&1
15 3 * * * root DB_CONTAINER=<dev-db-uuid>  DB_USER=leadsystem_dev  DB_NAME=leadsystem_dev BACKUP_NAME_PREFIX=leadsystem-dev   /usr/local/bin/alchemize-pg-backup.sh >> /var/log/alchemize-backup.log 2>&1
30 3 * * * root DB_CONTAINER=<chat-prod>    DB_USER=postgres        DB_NAME=postgres       BACKUP_NAME_PREFIX=chatbot-prod-db  /usr/local/bin/alchemize-pg-backup.sh >> /var/log/alchemize-backup.log 2>&1
45 3 * * * root DB_CONTAINER=<chat-dev>     DB_USER=postgres        DB_NAME=postgres       BACKUP_NAME_PREFIX=chatbot-dev-db   /usr/local/bin/alchemize-pg-backup.sh >> /var/log/alchemize-backup.log 2>&1

# LSB-169: Loki + Coolify state archive
0 4 * * * root /usr/local/bin/alchemize-log-backup.sh >> /var/log/alchemize-log-backup.log 2>&1
```

The container UUIDs are visible in `docker ps` or Coolify's resource list.
Cron picks up `/etc/cron.d/*` automatically — no `systemctl reload` needed.

### 6. Smoke-test

```bash
# Run one DB backup ad-hoc
DB_CONTAINER=<prod-db-uuid> DB_USER=leadsystem_user DB_NAME=leadsystem \
    BACKUP_NAME_PREFIX=leadsystem-prod /usr/local/bin/alchemize-pg-backup.sh

# Run the log archive ad-hoc
/usr/local/bin/alchemize-log-backup.sh

# List on the Storage Box
echo 'ls -la' | sftp -P 23 -i /root/.alchemize-backup/id_ed25519 \
    u592336@u592336.your-storagebox.de
```

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

#!/usr/bin/env bash
#
# LSB-163: PostgreSQL logical backup → encrypted tarball → Hetzner Storage Box.
#
# Runs on the Coolify host (CX23) via cron. Uses `docker exec` to run pg_dump
# inside each managed Postgres container, then gpg-encrypts + SFTPs to the
# Storage Box. Retention pruning trims files older than RETENTION_DAYS.
#
# Required env (set in the cron entry):
#   DB_CONTAINER         — Docker container name of the postgres service
#   DB_USER              — postgres role
#   DB_NAME              — database name to dump
#   BACKUP_NAME_PREFIX   — e.g. leadsystem-prod, chatbot-dev-db
#
# Configured globally (do not override per-cron):
#   STORAGE_BOX_HOST     u592336.your-storagebox.de
#   STORAGE_BOX_USER     u592336
#   STORAGE_BOX_PORT     23
#   SSH_KEY              /root/.alchemize-backup/id_ed25519
#   PASSPHRASE_FILE      /root/.alchemize-backup/passphrase
#   RETENTION_DAYS       30
#
# Log goes to /var/log/alchemize-backup.log

set -euo pipefail

STORAGE_BOX_HOST="u592336.your-storagebox.de"
STORAGE_BOX_USER="u592336"
STORAGE_BOX_PORT="23"
SSH_KEY="/root/.alchemize-backup/id_ed25519"
PASSPHRASE_FILE="/root/.alchemize-backup/passphrase"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

# Required per-call env vars
: "${DB_CONTAINER:?DB_CONTAINER not set}"
: "${DB_USER:?DB_USER not set}"
: "${DB_NAME:?DB_NAME not set}"
: "${BACKUP_NAME_PREFIX:?BACKUP_NAME_PREFIX not set}"

log() { echo "[$(date -u +%FT%TZ)] [$BACKUP_NAME_PREFIX] $*"; }

log "Starting backup"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
dump_filename="${BACKUP_NAME_PREFIX}-${timestamp}.dump"
encrypted_filename="${dump_filename}.gpg"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

dump_path="$tmp_dir/$dump_filename"
encrypted_path="$tmp_dir/$encrypted_filename"

# 1. pg_dump inside the DB container (custom format = smaller + parallel restore)
log "pg_dump from container $DB_CONTAINER"
docker exec -i "$DB_CONTAINER" \
    pg_dump --username="$DB_USER" --dbname="$DB_NAME" \
            --format=custom --no-owner --no-privileges \
    > "$dump_path"
dump_size="$(stat -c%s "$dump_path")"
log "pg_dump complete: ${dump_size} bytes"

# 2. AES-256 symmetric encrypt
log "Encrypting"
gpg --batch --yes --quiet \
    --passphrase-file "$PASSPHRASE_FILE" \
    --symmetric --cipher-algo AES256 \
    --output "$encrypted_path" "$dump_path"

# 3. SFTP upload
log "Uploading $encrypted_filename to $STORAGE_BOX_HOST"
echo "put $encrypted_path $encrypted_filename" | sftp -q \
    -P "$STORAGE_BOX_PORT" \
    -i "$SSH_KEY" \
    -o BatchMode=yes \
    -o StrictHostKeyChecking=accept-new \
    -o ConnectTimeout=30 \
    -b - \
    "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}"

# 4. Retention prune: delete files older than RETENTION_DAYS matching this prefix
log "Pruning >${RETENTION_DAYS} days"
existing="$(echo "ls ${BACKUP_NAME_PREFIX}-*.dump.gpg" | sftp -q \
    -P "$STORAGE_BOX_PORT" \
    -i "$SSH_KEY" \
    -o BatchMode=yes \
    -o StrictHostKeyChecking=accept-new \
    -b - \
    "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" 2>/dev/null \
    | grep -E "^${BACKUP_NAME_PREFIX}-.*\.dump\.gpg$" || true)"

if [[ -n "$existing" ]]; then
    cutoff_epoch="$(($(date -u +%s) - RETENTION_DAYS * 86400))"
    delete_cmds=""
    while IFS= read -r f; do
        ts="$(echo "$f" | sed -E "s/^${BACKUP_NAME_PREFIX}-([0-9]{8}T[0-9]{6}Z)\.dump\.gpg$/\1/")"
        if [[ "$ts" != "$f" ]]; then
            file_epoch="$(date -ud "${ts:0:4}-${ts:4:2}-${ts:6:2} ${ts:9:2}:${ts:11:2}:${ts:13:2}" +%s 2>/dev/null || echo 0)"
            if [[ "$file_epoch" -lt "$cutoff_epoch" && "$file_epoch" -gt 0 ]]; then
                delete_cmds+="rm $f"$'\n'
            fi
        fi
    done <<< "$existing"

    if [[ -n "$delete_cmds" ]]; then
        echo "$delete_cmds" | sftp -q \
            -P "$STORAGE_BOX_PORT" \
            -i "$SSH_KEY" \
            -o BatchMode=yes \
            -o StrictHostKeyChecking=accept-new \
            -b - \
            "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" >/dev/null 2>&1
        pruned="$(echo "$delete_cmds" | grep -c "^rm ")"
        log "Pruned $pruned older backups"
    fi
fi

log "Done: $encrypted_filename"

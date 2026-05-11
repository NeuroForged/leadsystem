#!/usr/bin/env bash
#
# LSB-163: PostgreSQL logical backup → encrypted tarball → Hetzner Storage Box.
#
# Designed to run as a Coolify Scheduled Task once a day per database. One
# scheduled task per DB (leadsystem-prod, leadsystem-dev, chatbot-prod-db,
# chatbot-dev-db) keeps failures isolated.
#
# Required env vars (set per scheduled-task in Coolify):
#   PG_HOST            — e.g. internal hostname of the postgres service
#   PG_PORT            — usually 5432
#   PG_USER            — usually postgres
#   PG_PASSWORD        — service-account password
#   PG_DATABASE        — db name to dump
#   BACKUP_NAME_PREFIX — e.g. "leadsystem-prod"
#   STORAGE_BOX_HOST   — Hetzner Storage Box hostname (e.g. u123456.your-storagebox.de)
#   STORAGE_BOX_USER   — Storage Box username
#   SSH_KEY_PATH       — path to private key inside the container (mount as secret)
#   ENCRYPTION_PASSPHRASE — symmetric encryption key (32+ chars random)
#
# Optional:
#   RETENTION_DAYS     — older files pruned from the Storage Box (default 30)
#
# Local test:
#   PG_HOST=localhost PG_PORT=5432 PG_USER=postgres PG_PASSWORD=changeme \
#   PG_DATABASE=leadsystem BACKUP_NAME_PREFIX=local-test \
#   STORAGE_BOX_HOST=u123.example.de STORAGE_BOX_USER=u123 \
#   SSH_KEY_PATH=/tmp/test_rsa ENCRYPTION_PASSPHRASE=... ./scripts/pg-backup.sh

set -euo pipefail

# ── Required env var check ─────────────────────────────────────────────────
required_vars=(
  PG_HOST PG_PORT PG_USER PG_PASSWORD PG_DATABASE
  BACKUP_NAME_PREFIX STORAGE_BOX_HOST STORAGE_BOX_USER SSH_KEY_PATH
  ENCRYPTION_PASSPHRASE
)
for var in "${required_vars[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: required env var $var is not set" >&2
    exit 1
  fi
done

RETENTION_DAYS="${RETENTION_DAYS:-30}"

# ── Build the backup file ──────────────────────────────────────────────────
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
hostname="$(hostname -s 2>/dev/null || echo unknown)"
dump_filename="${BACKUP_NAME_PREFIX}-${timestamp}.dump"
encrypted_filename="${dump_filename}.gpg"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

dump_path="$tmp_dir/$dump_filename"
encrypted_path="$tmp_dir/$encrypted_filename"

echo "[$(date -u +%FT%TZ)] Starting backup: $BACKUP_NAME_PREFIX on $hostname"

# 1. pg_dump in custom format (smaller + parallel restore later)
PGPASSWORD="$PG_PASSWORD" pg_dump \
  --host="$PG_HOST" \
  --port="$PG_PORT" \
  --username="$PG_USER" \
  --dbname="$PG_DATABASE" \
  --format=custom \
  --no-owner \
  --no-privileges \
  --file="$dump_path"

dump_size="$(stat -c%s "$dump_path" 2>/dev/null || stat -f%z "$dump_path")"
echo "[$(date -u +%FT%TZ)] pg_dump completed: ${dump_size} bytes"

# 2. Symmetric-encrypt with the configured passphrase. AES-256, no signing.
echo "$ENCRYPTION_PASSPHRASE" | gpg \
  --batch --yes --quiet \
  --passphrase-fd 0 \
  --symmetric \
  --cipher-algo AES256 \
  --output "$encrypted_path" \
  "$dump_path"

# 3. SFTP-upload to the Storage Box. -B 65536 for big-file throughput.
echo "[$(date -u +%FT%TZ)] Uploading $encrypted_filename to $STORAGE_BOX_HOST"
echo "put \"$encrypted_path\" $encrypted_filename" | sftp \
  -i "$SSH_KEY_PATH" \
  -o StrictHostKeyChecking=accept-new \
  -o UserKnownHostsFile=/dev/null \
  -o ConnectTimeout=30 \
  -B 65536 \
  "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}"

# 4. Prune older files matching this prefix.
echo "[$(date -u +%FT%TZ)] Pruning backups older than ${RETENTION_DAYS} days"
prune_script=$(cat <<EOF
ls -1 ${BACKUP_NAME_PREFIX}-*.dump.gpg
EOF
)
file_list="$(echo "$prune_script" | sftp \
  -i "$SSH_KEY_PATH" \
  -o StrictHostKeyChecking=accept-new \
  -o UserKnownHostsFile=/dev/null \
  -o ConnectTimeout=30 \
  -b - \
  "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" 2>/dev/null \
  | grep -E "^${BACKUP_NAME_PREFIX}-.*\.dump\.gpg$" || true)"

if [[ -n "$file_list" ]]; then
  cutoff_epoch="$(($(date -u +%s) - RETENTION_DAYS * 86400))"
  delete_commands=""
  while IFS= read -r f; do
    # Extract timestamp from filename and compare against cutoff.
    ts="$(echo "$f" | sed -E "s/^${BACKUP_NAME_PREFIX}-([0-9]{8}T[0-9]{6}Z)\.dump\.gpg$/\1/")"
    if [[ "$ts" != "$f" ]]; then
      file_epoch="$(date -ud "${ts:0:4}-${ts:4:2}-${ts:6:2} ${ts:9:2}:${ts:11:2}:${ts:13:2}" +%s 2>/dev/null || echo 0)"
      if [[ "$file_epoch" -lt "$cutoff_epoch" && "$file_epoch" -gt 0 ]]; then
        delete_commands+="rm $f"$'\n'
      fi
    fi
  done <<< "$file_list"

  if [[ -n "$delete_commands" ]]; then
    echo "$delete_commands" | sftp \
      -i "$SSH_KEY_PATH" \
      -o StrictHostKeyChecking=accept-new \
      -o UserKnownHostsFile=/dev/null \
      -o ConnectTimeout=30 \
      -b - \
      "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" >/dev/null 2>&1
    pruned_count="$(echo "$delete_commands" | grep -c "^rm " || true)"
    echo "[$(date -u +%FT%TZ)] Pruned $pruned_count old backups"
  fi
fi

echo "[$(date -u +%FT%TZ)] Backup complete: $encrypted_filename"

#!/usr/bin/env bash
#
# LSB-169: nightly log + container-state archive → Hetzner Storage Box logs/
#
# Bundles:
#   /var/lib/docker/volumes/monitoring_loki-data/_data  — Loki chunks
#   /data/coolify/applications                          — app deployment metadata + logs
#   /data/coolify/proxy                                 — proxy (Caddy/Nginx) config + logs
#   /data/coolify/services                              — Coolify-managed services
#
# Daily, retains 14 days. Uses the same Storage Box credentials + passphrase
# from LSB-163.
#
# Log goes to /var/log/alchemize-log-backup.log

set -euo pipefail

STORAGE_BOX_HOST="u592336.your-storagebox.de"
STORAGE_BOX_USER="u592336"
STORAGE_BOX_PORT="23"
SSH_KEY="/root/.alchemize-backup/id_ed25519"
PASSPHRASE_FILE="/root/.alchemize-backup/passphrase"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

log() { echo "[$(date -u +%FT%TZ)] [log-backup] $*"; }
log "Starting log archive"

date_tag="$(date -u +%Y-%m-%d)"
filename="logs-${date_tag}.tar.gpg"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

archive_path="$tmp_dir/${filename%.gpg}"

# 1. tar everything (skip missing paths gracefully)
log "Building tarball"
tar --create --gzip \
    --warning=no-file-changed \
    --warning=no-file-removed \
    --ignore-failed-read \
    --file="$archive_path" \
    /var/lib/docker/volumes/monitoring_loki-data/_data \
    /data/coolify/applications \
    /data/coolify/proxy \
    /data/coolify/services \
    2>/dev/null || true
size="$(stat -c%s "$archive_path")"
log "Tarball ready: ${size} bytes"

# 2. GPG encrypt
log "Encrypting"
gpg --batch --yes --quiet \
    --passphrase-file "$PASSPHRASE_FILE" \
    --symmetric --cipher-algo AES256 \
    --output "$tmp_dir/$filename" "$archive_path"

# 3. SFTP upload — ensures logs/ exists then put
log "Uploading $filename"
sftp -q -P "$STORAGE_BOX_PORT" -i "$SSH_KEY" \
     -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=30 \
     -b - "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" <<EOF || true
-mkdir logs
cd logs
put $tmp_dir/$filename $filename
EOF

# 4. Retention prune (logs/ subdir, files older than $RETENTION_DAYS)
log "Pruning >${RETENTION_DAYS} days"
existing="$(echo -e "cd logs\nls logs-*.tar.gpg" | sftp -q \
    -P "$STORAGE_BOX_PORT" -i "$SSH_KEY" \
    -o BatchMode=yes -o StrictHostKeyChecking=accept-new \
    -b - "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" 2>/dev/null \
    | grep -E "^logs-[0-9]{4}-[0-9]{2}-[0-9]{2}\.tar\.gpg$" || true)"

if [[ -n "$existing" ]]; then
    cutoff_epoch="$(($(date -u +%s) - RETENTION_DAYS * 86400))"
    delete_cmds="cd logs"$'\n'
    has_deletes=0
    while IFS= read -r f; do
        d="$(echo "$f" | sed -E "s/^logs-([0-9]{4}-[0-9]{2}-[0-9]{2})\.tar\.gpg$/\1/")"
        if [[ "$d" != "$f" ]]; then
            file_epoch="$(date -ud "$d" +%s 2>/dev/null || echo 0)"
            if [[ "$file_epoch" -lt "$cutoff_epoch" && "$file_epoch" -gt 0 ]]; then
                delete_cmds+="rm $f"$'\n'
                has_deletes=1
            fi
        fi
    done <<< "$existing"

    if [[ "$has_deletes" -eq 1 ]]; then
        echo "$delete_cmds" | sftp -q \
            -P "$STORAGE_BOX_PORT" -i "$SSH_KEY" \
            -o BatchMode=yes -o StrictHostKeyChecking=accept-new \
            -b - "${STORAGE_BOX_USER}@${STORAGE_BOX_HOST}" >/dev/null 2>&1
        pruned="$(echo "$delete_cmds" | grep -c "^rm ")"
        log "Pruned $pruned older log archives"
    fi
fi

log "Done: $filename"

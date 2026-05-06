# Loki + Promtail — Deployment Guide (LSB-138)

## Architecture

```
leadsystem (prod) ──writes──> /logs/app.log      ─┐
                               /logs/events.log   ─┤
                                                    ├──> Promtail ──> Loki ──> Grafana
leadsystem (dev)  ──writes──> /logs/app.log      ─┤
                               /logs/events.log   ─┘
```

Both leadsystem containers write to a named Docker volume (`/logs`).
Promtail mounts both volumes read-only and ships logs to Loki.
Grafana (already running in Coolify) queries Loki via the shared `coolify` network.

---

## Step 1 — Add log volumes to leadsystem services in Coolify

In Coolify, for **both** the prod and dev leadsystem services:

1. Open the service → **Storage** tab
2. Add a volume:
   - **Prod**: Source name = `leadsystem-logs`, Container path = `/logs`
   - **Dev**:  Source name = `leadsystem-logs-dev`, Container path = `/logs`
3. Redeploy each service

Coolify will create Docker volumes named `leadsystem-logs` and `leadsystem-logs-dev`.

> If Coolify prefixes volume names with the project ID, update the `name:` field under
> `volumes:` in `docker-compose.yml` to match the actual volume name
> (`docker volume ls | grep leadsystem` on the server).

---

## Step 2 — Deploy Loki + Promtail in Coolify

1. In Coolify → **New Service** → **Docker Compose**
2. Paste the contents of `docker-compose.yml` (or point at this repo path)
3. Upload / bind-mount the config files:
   - `loki-config.yml`
   - `promtail-config.yml`
   (Put them in the same directory as docker-compose.yml on the server,
    or use Coolify's file mount feature)
4. Deploy

Alternatively, SSH into the server and run:

```bash
cd /path/to/deploy/monitoring
docker compose up -d
```

---

## Step 3 — Add Loki data source in Grafana

1. Grafana → **Connections** → **Data sources** → **Add data source** → **Loki**
2. URL: `http://loki:3100`
3. Click **Save & test** — should show "Data source connected and labels found"

---

## Verifying logs arrive

```logql
# All business events (prod)
{app="leadsystem", env="prod", log_type="events"}

# All full logs at ERROR level (prod)
{app="leadsystem", env="prod", log_type="full", level="ERROR"}

# Logs for a specific request
{app="leadsystem"} | json | req="abc123def456"

# Lead received events
{app="leadsystem", log_type="events"} |= "LEAD RECEIVED"
```

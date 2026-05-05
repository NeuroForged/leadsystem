# Machine Handover — 2026-05-02

Generated at end of session. All work is pushed. Prod 502 resolved. Ready to continue on new machine.

---

## Quick setup on new machine

```bash
# 1. Install tools
# - Claude Code (claude.ai/code)
# - gh CLI + auth: gh auth login
# - Java 21 (temurin), Maven, Node 20+, git, Docker Desktop

# 2. Clone repos
git clone https://github.com/NeuroForged/leadsystem C:\Users\Josh\Desktop\NeuroForged\leadsystem
git clone https://github.com/NeuroForged/alchemize-portal C:\Users\Josh\Desktop\NeuroForged\alchemize-portal

# 3. Set up global Claude context
# Copy docs/handover/claude/CLAUDE.md → C:\Users\Josh\.claude\CLAUDE.md
# Copy docs/handover/claude/settings.json → C:\Users\Josh\.claude\settings.json
# Copy docs/handover/claude/memory/*.md → C:\Users\Josh\.claude\projects\C--Users-Josh-Desktop-NeuroForged\memory\

# 4. SSH key for Hetzner — copy alchemize_hetzner + alchemize_hetzner.pub to ~/.ssh/
# Key must be copied manually (not pushed to repo for security)

# 5. Local .env files (not in repo — must re-enter)
# leadsystem: copy .env.example → .env and fill values
# portal: create .env.local with VITE_API_BASE_URL=http://localhost:8080

# 6. Jira MCP — configure the Atlassian MCP server in Claude Code settings
# Cloud ID: alchemizeiq.atlassian.net
```

---

## Current state — what's live

### Production (master/main)
| Service | URL | Status |
|---------|-----|--------|
| Lead System Backend | https://api.alchemizeiq.com | LIVE (Coolify auto-deploys master) |
| Alchemize Portal | https://app.alchemizeiq.com | LIVE (Coolify auto-deploys main) |
| Scraper | https://scraper.alchemizeiq.com | LIVE |

### Dev
| Service | URL | Branch |
|---------|-----|--------|
| Lead System Backend | https://api-dev.alchemizeiq.com | develop |
| Alchemize Portal | https://app-dev.alchemizeiq.com | develop |

### Infrastructure
- Hetzner CX23 at 178.105.49.110, Coolify UI at http://178.105.49.110:8000
- SSH key: `alchemize_hetzner` (copy from old machine or regenerate + add to Coolify)

---

## What was done this session

### Prod 502 — RESOLVED
**Root cause:** PR #20 brought Flyway (`ddl-auto: validate`) to master but did not include V2 migration (`invitee_name` column on `calendly_meeting`). Prod container crashed on startup.
**Fix:** PR #21 merged (2026-05-02 15:18 UTC) — includes V2+V3 Flyway migrations. Coolify will auto-deploy.

### Backend (leadsystem develop → master via PR #21)
- **LSB-88 SampleDataSeeder:** Seeds 5 clients, 89 leads, 28+ meetings. Enable with env var `NEUROFORGED_SEED_SAMPLE_DATA=true` (dev only).
- **PORTAL-77 Scrape Job persistence:** `ScrapeJob` entity, V2 Flyway migration, full CRUD API:
  - `POST /api/clients/{id}/scrape` — triggers scrape + creates job record
  - `GET /api/scrape-jobs?clientId=X` — job history
  - `GET /api/scrape-jobs/{id}` — lazy status sync (polls scraper if PENDING/RUNNING)
  - `POST /api/scrape-jobs/{id}/sync` — force status sync
- **PORTAL-79 Scrape Presets:** `ScrapePreset` entity, `GET/POST/DELETE /api/scrape-presets?clientId=X`
- **PORTAL-80 Knowledge Base:** `KnowledgeBaseDocument` entity, V3 Flyway migration:
  - `POST /api/clients/{id}/kb/fetch` — downloads latest scraper ZIP, extracts .md files (20MB codec buffer)
  - `GET /api/clients/{id}/kb` — list all docs
  - `GET /api/clients/{id}/kb/search?q=` — ILIKE search
  - `DELETE /api/clients/{id}/kb` — clear all

### Frontend (alchemize-portal develop — not yet on main)
- **PORTAL-78 ScraperSection rebuilt:** DB-backed job history, PENDING/RUNNING/DONE/ERROR badges, save-as-preset form, preset Zap quick-run + Trash2 delete, job row Re-run + Download
- **PORTAL-79 Presets UI:** Inline in ScraperSection
- **PORTAL-80 KbSection:** Collapsible panel in EditClientSlideOver — word count badge, Fetch KB, Clear w/ confirmation, search, expandable Core/Blog content viewers

### Jira
- PORTAL-8 (Scraper integration epic) → Done
- PORTAL-77, 78, 79, 80 → all Done
- All other tickets were already in correct states

---

## Immediate next actions

### 1. Portal develop → main PR (to deploy PORTAL-78/79/80 to prod)
The frontend work for scrape job history, presets, and KB is on develop but not main yet.
```bash
cd C:\Users\Josh\Desktop\NeuroForged\alchemize-portal
gh pr create --base main --head develop --title "release: scrape job history, presets, knowledge base (PORTAL-78/79/80)" --body "..."
```

### 2. Prod is UP ✅
`{"status":"UP"}` confirmed at 15:27 UTC 2026-05-02. V4 migration applied, container running cleanly.

### 3. Test scraper integration end-to-end
- Trigger a scrape from EditClientSlideOver → verify job appears in history
- Wait for completion → click "Fetch KB" → verify Core/Blog docs appear
- Test search within KB

### 4. Next Jira tickets (PORTAL project)
- **PORTAL-9:** Client portal role — add `CLIENT` JWT role, scoped views per client (Phase 2 start)
- **PORTAL-81:** KB client access — blocked on PORTAL-9

---

## Repo state

### leadsystem
- **master:** PR #21 just merged — includes V2/V3 migrations + all Phase 2 backend
- **develop:** same as master (all work was merged)
- **Open PRs:** none
- **Worktrees:** `.claude/worktrees/agent-a5f7bfbc9227a6094` (develop) — safe to delete

### alchemize-portal  
- **main:** Phase 1 only (no PORTAL-78/79/80 UI yet)
- **develop:** 2 commits ahead — PORTAL-78/79/80 frontend complete
- **Open PRs:** none — needs one created (main ← develop)

### scraper
- **No changes this session**

---

## Flyway migration history (leadsystem)

| Version | File | Contents |
|---------|------|----------|
| V1 | `V1__initial_schema.sql` | Full baseline — all tables at PR #20 merge |
| V2 | `V2__scrape_jobs_presets.sql` | `ALTER TABLE calendly_meeting ADD invitee_name`; creates `scrape_job`, `scrape_preset` |
| V3 | `V3__knowledge_base.sql` | Creates `knowledge_base_document` + index |
| V4 | `V4__client_missing_columns.sql` | Adds `api_key` (with UUID backfill) + `last_scraped_at` to `client` table |

**Important:** `baseline-on-migrate=true` — existing prod DB will be baselined at V1 then V2+V3+V4 run automatically.

**Gotcha — Flyway baseline skips SQL:** When an existing DB is baselined, V1 SQL never runs. Any columns added to entities AFTER the last `ddl-auto:update` deployment but included in the V1 SQL will be missing from the prod DB. Always add a migration for columns that bridged this gap (V2 fixed `invitee_name`, V4 fixed `api_key` + `last_scraped_at`).

---

## Key env vars to configure

### leadsystem (prod — in Coolify)
```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL, _USERNAME, _PASSWORD
NEUROFORGED_JWT_SECRET (64+ chars)
NEUROFORGED_INTERNAL_TOKEN
NEUROFORGED_ADMIN_EMAIL / NEUROFORGED_ADMIN_PASSWORD
NEUROFORGED_CORS_ALLOWED_ORIGINS=https://app.alchemizeiq.com,https://app-dev.alchemizeiq.com
SCRAPER_API_URL=https://scraper.alchemizeiq.com
SCRAPER_API_KEY
NEUROFORGED_SEED_SAMPLE_DATA=false (set true on dev only to seed test data)
```

### alchemize-portal (Coolify)
```
VITE_API_BASE_URL=https://api.alchemizeiq.com   (prod)
VITE_API_BASE_URL=https://api-dev.alchemizeiq.com  (dev)
VITE_SCRAPER_BASE_URL=https://scraper.alchemizeiq.com
```

---

## Claude Code config files (copied to docs/handover/claude/)

- `CLAUDE.md` — global Claude instructions (→ `~/.claude/CLAUDE.md`)
- `settings.json` — tool permissions + Stop hook for Windows notifications (→ `~/.claude/settings.json`)
- `memory/*.md` — project memory files (→ `~/.claude/projects/C--Users-Josh-Desktop-NeuroForged/memory/`)

### Jira MCP server
Configure in Claude Code with:
- Provider: Atlassian
- Site: `alchemizeiq.atlassian.net`
- Cloud ID will auto-resolve

---

## Gotchas to remember

- **CORS wildcard + credentials:** Always use `setAllowedOriginPatterns("*")` not `setAllowedOrigins("*")` in Spring (LSB-79 fix already in code)
- **Flyway:** New columns/tables need `VX__description.sql` migration files — never rely on `ddl-auto: update` anymore
- **Vite proxy:** Must strip `Origin` header in `vite.config.ts` configure block — already done
- **Rate limiter:** `POST /api/leads` is rate-limited; add `sleep 2` between bulk curl requests
- **tslib shim:** `src/shims/tslib.js` aliased in vite.config.ts — don't remove it
- **WebClient codec:** 20MB buffer set in ScraperServiceImpl for ZIP downloads — needed for large sites
- **Coolify Livewire:** Navigate to app base page before clicking tabs; `$wire.deploy()` to redeploy programmatically

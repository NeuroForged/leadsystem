---
name: Lead System Backend — Current State
description: What's live, what's done, open PRs to master, and key technical decisions as of 2026-05-02
type: project
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**What it is:** Multi-tenant lead-capture + Calendly-integration backend. Chatbots submit leads via API key. Admin dashboard uses JWT. Calendly OAuth per client. Java 21 / Spring Boot 3.5.3 / PostgreSQL.

**Both environments live as of 2026-05-02:**
- Prod: https://api.alchemizeiq.com (master branch → Coolify auto-deploy)
- Dev: https://api-dev.alchemizeiq.com (develop branch → Coolify auto-deploy)

**GitHub:** https://github.com/NeuroForged/leadsystem
**Local path:** C:\Users\Josh\Desktop\NeuroForged\leadsystem
**Build:** `./mvnw clean package -DskipTests`

**Phase 1 shipped to prod (PR #20 → master 2026-05-02):**
- Auth: BCrypt admin seed, JWT, API key auth, CORS wildcard fix, PATCH /auth/password
- Calendly: OAuth, webhook, retry scheduler, polling fallback, token refresh, cancel/reschedule
- Lead pipeline: per-client duplicate check, LeadStatus, paginated leads, rate limiting, validation, timestamps
- Clients: GET list, PUT update, DELETE, websiteUrl, apiKey (UUID @PrePersist), notificationEmails[], calendlyConnected, lastScrapedAt; PATCH /scrape-timestamp
- Analytics: full SQL GROUP BY suite — kpis, volume, traffic source, score band, business type, pipeline, top leads, client summary JOIN query
- Meetings: GET /api/meetings (paginated, filtered) + GET /api/meetings/{id}
- Flyway: V1 baseline schema, ddl-auto:validate, baseline-on-migrate=true
- N+1 fixes: batch CalendlyAccount lookup in getAllClients; native SQL aggregate in getClientSummary

**Phase 2 shipped to prod (PR #21 → master 2026-05-02):**
- LSB-88: SampleDataSeeder — seeds 5 clients, 89 leads, 28+ meetings on startup when `NEUROFORGED_SEED_SAMPLE_DATA=true`
- PORTAL-77: ScrapeJob entity + API — `POST /api/clients/{id}/scrape`, `GET /api/scrape-jobs?clientId=X`, `GET /api/scrape-jobs/{id}` (lazy status sync), `POST /api/scrape-jobs/{id}/sync`
- PORTAL-79: ScrapePreset entity + API — `GET/POST/DELETE /api/scrape-presets?clientId=X`
- PORTAL-80: KnowledgeBaseDocument entity + API — `POST /api/clients/{id}/kb/fetch` (downloads ZIP from scraper, extracts .md files), `GET /api/clients/{id}/kb`, `GET /api/clients/{id}/kb/search?q=`, `DELETE /api/clients/{id}/kb`
- Flyway V2: `ALTER TABLE calendly_meeting ADD COLUMN IF NOT EXISTS invitee_name`; creates `scrape_job` + `scrape_preset` tables
- Flyway V3: creates `knowledge_base_document` table + index

**Open PRs:** None — all merged to master.

**Remaining To Do:**
- LSB-22: Fireflies.ai webhook — needs external Fireflies API credentials
- PORTAL-81: Phase 2 KB client access — blocked on PORTAL-9 (CLIENT JWT role)

**Prod outage (2026-05-02, now resolved):**
PR #20 brought Flyway (ddl-auto:validate) to master but V2 migration (invitee_name) was not included.
Fix: PR #21 merged V2+V3 migrations to master. Coolify will auto-redeploy.

**Flyway notes:**
- New schema changes need `VX__description.sql` in `src/main/resources/db/migration/`
- `baseline-on-migrate=true` handles existing prod DB on first deploy
- Flyway disabled in test profile (H2 create-drop unchanged)
- WebClient codec buffer raised to 20MB in ScraperServiceImpl (large ZIP downloads)

**Key env vars (prod via Coolify, local via .env):**
`NEUROFORGED_CORS_ALLOWED_ORIGINS`, `NEUROFORGED_JWT_SECRET`, `NEUROFORGED_INTERNAL_TOKEN`, `NEUROFORGED_ADMIN_EMAIL/PASSWORD`, `SCRAPER_API_URL`, `SCRAPER_API_KEY`

**Why:** NEUROFORGED_INTERNAL_TOKEN is the chatbot lead submission secret (X-Api-Key). Do not confuse with NEUROFORGED_JWT_SECRET (admin JWT signing).

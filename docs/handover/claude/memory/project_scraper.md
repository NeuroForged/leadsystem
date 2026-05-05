---
name: Scraper Service — Current State
description: Scraper service deployment, API contract, and integration points as of 2026-05-02
type: project
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**What it is:** Python / FastAPI web scraper. Accepts a URL + client ID, scrapes the site, outputs `knowledge_core.md` and `knowledge_blog.md` optimised for Voiceflow RAG.

**Live URL:** https://scraper.alchemizeiq.com
**GitHub:** https://github.com/Joshua-White-NeuroForged/alchemize-scraper (auto-deploys on push to master)
**Local path:** C:\Users\Josh\Desktop\NeuroForged\Alchemize Utilities
**Jira:** SCRAPER — alchemizeiq.atlassian.net/jira/software/projects/SCRAPER

**Deployment:**
- Runs as root in Docker on Coolify (same Hetzner CX23 as leadsystem)
- Jobs persisted to SQLite at `/app/output/jobs.db`
- Frontend auth: API key injected into HTML at serve time via `window._SCRAPER_API_KEY`

**API contract (consumed by leadsystem ScraperServiceImpl):**
- Auth: `X-Api-Key` header with `SCRAPER_API_KEY` value
- Trigger scrape: `POST /scrape` with `{ url, clientId }`
- Poll job: `GET /jobs/{jobId}`
- SSE progress: `GET /jobs/{jobId}/stream`
- Download KB: `GET /jobs/{jobId}/download`

**Leadsystem integration:**
- `ScraperServiceImpl` in `com.neuroforged.leadsystem.service.impl`
- Env vars: `SCRAPER_API_URL` (default: https://scraper.alchemizeiq.com) + `SCRAPER_API_KEY`
- Both vars in `application-local.yml` and `application-prod.yml`

**Portal integration (COMPLETE — PORTAL-8 Done 2026-05-02):**
- Re-scrape button in client edit slide-over (ScraperSection component)
- `VITE_SCRAPER_BASE_URL` env var required in portal (used for direct ZIP downloads)
- Leadsystem acts as proxy for trigger/status; portal calls scraper directly only for ZIP download

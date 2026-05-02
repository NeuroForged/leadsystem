---
name: Infrastructure & Deployment
description: Coolify, Hetzner, DNS, and deployment patterns for all Alchemize services
type: project
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**Server:** Hetzner CX23 at 178.105.49.110. Coolify UI at http://178.105.49.110:8000.

**All services on the same server:**

| Service | Prod URL | Dev URL | Branch → deploy |
|---------|----------|---------|-----------------|
| Lead System Backend | https://api.alchemizeiq.com | https://api-dev.alchemizeiq.com | master / develop |
| Alchemize Portal | https://app.alchemizeiq.com | https://app-dev.alchemizeiq.com | main / develop |
| Scraper | https://scraper.alchemizeiq.com | — | master only |

**Deployment flow:**
- Push to trigger branch → Coolify webhook → Docker build → container swap
- Portal: multi-stage Dockerfile (Node 20 Alpine build → Nginx Alpine serve). `VITE_*` vars baked at build time — changing them requires a full rebuild.
- Backend: multi-stage Dockerfile (Maven build → JRE runtime). Env vars read at startup.

**DNS:** All subdomains → 178.105.49.110. HTTPS via Coolify's Let's Encrypt integration.

**Coolify automation gotchas:**
- Direct URL to sub-pages (Deployments, Logs, etc.) redirects to dashboard — navigate to base app page first, then click tab via JS
- Env var save via Livewire JS automation is unreliable — use the UI manually or fix in code
- `$wire.deploy()` is the programmatic way to trigger a redeploy without the button (avoids session expiry)

**Env var management:**
- Prod env vars set in Coolify UI per service
- Local dev: `.env` file in project root, loaded by `DotEnvLoader` (leadsystem) or `.env.local` (portal, gitignored)
- `NEUROFORGED_CORS_ALLOWED_ORIGINS`: comma-separated frontend URLs in prod, `*` in dev (CorsConfig handles both correctly post LSB-79 fix)

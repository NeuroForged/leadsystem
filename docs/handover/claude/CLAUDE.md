# Claude Code — Joshua White, CTO @ Alchemize

## Communication
- Concise responses. One-line updates while working, brief summary at the end.
- No preamble, no filler phrases. Get straight to it.

## Coding defaults
- Primary stack: Java 21 / Spring Boot / PostgreSQL. Match conventions in whatever repo I'm in.
- Frontend: React (Vite + TypeScript + shadcn/ui + Tailwind) for alchemize-portal. WordPress/Elementor for marketing site.
- Solo developer on all projects. No need to hedge for other contributors.
- No unsolicited comments, abstractions, or error handling beyond what the task requires.
- Prefer editing existing files over creating new ones.

## Git & commits
- Conventional commits. Message should be concise and describe what the Jira ticket achieves.
- Include key file changes only if non-obvious, e.g. `feat(LSB-12): polling fallback — CalendlyPollingScheduler, CalendlyApiClient`
- PRs always target `develop` unless told otherwise.
- After every PR: move the relevant Jira ticket(s) to "Ready for Review" and comment the PR link.

## Tests & build
- Always build before declaring a task done: `./mvnw clean package -DskipTests` (backend) / `npm run build` (portal)
- Before creating a PR: attempt to run tests. If they fail, report the failures and ask whether to proceed with the PR anyway.
- Test suite is currently being improved — do not block PRs on test failures without asking.

## Workflow
- Use the todo list for any task with 3+ steps.
- Ask before irreversible actions (force push, dropping tables, modifying CI pipelines, etc.).
- At the end of sessions use /wrapup to close out cleanly.

## Platform
- Windows 11, bash shell. Use Unix syntax unless PowerShell is explicitly requested.
- Python projects: packages often land in user site-packages (C:\Users\Josh\AppData\Roaming\Python\Python313\site-packages). If a subprocess can't find a module, reinstall without --user or run without --reload.
- Binding to 0.0.0.0 on Windows requires admin rights — default to 127.0.0.1 for local dev servers.

## Active projects & Jira boards

| Project | Stack | Jira | Local path |
|---------|-------|------|------------|
| **Lead System Backend** | Java 21 / Spring Boot / PostgreSQL | **LSB** — alchemizeiq.atlassian.net/jira/software/projects/LSB | C:\Users\Josh\Desktop\NeuroForged\leadsystem |
| **Alchemize Portal** | React 19 / Vite / TS / shadcn | **PORTAL** — alchemizeiq.atlassian.net/jira/software/projects/PORTAL | C:\Users\Josh\Desktop\NeuroForged\alchemize-portal |
| **Scraper** | Python / FastAPI | **SCRAPER** — alchemizeiq.atlassian.net/jira/software/projects/SCRAPER | C:\Users\Josh\Desktop\NeuroForged\Alchemize Utilities |

## Deployment — Coolify on Hetzner CX23 (178.105.49.110)
All products deploy to the same Coolify instance at http://178.105.49.110:8000.

**Lead System Backend**
- **Prod URL:** https://api.alchemizeiq.com (live — Coolify service deployed, env vars configured)
- **Dev URL:** https://api-dev.alchemizeiq.com
- **GitHub:** https://github.com/NeuroForged/leadsystem (auto-deploys on push to master)
- Env vars: see .env.example in repo root

**Alchemize Portal**
- **Prod URL:** https://app.alchemizeiq.com (LIVE — Coolify service deployed, env vars configured)
- **Dev URL:** https://app-dev.alchemizeiq.com (LIVE — Coolify service deployed, env vars configured)
- **GitHub:** https://github.com/NeuroForged/alchemize-portal (auto-deploys: main → prod, develop → dev)
- Build: `npm run build` → Nginx serves `dist/`

**Scraper**
- **Live URL:** https://scraper.alchemizeiq.com
- **GitHub:** https://github.com/Joshua-White-NeuroForged/alchemize-scraper (auto-deploys on push to master)
- **API key:** stored as SCRAPER_API_KEY in Coolify env vars
- Scraper runs as root in Docker; jobs persisted to SQLite at /app/output/jobs.db
- Frontend auth: API key injected into HTML at serve time via window._SCRAPER_API_KEY

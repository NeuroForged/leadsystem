---
name: Alchemize Portal — Current State
description: What's live, what's built, deployment as of 2026-05-02
type: project
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**What it is:** Admin dashboard (Phase 1) + future client portal (Phase 2). React 19 / Vite 8 / TypeScript / shadcn/ui / Tailwind / TanStack Query v5 / Zustand v5.

**Both environments live as of 2026-05-02:**
- Prod: https://app.alchemizeiq.com (main branch → Coolify auto-deploy)
- Dev: https://app-dev.alchemizeiq.com (develop branch → Coolify auto-deploy)

**GitHub:** https://github.com/NeuroForged/alchemize-portal (private)
**Local path:** C:\Users\Josh\Desktop\NeuroForged\alchemize-portal
**Build:** `npm run build`

**Phase 1 shipped to prod (PR #10 merged to main 2026-05-02):**
- Auth, layout, leads, clients — all previously shipped
- Meetings: timeline (upcoming/this week/earlier), cards, detail panel, empty state
- Analytics: KPI cards, volume chart, score histogram, traffic source donut, business type bars, pipeline, top leads, client breakdown table, URL filter state
- Settings: password change form (useChangePassword → PATCH /auth/password)
- Scraper: trigger, polling, download KB, job history, lastScrapedAt display + auto-update
- Polish: error boundaries, EmptyState component, Sonner toasts on all mutations, ARIA/a11y, focus rings, code splitting (107KB initial, analytics 115KB lazy chunk)

**Phase 2 shipped to dev (develop branch, not yet on main as of 2026-05-02):**
- PORTAL-78: ScraperSection rebuilt — DB-backed job history from `/api/scrape-jobs?clientId=X`, status badges (PENDING/RUNNING/DONE/ERROR), save-as-preset inline form, preset quick-run (Zap) + delete (Trash2), job row re-run + download
- PORTAL-79 (portal): Preset management UI inside ScraperSection
- PORTAL-80 (portal): KbSection component in EditClientSlideOver — collapsible, word count badge, Fetch KB button, Clear w/ confirmation, search bar (2+ chars), expandable Core/Blog content viewers
- New API modules: `src/api/scrapeJobs.ts`, `src/api/knowledgeBase.ts`
- Updated types: `src/types/scraper.ts` — `ScrapeJobStatus`, `ScrapeJobDto`, `ScrapePresetDto`
- `useScraperJob.ts` rewritten: uses `/api/clients/{id}/scrape` → polls `/api/scrape-jobs/{id}` every 3s

**Open PRs:** None on main. develop is 2 commits ahead of main (PORTAL-78/79/80 UI).
**TODO:** Create develop→main PR for portal to deploy Phase 2 frontend to prod.

**Remaining To Do:**
- Phase 2: PORTAL-9 (client portal role, CLIENT JWT, scoped views) — future
- PORTAL-81: Phase 2 KB client access — blocked on PORTAL-9

**Key architectural decisions:**
- `sessionStorage` for JWT Phase 1. Moves to httpOnly cookie Phase 2.
- Vite dev proxy strips `Origin` header — required for Spring CORS (vite.config.ts `configure` block)
- `baseURL` in `src/api/client.ts`: `import.meta.env.DEV ? '' : VITE_API_BASE_URL`
- `tslib` shimmed at `src/shims/tslib.js` (Vite 8/Rolldown marks it `ideallyInert`)
- React.lazy on all page routes — initial bundle 107KB gzipped
- Analytics + leads filters in URL params (`useSearchParams`)
- Scraper client_id: `client-{dbId}` format (regex `[a-z0-9][a-z0-9_-]{0,62}`)
- `VITE_SCRAPER_BASE_URL` env var for browser→scraper direct calls

**Jira:** PORTAL — alchemizeiq.atlassian.net/jira/software/projects/PORTAL/boards/35

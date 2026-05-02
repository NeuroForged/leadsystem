---
name: Debugging Patterns & Gotchas
description: Hard-won lessons from debugging sessions — CORS, Coolify, bash, Spring, Vite
type: feedback
originSessionId: b384e883-533b-4a22-9875-2a4b3249054c
---
**Spring CORS wildcard + credentials is illegal.**
`setAllowedOrigins("*")` + `setAllowCredentials(true)` throws IllegalArgumentException and silently rejects all CORS requests with 403. Must use `setAllowedOriginPatterns("*")` for wildcard. `CorsConfig.java` already handles this correctly (LSB-79). If CORS 403s reappear, check this first.

**Why:** Spring enforces the RFC that a wildcard origin cannot be combined with credentials. The error doesn't surface clearly in logs.

**How to apply:** Diagnose CORS failures by running `curl -v -H "Origin: http://localhost:5173" <endpoint>` and checking the `Access-Control-*` response headers directly.

---

**Coolify Livewire env var save cannot be reliably automated via JS.**
Attempts to set env vars programmatically via `$wire.set()`, `saveKeys()`, native setter + input event, and ref-based clicks all failed to persist in different ways (session expiry, local reactive state only, redirect). 

**Why:** Livewire's save flow has CSRF/session requirements that don't survive headless JS injection.

**How to apply:** When env vars need changing, use the Coolify UI manually, OR fix in code so the existing env var value works correctly. Code fix is always preferable.

---

**Direct URL navigation to Coolify sub-pages redirects to dashboard.**
Navigating to `/application/{uuid}/deployments` directly causes a redirect. Must navigate to the base application page first, then click the tab via JS: `document.querySelectorAll('a').find(a => a.textContent.trim() === 'Deployments').click()`.

**Why:** Livewire page lifecycle requires the parent component to be mounted first.

---

**Em dash (—) in bash heredoc breaks curl JSON.**
Unicode em dash in `leadChallenge` or other string fields causes `400 Failed to read request`. Use ASCII hyphen `-` instead.

**Why:** The shell or HTTP layer mangles the multibyte character in a way Jackson can't parse.

**How to apply:** When scripting API requests, always use ASCII punctuation in string values.

---

**Rate limiter (Bucket4j) on POST /api/leads.** Rapid sequential submissions (e.g. seeding the DB) trigger the rate limiter and get 429/502. Add `sleep 2` between each curl request when bulk-inserting leads.

---

**Vite dev proxy must strip the Origin header.** The Vite proxy config must call `proxyReq.removeHeader('origin')` in its `configure` block. Without this, Spring's CORS filter sees `Origin: localhost:5173` on a same-host request and rejects it. This is already configured in `vite.config.ts`.

---

**`VITE_API_BASE_URL` is baked into the JS bundle at build time.** Changing the env var in Coolify requires a rebuild+redeploy to take effect — the value is not read at runtime.

---

**Livewire `$wire.deploy()` is the reliable way to trigger a redeploy** without clicking the UI deploy button (which can expire the session). Call `window.Livewire.all().find(c => c.name === 'project.application.heading').$wire.deploy()` from the browser console.

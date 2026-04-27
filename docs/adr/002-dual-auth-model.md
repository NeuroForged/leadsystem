# ADR-002: Dual authentication model (JWT + API key)

**Status:** Accepted  
**Date:** 2026-04-26

## Context

Two distinct callers need to access the system:
1. **Admin dashboard** — human operator managing leads and clients
2. **AI chatbots** — automated bots submitting leads on behalf of clients, running in third-party environments

## Decision

Two parallel auth paths, both wired in `SecurityConfig`:

- **JWT (`Authorization: Bearer`)** — for the admin dashboard. Issued by `POST /auth/login`. 24h expiry. Verified by `JwtAuthenticationFilter`.
- **API key (`X-Api-Key`)** — for chatbot lead submission to `/api/leads/**`. Single shared secret (`NEUROFORGED_INTERNAL_TOKEN`). Verified by `ApiTokenFilter`, which runs first.

If `ApiTokenFilter` authenticates the request, `JwtAuthenticationFilter` is still invoked but is a no-op (SecurityContext already populated).

## Consequences

- Chatbots don't need to manage sessions or token refresh
- A single compromised API key exposes all clients' lead endpoints — consider per-client keys post-MVP (KAN-7)
- JWT secret rotation invalidates all active admin sessions (acceptable — admin re-logs in)

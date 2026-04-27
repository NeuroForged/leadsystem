# ADR-004: Render + Docker deployment

**Status:** Accepted  
**Date:** 2026-04-26

## Context

The system needs a hosting platform for the Spring Boot API and PostgreSQL database.

## Decision

Deploy to **Render** using the project's multi-stage `Dockerfile`.

- **API service:** Render Web Service, Docker runtime, `--enable-preview` in entrypoint
- **Database:** Render PostgreSQL (managed)
- **Config:** all secrets injected as Render environment variables, `SPRING_PROFILES_ACTIVE=prod`

Branch strategy:
- `master` → prod Render service (auto-deploy on push)
- `develop` → dev Render service (auto-deploy on push)

## Consequences

- Render's free tier spins down after inactivity — cold starts can take 30–60s. Upgrade to paid if this affects clients.
- No infrastructure-as-code currently — Render config lives in the dashboard. Document env vars in `.env.example` as the source of truth.
- The `develop` service should use a separate DB instance from prod to avoid data cross-contamination.

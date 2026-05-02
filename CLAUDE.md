# Alchemize Lead System — Claude Context

## What this is

A multi-tenant lead-capture and Calendly-integration backend. AI chatbots (one per client) submit leads via API key. The admin dashboard uses JWT auth. Each client connects their Calendly account via OAuth; the system tracks meetings and matches them to leads.

**Company:** Alchemize (repo and package names still say `neuroforged` — do not rename them).

## Tech stack

- **Java 21** (preview features enabled — `STR."""` template strings are used)
- **Spring Boot 3.5.3** — web, security, data-jpa, mail, webflux, validation, actuator
- **PostgreSQL** — JPA with `ddl-auto: update` (no migration files; schema evolves automatically)
- **Maven** — build with `./mvnw clean package -DskipTests`
- **Docker** — multi-stage build, deployed on **Coolify** (Hetzner, same server as scraper)
- **Lombok** — `@Data`, `@Builder`, `@RequiredArgsConstructor` used throughout
- **MapStruct** — DTO mapping (`ClientMapper`)
- **jjwt 0.11.5** — JWT signing/validation
- **dotenv-java** — loads `.env` file in local profile via `DotEnvLoader.java`

## Package layout

```
com.neuroforged.leadsystem
├── client/          # External API clients (CalendlyApiClient)
├── config/          # Spring config beans (CORS, startup seed, dotenv, API token filter)
├── controller/      # REST controllers
├── dto/             # Request/response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── health/          # Spring Boot Actuator health indicators (CalendlyHealthIndicator)
├── mapper/          # MapStruct mappers
├── repository/      # Spring Data JPA repositories
├── scheduler/       # @Scheduled jobs (webhook retry, Calendly polling)
├── security/        # JWT, SecurityConfig, auth filters
├── service/         # Interfaces
└── service/impl/    # Implementations
```

## Key entities

| Entity | Table | Purpose |
|--------|-------|---------|
| `Lead` | `lead` | Lead capture data, scoped to `clientId` (String) |
| `Client` | `client` | Agency/client record, `id` is Long |
| `User` | `users` | Admin users — email/password/role |
| `CalendlyAccount` | `calendly_account` | OAuth tokens per client (`clientId` Long, unique). Fields: `usePolling` (bool), `lastPolledAt` (LocalDateTime) for polling-mode clients |
| `CalendlyIntegration` | `calendly_integration` | Tracks OAuth flow state + `completed` flag |
| `CalendlyMeeting` | `calendly_meeting` | Booked meetings synced from Calendly (webhook or polling) |
| `CalendlyWebhookLog` | `calendly_webhook_log` | Webhook event log — has `success`, `retryCount`, `errorDetails` |

**Important:** `Lead.clientId` is a `String`, but `Client.id` and `CalendlyAccount.clientId` are `Long`. They are related but not a foreign key — the chatbot passes a string client ID when submitting leads.

## Auth model

Two parallel auth paths, both wired in `SecurityConfig`:

1. **`X-Api-Key` header** (`ApiTokenFilter`) — for chatbot lead submission to `/api/leads/**`. Matches against `NEUROFORGED_INTERNAL_TOKEN`.
2. **JWT Bearer token** (`JwtAuthenticationFilter`) — for admin dashboard. Issued by `POST /auth/login`.

JWT filter only runs if the API key filter didn't authenticate the request.

Public endpoints: `/auth/**`, `/api/calendly/webhook`, `/api/calendly/oauth/callback`, `/actuator/health`, all `OPTIONS` (CORS preflight).

## Schedulers

| Scheduler | Interval | Purpose |
|-----------|----------|---------|
| `CalendlyWebhookRetryScheduler` | Every 5 min | Retries failed webhook deliveries (max 3 attempts, then dead-letters + admin email) |
| `CalendlyPollingScheduler` | Every 15 min (configurable via `calendly.polling-interval-ms`) | Polls Calendly API for accounts with `usePolling=true` — for clients on plans without webhook support |

## Profiles & configuration

| Profile | File | How activated |
|---------|------|--------------|
| `local` | `application-local.yml` | Default (`spring.profiles.active: local` in `application.yml`) |
| `prod` | `application-prod.yml` | Set `SPRING_PROFILES_ACTIVE=prod` in Coolify |

Local dev loads secrets from `.env` in the project root via `DotEnvLoader`. Prod reads them from Coolify environment variables.

### Full env var list

```
# Database
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

# Mail (SMTP)
SPRING_MAIL_HOST
SPRING_MAIL_PORT
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
SPRING_MAIL_FROM          # local profile key
NEUROFORGED_MAIL_FROM     # prod profile key
SPRING_MAIL_SMTP_AUTH
SPRING_MAIL_SMTP_STARTTLS_ENABLE

# Calendly OAuth
CALENDLY_CLIENT_ID
CALENDLY_CLIENT_SECRET
CALENDLY_REDIRECT_URI
CALENDLY_WEBHOOK_SIGNING_KEY

# App secrets
NEUROFORGED_JWT_SECRET          # 64+ char random string — rotated, invalidates all tokens
NEUROFORGED_INTERNAL_TOKEN      # Shared secret for chatbot API key auth
NEUROFORGED_ADMIN_EMAIL
NEUROFORGED_ADMIN_PASSWORD

# CORS
NEUROFORGED_CORS_ALLOWED_ORIGINS   # prod only — comma-separated frontend URLs

# Scraper integration
SCRAPER_API_URL                    # default: https://scraper.alchemizeiq.com
SCRAPER_API_KEY                    # shared secret for ScraperServiceImpl
```

## Common conventions

- **Entities**: `@Data @Entity` with `@Builder @NoArgsConstructor @AllArgsConstructor` for most, `@Data @Entity` only for simpler ones like `Client`, `CalendlyIntegration`.
- **Services**: always interface + impl pair. Interface in `service/`, impl in `service/impl/`.
- **Config properties**: add to both `application-local.yml` (hardcoded or env var ref) and `application-prod.yml` (always `${ENV_VAR}` pattern). Never hardcode secrets in source.
- **No Flyway/Liquibase**: schema changes go directly as entity field additions. Hibernate auto-creates/alters columns. Be careful with `NOT NULL` columns — add a DB default or make them nullable.
- **Java 21 preview**: `STR."""..."""` template strings are used in `LeadServiceImpl`. The Maven compiler plugin has `--enable-preview` set; keep this in mind when adding new Java 21 features.

## Build & run

```bash
# Local
./mvnw spring-boot:run

# Build JAR
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Docker
docker build -t leadsystem .
docker run -p 8080:8080 --env-file .env leadsystem
```

## Deployment

- **Platform**: Coolify (Hetzner CX23, 178.105.49.110 — same server as scraper)
- **Prod URL**: api.alchemizeiq.com (DNS pointed, Coolify service exists — needs env vars)
- **Prod deploy**: push to `master` → Coolify auto-deploys
- **Dev deploy**: push to `develop` → Coolify dev service (to be configured)
- **PR workflow**: feature branch → PR to `develop` → merge to `master` for prod
- **`gh` CLI**: installed and working

## Jira board

Project: **KAN** on [alchemizeiq.atlassian.net](https://alchemizeiq.atlassian.net)

| Epic | Status | Tickets |
|------|--------|---------|
| **KAN-4** Security & Auth Hardening | ✅ All done | KAN-8, 9, 10, 11 |
| **KAN-5** Calendly Integration Reliability | 🔄 In review | KAN-12 ✓ (PR#5), KAN-13 ✓, KAN-14 ✓, KAN-15 ✓, KAN-16 ✓, KAN-17 ✓ (PR#5) |
| **KAN-6** Lead Pipeline Polish | 🔲 Open | KAN-18, KAN-19, KAN-20 |
| **KAN-7** Post-MVP Scalability | 🔲 Open | KAN-21, KAN-22 |

## Known issues / gotchas

- `Lead.clientId` is a `String` — not a FK to `Client`. The duplicate email check (`LeadRepository.existsByEmail`) is global, not per-client. KAN-19 fixes this.
- `LeadNotFoundException` is an empty class, never used. KAN-20 cleans this up.
- Notification emails in `LeadServiceImpl.sendNotificationEmails()` are hardcoded to internal addresses. KAN-18 fixes this.

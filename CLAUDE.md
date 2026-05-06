# Alchemize Lead System — Claude Context

## What this is

A multi-tenant lead-capture and Calendly-integration backend. AI chatbots (one per client) submit leads via API key. The admin dashboard uses JWT/cookie auth. Each client connects their Calendly account via OAuth; the system tracks meetings and matches them to leads.

**Company:** Alchemize (repo and package names still say `neuroforged` — do not rename them).

## Tech stack

- **Java 21** (no preview features — `--enable-preview` and STR templates removed)
- **Spring Boot 3.5.3** — web, security, data-jpa, mail, webflux, validation, actuator, aop
- **PostgreSQL** — JPA with `ddl-auto: validate`; schema managed by **Flyway** (migrations in `src/main/resources/db/migration/`)
- **Maven** — build with `./mvnw clean package -DskipTests`
- **Docker** — multi-stage build, deployed on **Coolify** (Hetzner, same server as scraper)
- **Lombok** — `@Data`, `@Builder`, `@RequiredArgsConstructor` used throughout
- **MapStruct** — DTO mapping (`ClientMapper`, `LeadMapper`)
- **jjwt 0.11.5** — JWT signing/validation
- **dotenv-java** — loads `.env` file in local profile via `DotEnvLoader.java`

## Package layout

```
com.neuroforged.leadsystem
├── audit/           # AuditAspect (Spring AOP @Around on mutating endpoints)
├── client/          # External API clients (CalendlyApiClient)
├── config/          # Spring config beans (CORS, startup seed, dotenv, API token filter, TenantFilterInterceptor)
├── controller/      # REST controllers
├── dto/             # Request/response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── health/          # Spring Boot Actuator health indicators (CalendlyHealthIndicator)
├── mapper/          # MapStruct mappers
├── metrics/         # Micrometer/Prometheus metrics (LeadSystemMetrics)
├── repository/      # Spring Data JPA repositories
├── scheduler/       # @Scheduled jobs (webhook retry, Calendly polling, ReScrapeScheduler)
├── security/        # JWT, SecurityConfig, auth filters
├── service/         # Interfaces
└── service/impl/    # Implementations
```

## Key entities

| Entity | Table | Purpose |
|--------|-------|---------|
| `Lead` | `lead` | Lead capture data, scoped to `clientId` (String); `assignedTo`, `relevantKbSnippet` added |
| `Client` | `client` | Agency/client record, `id` is Long; `logoUrl`, `accentColor`, `webhookUrl`, `webhookSecret` |
| `User` | `users` | Admin users — email/password/role |
| `CalendlyAccount` | `calendly_account` | OAuth tokens per client (`clientId` Long, unique) |
| `CalendlyIntegration` | `calendly_integration` | Tracks OAuth flow state + `completed` flag |
| `CalendlyMeeting` | `calendly_meeting` | Booked meetings synced from Calendly (webhook or polling) |
| `CalendlyWebhookLog` | `calendly_webhook_log` | Webhook event log — has `success`, `retryCount`, `errorDetails` |
| `KnowledgeBaseDocument` | `knowledge_base_document` | Scraped client KB docs used for lead enrichment |
| `ScrapeJob` | `scrape_job` | Background scrape job records |
| `LeadComment` | `lead_comment` | Comments on leads (GET/POST /api/leads/{id}/comments) |
| `NotificationChannel` | `notification_channel` | Per-client Slack/email alert channels per event type |
| `AuditEvent` | `audit_event` | Audit log of all mutating API operations |
| `LeadRoutingRule` | `lead_routing_rule` | Priority-ordered field-match rules → assignTo |

**Important:** `Lead.clientId` is a `String`, but `Client.id` and `CalendlyAccount.clientId` are `Long`. They are related but not a foreign key — the chatbot passes a string client ID when submitting leads. See LSB-89 for the planned FK refactor.

## Auth model

Two parallel auth paths, both wired in `SecurityConfig`:

1. **`X-Api-Key` header** (`ApiTokenFilter`) — for chatbot lead submission to `/api/leads/**` and `/api/v1/leads/**`. Matches against per-client API keys in DB (resolved by `ApiTokenFilter`).
2. **JWT Bearer token + httpOnly cookie** (`JwtAuthenticationFilter`) — for admin dashboard. Issued by `POST /auth/login`. Cookies: `alchemize_at` (access, short-lived) + `alchemize_rt` (refresh, 7-day).

Public endpoints: `/auth/**`, `/api/calendly/webhook`, `/api/calendly/oauth/callback`, `/actuator/health`, all `OPTIONS` (CORS preflight).

## Hibernate tenant filters

Row-level multi-tenant isolation for CLIENT-role requests, enabled by `TenantFilterInterceptor`:

- `clientFilter` — String param, on `Lead` (clientId column is TEXT)
- `longClientFilter` — Long param, on `CalendlyMeeting`, `CalendlyAccount`, `ScrapeJob`, `KnowledgeBaseDocument`

`@FilterDef` for `longClientFilter` is declared **once** on `CalendlyAccount`. All other entities use `@Filter` only. Duplicate `@FilterDef` declarations cause Hibernate startup failure.

## Schedulers

| Scheduler | Interval | Purpose |
|-----------|----------|---------|
| `CalendlyWebhookRetryScheduler` | Every 5 min | Retries failed webhook deliveries (max 3 attempts, then dead-letters + admin email) |
| `CalendlyPollingScheduler` | Every 15 min | Polls Calendly API for accounts with `usePolling=true` |
| `ReScrapeScheduler` | 03:00 UTC daily | Re-scrapes clients whose `scrapeFrequencyDays` has elapsed |

## Flyway migrations

Migrations live in `src/main/resources/db/migration/`. Current state:

| Version | Description |
|---------|-------------|
| V1–V11 | Core schema, auth, branding, webhooks, comments, key rotation, meetings |
| V12 | `notification_channel` + `notification_channel_events` |
| V13 | `audit_event` |
| V14 | `lead.relevant_kb_snippet` |
| V15 | `lead.assigned_to` + `lead_routing_rule` |

**Rule:** Every entity field addition requires a migration file. Do NOT rely on `ddl-auto: update` — it is set to `validate` in prod. `baseline-on-migrate=true` is set so V1 is recorded without running on existing prod DBs.

## Profiles & configuration

| Profile | File | How activated |
|---------|------|--------------|
| `local` | `application-local.yml` | Default (`spring.profiles.active: local` in `application.yml`) |
| `prod` | `application-prod.yml` | Set `SPRING_PROFILES_ACTIVE=prod` in Coolify |

Local dev loads secrets from `.env` in the project root via `DotEnvLoader`. Prod reads from Coolify environment variables.

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
NEUROFORGED_MAIL_FROM     # prod profile key

# Calendly OAuth
CALENDLY_CLIENT_ID
CALENDLY_CLIENT_SECRET
CALENDLY_REDIRECT_URI
CALENDLY_WEBHOOK_SIGNING_KEY

# App secrets
NEUROFORGED_JWT_SECRET          # 64+ char random string
NEUROFORGED_INTERNAL_TOKEN      # Legacy; per-client API keys now in DB
NEUROFORGED_ADMIN_EMAIL
NEUROFORGED_ADMIN_PASSWORD
NEUROFORGED_ENCRYPTION_KEY      # base64 32-byte AES key for CalendlyAccount token encryption

# CORS
NEUROFORGED_CORS_ALLOWED_ORIGINS   # prod only — comma-separated frontend URLs

# Scraper integration
SCRAPER_API_URL                    # default: https://scraper.alchemizeiq.com
SCRAPER_API_KEY
```

## Common conventions

- **Entities**: `@Data @Entity` with `@Builder @NoArgsConstructor @AllArgsConstructor`.
- **Services**: always interface + impl pair. Interface in `service/`, impl in `service/impl/`.
- **Config properties**: add to both `application-local.yml` and `application-prod.yml` (always `${ENV_VAR}` pattern in prod). Never hardcode secrets.
- **Flyway**: every schema change needs a `VN__description.sql`. Adding `NOT NULL` columns without a DB default requires a nullable migration + backfill, or a default value in the migration SQL.
- **`@FilterDef` rule**: declare each named filter definition **once** globally — on the primary entity (e.g. `longClientFilter` on `CalendlyAccount`). All other entities that use the filter get `@Filter` only. Duplicate `@FilterDef` = Hibernate boot failure.

## Build & run

```bash
# Local
./mvnw spring-boot:run

# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Docker
docker build -t leadsystem .
docker run -p 8080:8080 --env-file .env leadsystem
```

## Deployment

- **Platform**: Coolify (Hetzner CX23, 178.105.49.110)
- **Prod URL**: https://api.alchemizeiq.com — auto-deploys on push to `master`
- **Dev URL**: https://api-dev.alchemizeiq.com — auto-deploys on push to `develop`
- **PR workflow**: feature branch → PR to `develop` → merge to `master` for prod

## Jira board

Project: **LSB** on [alchemizeiq.atlassian.net](https://alchemizeiq.atlassian.net/jira/software/projects/LSB)

## Known issues / gotchas

- **CORS wildcard + credentials**: `setAllowedOrigins("*")` + `setAllowedCredentials(true)` is illegal in Spring. `CorsConfig` branches on `*` and calls `setAllowedOriginPatterns("*")` instead (LSB-79, fixed). If this breaks again, that's the first place to look.
- **`Lead.clientId` is a `String`**: not a FK to `Client`. The duplicate email check is per-client (composite unique index on `email + client_id`). Auth scoping uses `AuthPrincipalUtil.resolveStringClientIdForCaller()` which relies on `Lead.clientId == Long.toString(client.id)` — fragile. See LSB-89 for the FK refactor design ticket.
- **Flyway baseline**: `baseline-on-migrate=true` records V1 as applied without running it. When adding new migrations, test against a clean DB and also against an existing one with the baseline applied.
- **CLIENT role auth (PORTAL-82/83/84)**: `User.clientId` is `Long` nullable. CLIENT JWTs include a `clientId` claim. Read via `AuthPrincipalUtil.currentClientId()` / `currentRole()`. Wrap clientId params in `resolveClientIdForCaller(Long)` or `resolveStringClientIdForCaller(String)`. For get-by-id, call `assertCanAccessClient(Long)`.
- **Rate limiter (LSB-37)**: Bucket4j on `POST /api/leads`. Rapid test submissions need `sleep 2` between requests or they get throttled. In-memory — resets on restart.
- **Em dash in bash heredoc**: Unicode `—` in curl JSON payloads causes 400 "Failed to read request". Use ASCII `-` instead.
- **Schema drift audit (LSB-90)**: `SchemaAuditRunner` (`@Profile("prod")`) runs on startup, compares Hibernate entity column mappings against `information_schema.columns`. Logs WARN per missing column. `neuroforged.schema-audit.fail-on-drift: false` — set to `true` to abort startup on drift.
- **`@FilterDef` uniqueness**: Hibernate requires each named `@FilterDef` to be declared exactly once across all entities. `longClientFilter` is declared on `CalendlyAccount` only. Adding it to another entity will cause startup failure with "Ambiguous FilterDef" error.
- **`LeadServiceImplTest` mocks**: `LeadServiceImpl` has many constructor deps. All must be `@Mock`-annotated in the test: `LeadRepository`, `LeadNotificationService`, `NotificationService`, `LeadEnrichmentService`, `LeadRoutingService`, `LeadMapper`, `ClientRepository`, `OutboundWebhookService`, `LeadSystemMetrics`.

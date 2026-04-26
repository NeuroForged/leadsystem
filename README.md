# NeuroForged Lead System

Backend for NeuroForged's AI-powered lead capture and Calendly integration platform.

Each agency client gets an AI chatbot that captures leads via the API. The system routes leads, sends email notifications, and syncs Calendly meetings per client.

## Tech Stack

- **Java 21** · Spring Boot 3.5.3 · Maven
- **PostgreSQL** · JPA/Hibernate (`ddl-auto: update`)
- **Spring Security** — dual auth: JWT for admin dashboard, `X-Api-Key` for chatbot lead submission
- **Calendly OAuth 2.0** — per-client Calendly account connection
- **Docker** — deployed on Render

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker (for local DB)
- A `.env` file in the project root (see `.env.example`)

## Local Setup

```bash
# 1. Copy env template and fill in values
cp .env.example .env

# 2. Start a local PostgreSQL instance
docker run -d \
  --name leadsystem-db \
  -e POSTGRES_DB=leadsystem \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# 3. Run the app
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

## Auth

| Method | Header | Used by |
|--------|--------|---------|
| JWT Bearer | `Authorization: Bearer <token>` | Admin dashboard |
| API Key | `X-Api-Key: <token>` | Chatbot lead submission |

Obtain a JWT: `POST /auth/login` with `{"email": "...", "password": "..."}`.

## Key Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/login` | None | Get JWT |
| POST | `/api/leads` | API Key | Submit a lead |
| GET | `/api/leads` | JWT | List all leads |
| GET | `/api/clients` | JWT | List clients |
| POST | `/api/clients` | JWT | Create client |
| GET | `/api/calendly/authorize?clientId=` | JWT | Start Calendly OAuth flow |
| GET | `/api/calendly/oauth/callback` | None | Calendly OAuth callback |
| POST | `/api/calendly/webhook` | Signed | Calendly webhook receiver |

## Calendly Integration

1. Admin calls `GET /api/calendly/authorize?clientId={id}` → redirected to Calendly
2. Client authorises → Calendly redirects to `/api/calendly/oauth/callback`
3. Tokens stored in `CalendlyAccount`; auto-refreshed before expiry
4. Meeting events received via signed webhook and stored as `CalendlyMeeting`

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Production — Render auto-deploys on push |
| `develop` | Development — Render dev service auto-deploys on push |
| `claude/*` | AI agent work branches — PR into `develop` |

## Build

```bash
# Compile
./mvnw compile

# Package JAR (skip tests)
./mvnw clean package -DskipTests

# Docker
docker build -t leadsystem .
```

## Environment Variables

See `.env.example` for the full list with descriptions.

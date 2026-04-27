# Contributing

## Conventions

### Entities
Use `@Data @Entity` as the base. Add `@Builder @NoArgsConstructor @AllArgsConstructor` when the entity is built via a builder pattern. Always add `@NoArgsConstructor @AllArgsConstructor` alongside `@Builder` — Hibernate requires a no-args constructor.

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyEntity { ... }
```

### Services
Always interface + impl pair. Interface in `service/`, implementation in `service/impl/`. Use `@RequiredArgsConstructor` for injection.

### Configuration
- Add new config properties to **both** `application-local.yml` and `application-prod.yml`
- Local: hardcode a dev default or use `${ENV_VAR}` if the value is secret
- Prod: always `${ENV_VAR}` — no hardcoded values
- Document every new env var in `.env.example`

### Secrets
Never hardcode secrets in source. No exceptions. Use `@Value("${property.path}")` injected from env vars.

### Schema changes
No Flyway — Hibernate `ddl-auto: update` handles it. When adding `NOT NULL` columns to existing tables, either make the field nullable in Java or add a `columnDefinition` with a DB default to avoid startup failures on existing data.

### Java 21 features
Preview features are enabled (`--enable-preview`). `STR.""""""` template strings are used in the codebase — this is intentional.

## Branch workflow

```
master          ← production (auto-deploy on Render)
  └── develop   ← dev environment (auto-deploy on Render dev service)
        └── claude/feature-name   ← AI agent branches → PR to develop
        └── feature/your-feature  ← human branches → PR to develop
```

Merge `develop` → `master` when dev looks good. Never push directly to `master`.

## Commit messages

Follow conventional commits: `type(scope): description`

Types: `feat`, `fix`, `chore`, `docs`, `refactor`

Include the Jira ticket key in the scope when applicable:
```
fix(KAN-8): encode admin password with BCrypt on startup seed
feat(KAN-14): implement Calendly access token refresh
```

## Environment setup

See `.env.example` for all required environment variables and how to obtain them.

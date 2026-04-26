# Work Log

Running log of Claude Code sessions. Updated at the end of each session.
Most recent session at the top.

---

## Session: 2026-04-26

**Branch:** `claude/nostalgic-clarke-10c237`  
**PRs:** [#1](https://github.com/NeuroForged/leadsystem/pull/1) · [#2](https://github.com/NeuroForged/leadsystem/pull/2)

### Tickets closed
| Ticket | Title |
|--------|-------|
| [KAN-8](https://alchemizeiq.atlassian.net/browse/KAN-8) | Fix admin password not BCrypt encoded on startup seed |
| [KAN-9](https://alchemizeiq.atlassian.net/browse/KAN-9) | Rotate and externalise hardcoded JWT secret |
| [KAN-10](https://alchemizeiq.atlassian.net/browse/KAN-10) | Persist OAuth state to DB — remove in-memory map |
| [KAN-11](https://alchemizeiq.atlassian.net/browse/KAN-11) | Implement CORS configuration |
| [KAN-13](https://alchemizeiq.atlassian.net/browse/KAN-13) | Implement Calendly webhook signature verification |
| [KAN-14](https://alchemizeiq.atlassian.net/browse/KAN-14) | Implement Calendly access token refresh |
| [KAN-15](https://alchemizeiq.atlassian.net/browse/KAN-15) | Handle invitee.canceled and invitee.rescheduled webhook events |
| [KAN-16](https://alchemizeiq.atlassian.net/browse/KAN-16) | Implement webhook retry logic |

### Key decisions made
- `CalendlyWebhookLog` was missing `@NoArgsConstructor`/`@AllArgsConstructor` — fixed (would have caused Hibernate failures)
- `MeetingStatus` enum added to `CalendlyMeeting` (`SCHEDULED`, `CANCELLED`, `RESCHEDULED`, `NO_SHOW`)
- `CalendlyTokenRefreshService` created as the single entry point before any Calendly API call — callers must use `ensureFreshToken(clientId)` before accessing access tokens

### Environment setup done this session
- Java 21 (Temurin) installed and JAVA_HOME set
- `gh` CLI added to user PATH
- `develop` branch created and pushed
- `.gitattributes` added to enforce LF line endings
- `CLAUDE.md` written with full project context

### Open tickets remaining (by priority)
- KAN-12 — Calendly polling fallback (High)
- KAN-17 — Spring Boot Actuator health check (High)
- KAN-18 — Externalise notification email recipients (Medium)
- KAN-19 — Fix duplicate email check scope to per-client (Medium)
- KAN-20 — Fix empty LeadNotFoundException (Medium)

---

<!-- Template for future sessions:

## Session: YYYY-MM-DD

**Branch:** `claude/...`
**PRs:** [#N](url)

### Tickets closed
| Ticket | Title |
|--------|-------|

### Key decisions made
-

### Open tickets remaining
-

-->

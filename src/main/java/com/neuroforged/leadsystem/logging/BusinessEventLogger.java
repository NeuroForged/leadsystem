package com.neuroforged.leadsystem.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Structured business event logger — LSB-133.
 *
 * Logs to the named "EVENTS" logger which Logback routes to its own
 * plain-text appenders (stdout + rolling file), separate from the full
 * technical JSON log.  Every method produces one consistently formatted
 * line that is human-readable at a glance in Coolify or Grafana.
 *
 * Format: HH:mm:ss [CATEGORY] outcome  key=value  key=value  ...
 */
@Component
public class BusinessEventLogger {

    private static final Logger EVENTS = LoggerFactory.getLogger("EVENTS");
    private static final DateTimeFormatter MEETING_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    // ── Leads ──────────────────────────────────────────────────────────────────

    public void leadReceived(String clientName, String clientId, String email,
                             Integer score, boolean enriched, String assignedTo) {
        EVENTS.info("[LEAD]    {} received     client={}  email={}  score={}  enriched={}{}",
                ok(),
                label(clientName, clientId),
                email,
                score != null ? score : "n/a",
                enriched,
                assignedTo != null ? "  routed=" + assignedTo : "");
    }

    public void leadDuplicate(String clientName, String clientId, String email) {
        EVENTS.info("[LEAD]    {} duplicate    client={}  email={}",
                fail(), label(clientName, clientId), email);
    }

    public void leadInvalid(String clientName, String clientId, String reason, String value) {
        EVENTS.info("[LEAD]    {} invalid      client={}  reason={}{}",
                fail(),
                label(clientName, clientId),
                reason,
                value != null ? "  value=" + value : "");
    }

    // ── Calendly meetings ──────────────────────────────────────────────────────

    public void meetingBooked(String clientName, String inviteeEmail,
                              String eventType, ZonedDateTime startTime) {
        EVENTS.info("[MEETING] {} booked       client={}  invitee={}  event=\"{}\"  starts={}",
                ok(),
                clientName != null ? clientName : "unknown",
                inviteeEmail,
                eventType != null ? eventType : "unknown",
                startTime != null ? startTime.format(MEETING_FMT) : "unknown");
    }

    public void meetingCancelled(String clientName, String inviteeEmail, String eventType) {
        EVENTS.info("[MEETING] {} cancelled    client={}  invitee={}  event=\"{}\"",
                fail(),
                clientName != null ? clientName : "unknown",
                inviteeEmail,
                eventType != null ? eventType : "unknown");
    }

    // ── Outbound webhooks (client-configured) ──────────────────────────────────

    public void outboundWebhookDelivered(String clientName, String url, long latencyMs) {
        EVENTS.info("[WEBHOOK] {} delivered    client={}  url={}  latency={}ms",
                ok(), clientName, sanitiseUrl(url), latencyMs);
    }

    public void outboundWebhookFailed(String clientName, String url,
                                      int httpStatus, int attempt, int maxAttempts) {
        EVENTS.info("[WEBHOOK] {} failed({}/{}) client={}  url={}  status={}",
                fail(), attempt, maxAttempts, clientName, sanitiseUrl(url), httpStatus);
    }

    public void outboundWebhookDeadLettered(String clientName, String url, int attempts) {
        EVENTS.info("[WEBHOOK] {} dead-letter  client={}  url={}  attempts={}",
                fail(), clientName, sanitiseUrl(url), attempts);
    }

    // ── Scrape jobs ────────────────────────────────────────────────────────────

    public void scrapeStarted(String clientName, String url, int maxPages) {
        EVENTS.info("[SCRAPE]  {} started      client={}  url={}  maxPages={}",
                ok(), clientName, url, maxPages);
    }

    public void scrapeCompleted(String clientName, int pages, int wordCount, long durationMs) {
        EVENTS.info("[SCRAPE]  {} completed    client={}  pages={}  words={}  duration={}",
                ok(), clientName, pages, wordCount, formatDuration(durationMs));
    }

    public void scrapeFailed(String clientName, String error, long durationMs) {
        EVENTS.info("[SCRAPE]  {} failed       client={}  error={}  after={}",
                fail(), clientName, error, formatDuration(durationMs));
    }

    // ── Schedulers ─────────────────────────────────────────────────────────────

    public void schedulerRun(String schedulerName, Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) -> sb.append("  ").append(k).append("=").append(v));
        EVENTS.info("[SCHED]   {} {}{}",
                ok(), schedulerName, sb);
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    public void authFailed(String reason, String email, String ip) {
        EVENTS.info("[AUTH]    {} failed       reason={}  email={}  ip={}",
                fail(), reason, email, ip != null ? ip : "unknown");
    }

    public void authSuccess(String email) {
        EVENTS.info("[AUTH]    {} login        email={}", ok(), email);
    }

    // ── Calendly OAuth ─────────────────────────────────────────────────────────

    public void calendlyOAuthConnected(String clientName) {
        EVENTS.info("[OAUTH]   {} connected    client={}  provider=Calendly",
                ok(), clientName);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String ok()   { return "✓"; }
    private String fail() { return "✗"; }

    private String label(String name, String id) {
        return name != null ? name : "client-" + id;
    }

    /** Strip query params to avoid accidentally logging tokens in URLs. */
    private String sanitiseUrl(String url) {
        if (url == null) return "null";
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format("%.1fs", ms / 1000.0);
        long mins = ms / 60_000;
        long secs = (ms % 60_000) / 1000;
        return mins + "m" + secs + "s";
    }
}

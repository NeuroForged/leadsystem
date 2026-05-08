package com.neuroforged.leadsystem.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralised Micrometer metrics for the lead system — LSB-105, LSB-124–129.
 *
 * Metrics exposed:
 *   leadsystem_leads_received_total{client_id}              — lead intake counter
 *   leadsystem_leads_duplicate_total{client_id}             — duplicate rejections (LSB-124)
 *   leadsystem_enrichment_hit_total{outcome}                — KB enrichment hit/miss (LSB-125)
 *   leadsystem_routing_matched_total{rule_id,client_id}     — routing rule match (LSB-126)
 *   leadsystem_outbound_webhook_total{client_id,status}     — webhook delivery (LSB-127)
 *   leadsystem_auth_failure_total{reason}                   — auth failures (LSB-128)
 *   leadsystem_calendly_poll_meetings_synced_total{client_id} — meetings synced (LSB-129)
 *   leadsystem_calendly_webhook_total{status}               — Calendly inbound webhook
 *   leadsystem_scheduler_runs_total{scheduler}              — scheduled job executions
 *   leadsystem_kb_fetch_duration_seconds{client_id}         — KB fetch timer
 */
@Component
public class LeadSystemMetrics {

    private final MeterRegistry registry;

    public LeadSystemMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // ── Leads ─────────────────────────────────────────────────────────────────

    public void recordLeadReceived(String clientId) {
        Counter.builder("leadsystem.leads.received")
                .description("Total leads received per client")
                .tag("client_id", clientId != null ? clientId : "unknown")
                .register(registry)
                .increment();
    }

    /** LSB-124: duplicate lead rejection per client */
    public void recordLeadDuplicate(String clientId) {
        Counter.builder("leadsystem.leads.duplicate")
                .description("Duplicate lead submissions rejected per client")
                .tag("client_id", clientId != null ? clientId : "unknown")
                .register(registry)
                .increment();
    }

    /** LSB-125: KB enrichment outcome — outcome: "hit" | "miss" | "skipped" | "error" */
    public void recordEnrichmentOutcome(String outcome) {
        Counter.builder("leadsystem.enrichment.hit")
                .description("Lead enrichment KB lookup outcomes")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    /** LSB-126: routing rule matched — ruleId and clientId tags */
    public void recordRoutingMatched(String ruleId, String clientId) {
        Counter.builder("leadsystem.routing.matched")
                .description("Lead routing rule match events")
                .tag("rule_id", ruleId != null ? ruleId : "unknown")
                .tag("client_id", clientId != null ? clientId : "unknown")
                .register(registry)
                .increment();
    }

    /** LSB-127: outbound webhook delivery — status: "success" | "failure" | "skipped" */
    public void recordOutboundWebhook(String clientId, String status) {
        Counter.builder("leadsystem.outbound.webhook")
                .description("Outbound webhook delivery outcomes per client")
                .tag("client_id", clientId != null ? clientId : "unknown")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    /** LSB-128: auth failure — reason: "bad_credentials" | "invalid_token" | "refresh_token_used" | "user_not_found" | "invalid_token_type" */
    public void recordAuthFailure(String reason) {
        Counter.builder("leadsystem.auth.failure")
                .description("Authentication failure events by reason")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    /** LSB-129: meetings synced per Calendly polling run per client */
    public void recordCalendlyPollSynced(String clientId, int count) {
        Counter.builder("leadsystem.calendly.poll.meetings.synced")
                .description("Meetings synced per Calendly polling run per client")
                .tag("client_id", clientId != null ? clientId : "unknown")
                .register(registry)
                .increment(count);
    }

    // ── Calendly webhooks ─────────────────────────────────────────────────────

    /** status: "success" | "failure" | "rejected" */
    public void recordWebhook(String status) {
        Counter.builder("leadsystem.calendly.webhook")
                .description("Calendly webhook events by outcome")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    // ── Schedulers ────────────────────────────────────────────────────────────

    public void recordSchedulerRun(String schedulerName) {
        Counter.builder("leadsystem.scheduler.runs")
                .description("Number of scheduled job executions")
                .tag("scheduler", schedulerName)
                .register(registry)
                .increment();
    }

    // ── KB fetch ──────────────────────────────────────────────────────────────

    public Timer.Sample startKbFetchTimer() {
        return Timer.start(registry);
    }

    public void stopKbFetchTimer(Timer.Sample sample, String clientId, boolean success) {
        sample.stop(Timer.builder("leadsystem.kb.fetch.duration")
                .description("Knowledge base fetch and store duration")
                .tag("client_id", clientId)
                .tag("outcome", success ? "success" : "error")
                .register(registry));
    }
}

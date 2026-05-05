package com.neuroforged.leadsystem.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralised Micrometer metrics for the lead system — LSB-105.
 *
 * Metrics exposed:
 *   leadsystem_leads_received_total{client_id}     — lead intake counter
 *   leadsystem_calendly_webhook_total{status}       — webhook success/failure/rejected
 *   leadsystem_scheduler_runs_total{scheduler}      — scheduled job executions
 *   leadsystem_kb_fetch_duration_seconds{client_id} — KB fetch timer
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

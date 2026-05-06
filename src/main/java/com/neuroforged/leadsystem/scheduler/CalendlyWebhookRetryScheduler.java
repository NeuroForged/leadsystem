package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendlyWebhookRetryScheduler {

    private final CalendlyWebhookService webhookService;
    private final LeadSystemMetrics metrics;
    private final BusinessEventLogger eventLogger;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void retryFailedWebhooks() {
        log.debug("Calendly webhook retry scheduler firing");
        metrics.recordSchedulerRun("calendly-webhook-retry");
        CalendlyWebhookService.RetryResult result = webhookService.retryFailedWebhooks();
        eventLogger.schedulerRun("webhook-retry",
                Map.of("retried", result.retried(), "dead-lettered", result.deadLettered()));
    }
}

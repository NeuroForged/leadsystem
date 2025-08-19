package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendlyWebhookRetryScheduler {

    private final CalendlyWebhookService webhookService;

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void retryFailedWebhooks() {
        log.info("Retrying failed Calendly webhook deliveries...");
        webhookService.retryFailedWebhooks();
    }
}

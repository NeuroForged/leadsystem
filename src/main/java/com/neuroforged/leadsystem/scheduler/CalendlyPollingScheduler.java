package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.service.CalendlyPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendlyPollingScheduler {

    private final CalendlyPollingService pollingService;
    private final LeadSystemMetrics metrics;
    private final BusinessEventLogger eventLogger;

    @Scheduled(fixedDelayString = "${calendly.polling-interval-ms:900000}")
    public void pollCalendlyAccounts() {
        log.debug("Calendly polling scheduler firing");
        metrics.recordSchedulerRun("calendly-polling");
        int synced = pollingService.pollAllAccounts();
        eventLogger.schedulerRun("calendly-poll", Map.of("meetings-synced", synced));
    }
}

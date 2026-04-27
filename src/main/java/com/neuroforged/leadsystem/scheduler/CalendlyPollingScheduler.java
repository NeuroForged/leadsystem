package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.service.CalendlyPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendlyPollingScheduler {

    private final CalendlyPollingService pollingService;

    @Scheduled(fixedDelayString = "${calendly.polling-interval-ms:900000}")
    public void pollCalendlyAccounts() {
        log.info("Running Calendly polling cycle for accounts without webhooks...");
        pollingService.pollAllAccounts();
    }
}

package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.ScrapeJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReScrapeScheduler {

    private final ClientRepository clientRepository;
    private final ScrapeJobService scrapeJobService;
    private final LeadSystemMetrics metrics;

    @Scheduled(cron = "0 0 3 * * *") // 03:00 UTC daily
    public void triggerDueReScrapes() {
        metrics.recordSchedulerRun("rescrape");
        List<Client> candidates = clientRepository.findAll().stream()
                .filter(c -> c.getScrapeFrequencyDays() != null && c.getScrapeFrequencyDays() > 0)
                .filter(c -> c.getWebsiteUrl() != null && !c.getWebsiteUrl().isBlank())
                .filter(c -> isDue(c))
                .toList();

        if (candidates.isEmpty()) {
            log.debug("ReScrapeScheduler: no clients due for re-scrape");
            return;
        }

        log.info("ReScrapeScheduler: triggering re-scrape for {} client(s)", candidates.size());
        for (Client client : candidates) {
            try {
                scrapeJobService.createJob(client.getId(), client.getWebsiteUrl(), 1000, "scheduler");
                log.info("Re-scrape job created for client id={} (frequency={}d)", client.getId(), client.getScrapeFrequencyDays());
            } catch (Exception ex) {
                log.error("Failed to create re-scrape job for client id={}: {}", client.getId(), ex.getMessage());
            }
        }
    }

    private boolean isDue(Client client) {
        if (client.getLastScrapedAt() == null) return true;
        LocalDateTime nextDue = client.getLastScrapedAt().plusDays(client.getScrapeFrequencyDays());
        return LocalDateTime.now().isAfter(nextDue);
    }
}

package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.ScrapeJobResponse;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.service.ScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class ScraperServiceImpl implements ScraperService {

    private final WebClient webClient;

    public ScraperServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${scraper.api-url}") String scraperApiUrl,
            @Value("${scraper.api-key}") String scraperApiKey) {
        this.webClient = webClientBuilder
                .baseUrl(scraperApiUrl)
                .defaultHeader("X-API-Key", scraperApiKey)
                .build();
    }

    @Override
    public ScrapeJobResponse triggerScrape(String websiteUrl, String clientId) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            throw new ResourceNotFoundException("Client has no websiteUrl configured — cannot trigger scrape");
        }
        log.info("Triggering scrape for clientId={} url={}", clientId, websiteUrl);
        return webClient.post()
                .uri("/api/scrape")
                .bodyValue(Map.of("url", websiteUrl, "client_id", clientId, "max_pages", 1000))
                .retrieve()
                .bodyToMono(ScrapeJobResponse.class)
                .block();
    }
}

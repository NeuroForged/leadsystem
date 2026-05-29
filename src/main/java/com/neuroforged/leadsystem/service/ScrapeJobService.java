package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;

import java.util.List;

public interface ScrapeJobService {
    ScrapeJobDto createJob(Long clientId, String url, Integer maxPages, String initiatedBy);
    ScrapeJobDto getJob(Long id);
    List<ScrapeJobDto> listByClient(Long clientId);
    ScrapeJobDto syncStatus(Long id);

    /**
     * Resolves the internal scrape job to its scraper-side job id and proxies the
     * KB zip download through {@link ScraperService}, so the scraper API key never
     * leaves the backend. Caller must enforce tenant access via the returned
     * {@code clientId} before streaming the bytes.
     */
    ScrapeJobZip downloadZip(Long id);

    record ScrapeJobZip(Long clientId, byte[] data) {}
}

package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ScrapeJobResponse;
import com.neuroforged.leadsystem.dto.ScraperStatusResponse;

public interface ScraperService {
    ScrapeJobResponse triggerScrape(String websiteUrl, String clientId, int maxPages);
    ScraperStatusResponse getJobStatus(String scraperJobId);
    byte[] downloadZip(String scraperJobId);
}

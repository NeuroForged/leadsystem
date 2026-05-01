package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ScrapeJobResponse;

public interface ScraperService {
    ScrapeJobResponse triggerScrape(String websiteUrl, String clientId);
}

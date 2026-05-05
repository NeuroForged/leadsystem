package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;

import java.util.List;

public interface ScrapeJobService {
    ScrapeJobDto createJob(Long clientId, String url, Integer maxPages, String initiatedBy);
    ScrapeJobDto getJob(Long id);
    List<ScrapeJobDto> listByClient(Long clientId);
    ScrapeJobDto syncStatus(Long id);
}

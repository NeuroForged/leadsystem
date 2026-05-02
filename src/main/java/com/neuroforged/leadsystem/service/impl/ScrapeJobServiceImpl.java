package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;
import com.neuroforged.leadsystem.dto.ScraperStatusResponse;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.ScrapeJob;
import com.neuroforged.leadsystem.entity.ScrapeJobStatus;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.ScrapeJobRepository;
import com.neuroforged.leadsystem.service.ScrapeJobService;
import com.neuroforged.leadsystem.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapeJobServiceImpl implements ScrapeJobService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final ClientRepository clientRepository;
    private final ScraperService scraperService;

    @Override
    @Transactional
    public ScrapeJobDto createJob(Long clientId, String url, Integer maxPages, String initiatedBy) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        int pages = maxPages != null ? maxPages : 1000;

        var scraperResponse = scraperService.triggerScrape(url, String.valueOf(clientId), pages);

        ScrapeJob job = ScrapeJob.builder()
                .client(client)
                .scraperJobId(scraperResponse.getJobId())
                .status(ScrapeJobStatus.PENDING)
                .url(url)
                .maxPages(pages)
                .initiatedBy(initiatedBy)
                .reused(scraperResponse.isReused())
                .build();

        return toDto(scrapeJobRepository.save(job));
    }

    @Override
    @Transactional
    public ScrapeJobDto getJob(Long id) {
        ScrapeJob job = scrapeJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScrapeJob not found: " + id));

        if (job.getStatus() == ScrapeJobStatus.PENDING || job.getStatus() == ScrapeJobStatus.RUNNING) {
            return syncStatus(id);
        }
        return toDto(job);
    }

    @Override
    public List<ScrapeJobDto> listByClient(Long clientId) {
        return scrapeJobRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScrapeJobDto syncStatus(Long id) {
        ScrapeJob job = scrapeJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScrapeJob not found: " + id));

        if (job.getScraperJobId() == null) {
            return toDto(job);
        }

        try {
            ScraperStatusResponse remote = scraperService.getJobStatus(job.getScraperJobId());
            applyRemoteStatus(job, remote);
            scrapeJobRepository.save(job);
        } catch (Exception e) {
            log.warn("Failed to sync scrape job {} from scraper: {}", id, e.getMessage());
        }
        return toDto(job);
    }

    private void applyRemoteStatus(ScrapeJob job, ScraperStatusResponse remote) {
        job.setStatus(mapStatus(remote.getStatus()));
        job.setErrorMessage(remote.getError());

        if (remote.getResult() != null) {
            job.setScrapedCount(remote.getResult().getScraped());
            job.setErrorCount(remote.getResult().getErrors());
            if (remote.getResult().getFiles() != null) {
                job.setFiles(String.join(",", remote.getResult().getFiles().keySet()));
            }
        }

        if (job.getStatus() == ScrapeJobStatus.DONE || job.getStatus() == ScrapeJobStatus.ERROR) {
            if (job.getFinishedAt() == null) {
                job.setFinishedAt(LocalDateTime.now());
            }
        }
    }

    private ScrapeJobStatus mapStatus(String scraperStatus) {
        if (scraperStatus == null) return ScrapeJobStatus.PENDING;
        return switch (scraperStatus.toLowerCase()) {
            case "running" -> ScrapeJobStatus.RUNNING;
            case "done"    -> ScrapeJobStatus.DONE;
            case "error"   -> ScrapeJobStatus.ERROR;
            default        -> ScrapeJobStatus.PENDING;
        };
    }

    private ScrapeJobDto toDto(ScrapeJob job) {
        return ScrapeJobDto.builder()
                .id(job.getId())
                .clientId(job.getClient() != null ? job.getClient().getId() : null)
                .scraperJobId(job.getScraperJobId())
                .status(job.getStatus())
                .url(job.getUrl())
                .maxPages(job.getMaxPages())
                .initiatedBy(job.getInitiatedBy())
                .createdAt(job.getCreatedAt())
                .finishedAt(job.getFinishedAt())
                .scrapedCount(job.getScrapedCount())
                .errorCount(job.getErrorCount())
                .files(job.getFiles())
                .errorMessage(job.getErrorMessage())
                .reused(job.isReused())
                .build();
    }
}

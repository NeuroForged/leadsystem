package com.neuroforged.leadsystem.dto;

import com.neuroforged.leadsystem.entity.ScrapeJobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScrapeJobDto {
    private Long id;
    private Long clientId;
    private String scraperJobId;
    private ScrapeJobStatus status;
    private String url;
    private Integer maxPages;
    private String initiatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private Integer scrapedCount;
    private Integer errorCount;
    private String files;
    private String errorMessage;
    private boolean reused;
}

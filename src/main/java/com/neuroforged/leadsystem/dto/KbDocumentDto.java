package com.neuroforged.leadsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KbDocumentDto {
    private Long id;
    private Long clientId;
    private Long scrapeJobId;
    private String filename;
    private String content;
    private Integer wordCount;
    private LocalDateTime fetchedAt;
}

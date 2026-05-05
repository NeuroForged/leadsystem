package com.neuroforged.leadsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LeadCommentDto {
    private Long id;
    private Long leadId;
    private String authorEmail;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;
}

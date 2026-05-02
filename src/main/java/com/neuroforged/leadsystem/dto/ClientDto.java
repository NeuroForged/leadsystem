package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientDto {
    private Long id;

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String primaryEmail;

    private List<String> notificationEmails;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    private String websiteUrl;

    private String apiKey;

    private boolean calendlyConnected;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastScrapedAt;
}

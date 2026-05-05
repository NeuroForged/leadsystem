package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScrapePresetDto {
    private Long id;

    @NotNull
    private Long clientId;

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    private Integer maxPages;
    private LocalDateTime createdAt;
}

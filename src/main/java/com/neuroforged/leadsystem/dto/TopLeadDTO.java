package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopLeadDTO {
    private Long id;
    private String businessName;
    private String email;
    private int leadScore;
    private String status;
    private String clientId;
    private LocalDateTime createdAt;
}

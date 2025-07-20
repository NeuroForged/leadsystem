package com.neuroforged.leadsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LeadResponseDTO {

    private Long id;
    private String firstName;
    private String email;
    private String businessName;
    private String businessType;
    private String customerType;
    private String trafficSource;
    private Integer monthlyLeads;
    private Double conversionRate;
    private Double costPerLead;
    private Double clientValue;
    private Integer leadScore;
    private String leadChallenge;
    private String clientId;
    private LocalDateTime createdAt;

}

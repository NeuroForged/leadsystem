package com.neuroforged.leadsystem.dto;

import java.util.Map;
import com.neuroforged.leadsystem.entity.LeadStatus;
import lombok.Data;

import java.time.LocalDateTime;

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
    private LeadStatus status;
    private Map<String, String> capturedFields;
    private String relevantKbSnippet;
    private String assignedTo;
    private LocalDateTime createdAt;

}

package com.neuroforged.leadsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeadRoutingRuleDto {
    private Long id;
    private Long clientId;
    private String matchField;
    private String matchValue;
    private String assignTo;
    private Integer priority;
    private LocalDateTime createdAt;
}

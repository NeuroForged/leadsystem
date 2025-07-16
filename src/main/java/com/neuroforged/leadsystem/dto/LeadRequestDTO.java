package com.neuroforged.leadsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeadRequestDTO {

    private String firstName;
    private String email;
    private String phoneNumber;
    private String businessName;
    private String businessType;
    private String customerType;
    private String trafficSource;
    private Integer monthlyLeads;
    private Double conversionRate;
    private BigDecimal costPerLead;
    private BigDecimal clientValue;
    private Integer leadScore;
    private String leadChallenge;
    private String clientId;

}

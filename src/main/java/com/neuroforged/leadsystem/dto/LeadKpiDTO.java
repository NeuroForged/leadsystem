package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadKpiDTO {
    private long totalLeads;
    private long totalMeetings;
    private double conversionRate;
    private double avgLeadScore;
    private long highQualityLeads;
}

package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryDTO {
    private Long clientId;
    private String clientName;
    private long totalLeads;
    private long totalMeetings;
    private double conversionRate;
    private double avgLeadScore;
}

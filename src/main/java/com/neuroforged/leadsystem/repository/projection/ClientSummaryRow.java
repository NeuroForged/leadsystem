package com.neuroforged.leadsystem.repository.projection;

public interface ClientSummaryRow {
    Long getClientId();
    String getClientName();
    Long getTotalLeads();
    Long getTotalMeetings();
    Double getAvgLeadScore();
}

package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.*;

import java.util.List;

public interface AnalyticsService {
    List<ClientSummaryDTO> getClientSummary();
    LeadKpiDTO getLeadKpis(Long clientId, String from, String to);
    List<LeadVolumeDTO> getLeadVolume(Long clientId, String from, String to);
    List<GroupCountDTO> getLeadsByTrafficSource(Long clientId);
    List<GroupCountDTO> getLeadsByScoreBand(Long clientId);
    List<GroupCountDTO> getLeadsByBusinessType(Long clientId);
    List<GroupCountDTO> getLeadsByPipelineStatus(Long clientId);
    List<TopLeadDTO> getTopLeads(Long clientId, int limit);
}

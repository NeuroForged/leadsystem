package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;

import java.util.List;

public interface LeadService {
    LeadResponseDTO createLead(LeadRequestDTO dto);
    List<LeadResponseDTO> getAllLeads();
    List<LeadResponseDTO> getLeadsByClientId(String clientId);
    LeadResponseDTO getLeadById(Long id);
}

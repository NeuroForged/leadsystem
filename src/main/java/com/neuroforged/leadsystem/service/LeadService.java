package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.LeadStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeadService {
    LeadResponseDTO createLead(LeadRequestDTO dto);
    PagedResponse<LeadResponseDTO> getLeads(Long clientId, LeadStatus status, String search, Pageable pageable);
    LeadResponseDTO updateLeadStatus(Long id, LeadStatus status);
    List<LeadResponseDTO> getLeadsByClientId(Long clientId);
    LeadResponseDTO getLeadById(Long id);
}

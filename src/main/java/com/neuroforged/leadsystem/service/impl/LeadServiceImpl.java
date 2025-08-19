package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.EmailSendException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final EmailService emailService;

    @Override
    public LeadResponseDTO createLead(LeadRequestDTO dto) {

        validateLeadRequest(dto);

        if (leadRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Lead with email already exists: " + dto.getEmail());
        }

        Lead lead = buildLeadEntity(dto);
        Lead savedLead = leadRepository.save(lead);

        sendNotificationEmails(savedLead);

        return mapToDTO(savedLead);
    }

    @Override
    public List<LeadResponseDTO> getAllLeads() {
        return leadRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<LeadResponseDTO> getLeadsByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidLeadException("Client ID must not be null or blank.");
        }

        return leadRepository.findByClientId(clientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public LeadResponseDTO getLeadById(Long id) {
        return leadRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new InvalidLeadException("Lead not found with ID: " + id));
    }

    private void validateLeadRequest(LeadRequestDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
            throw new InvalidLeadException("A valid email address is required.");
        }

        if (dto.getClientId() == null || dto.getClientId().isBlank()) {
            throw new InvalidLeadException("Client ID must be provided.");
        }
    }

    private Lead buildLeadEntity(LeadRequestDTO dto) {
        return Lead.builder()
                .email(dto.getEmail())
                .businessName(dto.getBusinessName())
                .businessType(dto.getBusinessType())
                .customerType(dto.getCustomerType())
                .trafficSource(dto.getTrafficSource())
                .monthlyLeads(dto.getMonthlyLeads())
                .conversionRate(dto.getConversionRate())
                .costPerLead(dto.getCostPerLead())
                .clientValue(dto.getClientValue())
                .leadScore(dto.getLeadScore())
                .leadChallenge(dto.getLeadChallenge())
                .clientId(dto.getClientId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void sendNotificationEmails(Lead lead) {
        String subject = "New Lead Received - " + lead.getEmail() + " - " + lead.getLeadScore() + "/100";
        String body = STR."""
            Email: \{lead.getEmail()}
            Customer Type: \{lead.getCustomerType()}
            Business Name: \{lead.getBusinessName()}
            Business Type: \{lead.getBusinessType()}
            Monthly Leads: \{lead.getMonthlyLeads()}
            Traffic Source: \{lead.getTrafficSource()}
            Conversion Rate: \{lead.getConversionRate()}
            Cost Per Lead: \{lead.getCostPerLead()}
            Client Value: \{lead.getClientValue()}
            Lead Challenge: \{lead.getLeadChallenge()}
            Client ID: \{lead.getClientId()}
            Created At: \{lead.getCreatedAt()}
            """;

        String[] team = {
                "joshua.white@neuroforged.com",
                "matthew.mcfarlane@neuroforged.com"
        };

        try {
            emailService.sendLeadToMultiple(team, subject, body);
        } catch (EmailSendException e) {
            log.warn("Failed to send notification email: {}", e.getMessage(), e);
        }
    }

    private LeadResponseDTO mapToDTO(Lead lead) {
        LeadResponseDTO dto = new LeadResponseDTO();
        dto.setId(lead.getId());
        dto.setEmail(lead.getEmail());
        dto.setBusinessName(lead.getBusinessName());
        dto.setBusinessType(lead.getBusinessType());
        dto.setTrafficSource(lead.getTrafficSource());
        dto.setMonthlyLeads(lead.getMonthlyLeads());
        dto.setConversionRate(lead.getConversionRate());
        dto.setCostPerLead(lead.getCostPerLead());
        dto.setClientValue(lead.getClientValue());
        dto.setLeadScore(lead.getLeadScore());
        dto.setLeadChallenge(lead.getLeadChallenge());
        dto.setClientId(lead.getClientId());
        dto.setCreatedAt(lead.getCreatedAt());
        return dto;
    }
}

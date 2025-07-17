package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService{

    @Autowired
    private EmailService emailService;

    private final LeadRepository leadRepository;

    @Override
    public LeadResponseDTO createLead(LeadRequestDTO dto) {
        Lead lead = Lead.builder()
                .firstName(dto.getFirstName())
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

        Lead saved = leadRepository.save(lead);
        String subject = "New Lead Received";
        String body = "Name: " + lead.getFirstName() +
                "\nEmail: " + lead.getEmail() +
                "\nCustomer Type: " + lead.getCustomerType() +
                "\nBusiness Name" + lead.getBusinessName() +
                "\mBusiness Type: " + lead.getLeadChallenge() +
                "\nMonthly Leads: " + lead.getMonthlyLeads() +
                "\nTraffic Sourece: " + lead.getTrafficSource() +
                "\nMonthly Leads: " + lead.getMonthlyLeads() +
                "\nConversion Rate: " + lead.getConversionRate() +
                "\nCost Per Lead" + lead.getCostPerLead() +
                "\mClient Value: " + lead.getClientValue() +
                "\nLead Challenge: " + lead.getLeadChallenge() +
                "\nTraffic Sourece: " + lead.getTrafficSource() +
                "\nClientId: " + lead.getClientId() +
                "\nCreated at:" + lead.getCreatedAt();
        try {
            emailService.sendLeadNotification("joshua.white@neuroforged.com", subject, body);
        } catch (MessagingException e) {
            e.printStackTrace(); // You might log this properly
        }
        return mapToDTO(saved);
    }

    @Override
    public List<LeadResponseDTO> getAllLeads() {
        return leadRepository.findAll().stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<LeadResponseDTO> getLeadsByClientId(String clientId) {
        return leadRepository.findByClientId(clientId).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public LeadResponseDTO getLeadById(Long id) {
        return leadRepository.findById(id)
                .map(this::mapToDTO).orElse(null);

    }

    public LeadResponseDTO mapToDTO(Lead lead) {
        LeadResponseDTO dto = new LeadResponseDTO();
        dto.setId(lead.getId());
        dto.setFirstName(lead.getFirstName());
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

package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.mapper.LeadMapper;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.repository.spec.LeadFilterSpec;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import com.neuroforged.leadsystem.service.LeadNotificationService;
import com.neuroforged.leadsystem.service.LeadService;
import com.neuroforged.leadsystem.service.NotificationService;
import com.neuroforged.leadsystem.service.OutboundWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private static final int HIGH_SCORE_THRESHOLD = 80;

    private final LeadRepository leadRepository;
    private final LeadNotificationService leadNotificationService;
    private final NotificationService notificationService;
    private final LeadMapper leadMapper;
    private final ClientRepository clientRepository;
    private final OutboundWebhookService outboundWebhookService;
    private final LeadSystemMetrics metrics;

    @Override
    public LeadResponseDTO createLead(LeadRequestDTO dto) {

        validateLeadRequest(dto);

        if (leadRepository.existsByEmailAndClientId(dto.getEmail(), dto.getClientId())) {
            throw new DuplicateResourceException(
                    "Lead with email " + dto.getEmail() + " already exists for clientId " + dto.getClientId());
        }

        Lead lead = buildLeadEntity(dto);
        Lead savedLead = leadRepository.save(lead);

        metrics.recordLeadReceived(savedLead.getClientId());
        leadNotificationService.notifyNewLead(savedLead);

        try {
            Long clientLongId = Long.parseLong(savedLead.getClientId());
            Optional<Client> clientOpt = clientRepository.findById(clientLongId);
            clientOpt.ifPresent(client -> outboundWebhookService.notifyWebhook(savedLead, client));

            Map<String, String> ctx = buildLeadContext(savedLead);
            notificationService.notify(clientLongId, NotificationEventType.NEW_LEAD, ctx);
            if (savedLead.getLeadScore() != null && savedLead.getLeadScore() >= HIGH_SCORE_THRESHOLD) {
                notificationService.notify(clientLongId, NotificationEventType.LEAD_SCORED_HIGH, ctx);
            }
        } catch (Exception e) {
            log.warn("Outbound webhook/notification skipped for lead {} - client lookup failed: {}", savedLead.getId(), e.getMessage());
        }

        return leadMapper.toDto(savedLead);
    }

    @Override
    public PagedResponse<LeadResponseDTO> getLeads(String clientId, LeadStatus status, Pageable pageable) {
        var spec = LeadFilterSpec.withClientId(clientId)
                .and(LeadFilterSpec.withStatus(status));
        return PagedResponse.from(leadRepository.findAll(spec, pageable).map(leadMapper::toDto));
    }

    @Override
    public LeadResponseDTO updateLeadStatus(Long id, LeadStatus status) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
        lead.setStatus(status);
        return leadMapper.toDto(leadRepository.save(lead));
    }

    public List<LeadResponseDTO> getLeadsByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidLeadException("Client ID must not be null or blank.");
        }

        return leadRepository.findByClientId(clientId).stream()
                .map(leadMapper::toDto)
                .collect(Collectors.toList());
    }

    public LeadResponseDTO getLeadById(Long id) {
        return leadRepository.findById(id)
                .map(leadMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
    }

    private void validateLeadRequest(LeadRequestDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
            throw new InvalidLeadException("A valid email address is required.");
        }

        if (dto.getClientId() == null || dto.getClientId().isBlank()) {
            throw new InvalidLeadException("Client ID must be provided.");
        }
    }

    private Map<String, String> buildLeadContext(Lead lead) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("Email", lead.getEmail());
        ctx.put("Name", lead.getFirstName() != null ? lead.getFirstName() : "");
        ctx.put("Business", lead.getBusinessName() != null ? lead.getBusinessName() : "");
        ctx.put("Score", lead.getLeadScore() != null ? String.valueOf(lead.getLeadScore()) : "N/A");
        ctx.put("Client ID", lead.getClientId());
        return ctx;
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
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

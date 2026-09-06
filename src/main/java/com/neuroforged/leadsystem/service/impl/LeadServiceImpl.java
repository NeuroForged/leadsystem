package com.neuroforged.leadsystem.service.impl;

import org.springframework.data.domain.PageRequest;
import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.mapper.LeadMapper;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.repository.spec.LeadFilterSpec;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import com.neuroforged.leadsystem.service.LeadEnrichmentService;
import com.neuroforged.leadsystem.service.LeadNotificationService;
import com.neuroforged.leadsystem.service.LeadRoutingService;
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
    private final LeadEnrichmentService leadEnrichmentService;
    private final LeadRoutingService leadRoutingService;
    private final LeadMapper leadMapper;
    private final ClientRepository clientRepository;
    private final OutboundWebhookService outboundWebhookService;
    private final LeadSystemMetrics metrics;
    private final BusinessEventLogger eventLogger;

    @Override
    public LeadResponseDTO createLead(LeadRequestDTO dto) {

        try {
            validateLeadRequest(dto);
        } catch (InvalidLeadException e) {
            eventLogger.leadInvalid(null, dto.getClientId(), e.getMessage(), dto.getEmail());
            throw e;
        }

        Client resolvedClient = resolveClient(dto.getClientId());
        if (resolvedClient == null) {
            // Reject rather than silently persisting an orphan lead (client = null) that
            // also bypasses the per-client duplicate guard / partial unique index.
            InvalidLeadException ex = new InvalidLeadException(
                    "clientId '" + dto.getClientId() + "' does not resolve to a known client.");
            eventLogger.leadInvalid(null, dto.getClientId(), ex.getMessage(), dto.getEmail());
            throw ex;
        }
        String clientName = resolvedClient.getName();

        if (leadRepository.existsByEmailAndClient_Id(dto.getEmail(), resolvedClient.getId())) {
            eventLogger.leadDuplicate(clientName, dto.getClientId(), dto.getEmail());
            metrics.recordLeadDuplicate(dto.getClientId());
            throw new DuplicateResourceException(
                    "Lead with email " + dto.getEmail() + " already exists for clientId " + dto.getClientId());
        }

        Lead lead = buildLeadEntity(dto, resolvedClient);
        leadEnrichmentService.enrich(lead);
        leadRoutingService.route(lead);
        Lead savedLead = leadRepository.save(lead);

        metrics.recordLeadReceived(savedLead.getClientIdStr());

        eventLogger.leadReceived(clientName, savedLead.getClientIdStr(), savedLead.getEmail(),
                savedLead.getLeadScore(),
                savedLead.getRelevantKbSnippet() != null,
                savedLead.getAssignedTo());

        leadNotificationService.notifyNewLead(savedLead);

        // Post-save side effects are best-effort: the lead is already persisted, so a delivery
        // failure must not fail the request. Each channel is isolated in its own try/catch so a
        // bug in one (e.g. NPE) is distinguishable from a transient delivery failure in another
        // and doesn't suppress the rest.
        Client finalClient = resolvedClient != null ? resolvedClient : savedLead.getClient();
        if (finalClient != null) {
            try {
                outboundWebhookService.notifyWebhook(savedLead, finalClient);
            } catch (Exception e) {
                log.warn("Outbound webhook dispatch failed for lead {}: {}", savedLead.getId(), e.getMessage());
            }

            Map<String, String> ctx = buildLeadContext(savedLead);
            try {
                notificationService.notify(finalClient.getId(), NotificationEventType.NEW_LEAD, ctx);
            } catch (Exception e) {
                log.warn("NEW_LEAD notification failed for lead {}: {}", savedLead.getId(), e.getMessage());
            }

            if (savedLead.getLeadScore() != null && savedLead.getLeadScore() >= HIGH_SCORE_THRESHOLD) {
                try {
                    notificationService.notify(finalClient.getId(), NotificationEventType.LEAD_SCORED_HIGH, ctx);
                } catch (Exception e) {
                    log.warn("LEAD_SCORED_HIGH notification failed for lead {}: {}", savedLead.getId(), e.getMessage());
                }
            }
        }

        return leadMapper.toDto(savedLead);
    }

    /** Best-effort Client entity lookup — returns null if id is unparseable or not found. */
    private Client resolveClient(String clientId) {
        if (clientId == null || clientId.isBlank()) return null;
        try {
            return clientRepository.findById(Long.parseLong(clientId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public PagedResponse<LeadResponseDTO> getLeads(Long clientId, LeadStatus status, String search, String from, String to, Pageable pageable) {
        var spec = LeadFilterSpec.withClientId(clientId)
                .and(LeadFilterSpec.withStatus(status))
                .and(LeadFilterSpec.withSearch(search))
                .and(LeadFilterSpec.withDateRange(from, to));
        return PagedResponse.from(leadRepository.findAll(spec, pageable).map(leadMapper::toDto));
    }

    @Override
    public LeadResponseDTO updateLeadStatus(Long id, LeadStatus status) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
        lead.setStatus(status);
        return leadMapper.toDto(leadRepository.save(lead));
    }

    @Override
    public List<LeadResponseDTO> getLeadsByClientId(Long clientId) {
        if (clientId == null) {
            throw new InvalidLeadException("Client ID must not be null.");
        }

        // Bounded: this used to materialise every lead a client ever had.
        return leadRepository.findByClient_Id(clientId, PageRequest.of(0, 500)).stream()
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
        ctx.put("Client ID", lead.getClientIdStr());
        return ctx;
    }

    private Lead buildLeadEntity(LeadRequestDTO dto, Client client) {
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
                .client(client)
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

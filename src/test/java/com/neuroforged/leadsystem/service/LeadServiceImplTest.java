package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.mapper.LeadMapper;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.repository.spec.LeadFilterSpec;
import com.neuroforged.leadsystem.service.LeadEnrichmentService;
import com.neuroforged.leadsystem.service.LeadRoutingService;
import com.neuroforged.leadsystem.service.NotificationService;
import com.neuroforged.leadsystem.service.OutboundWebhookService;
import com.neuroforged.leadsystem.service.impl.LeadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadNotificationService leadNotificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LeadEnrichmentService leadEnrichmentService;

    @Mock
    private LeadRoutingService leadRoutingService;

    @Mock
    private LeadMapper leadMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private OutboundWebhookService outboundWebhookService;

    @Mock
    private LeadSystemMetrics metrics;

    @Mock
    private BusinessEventLogger eventLogger;

    @InjectMocks
    private LeadServiceImpl leadService;

    private LeadRequestDTO validRequest() {
        LeadRequestDTO dto = new LeadRequestDTO();
        dto.setEmail("test@example.com");
        dto.setClientId("1");
        dto.setBusinessName("Acme");
        dto.setBusinessType("SaaS");
        dto.setCustomerType("B2B");
        dto.setTrafficSource("Google");
        dto.setMonthlyLeads(100);
        dto.setConversionRate(2.5);
        dto.setCostPerLead(50.0);
        dto.setClientValue(5000.0);
        dto.setLeadScore(80);
        dto.setLeadChallenge("Scaling");
        return dto;
    }

    @Test
    void createLead_happyPath_returnsDto() {
        LeadRequestDTO dto = validRequest();
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientIdStr(dto.getClientId()).build();
        LeadResponseDTO responseDto = new LeadResponseDTO();
        responseDto.setEmail(dto.getEmail());

        when(leadRepository.existsByEmailAndClientIdStr(dto.getEmail(), dto.getClientId())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);
        when(leadMapper.toDto(saved)).thenReturn(responseDto);

        LeadResponseDTO result = leadService.createLead(dto);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(leadRepository).save(any(Lead.class));
        verify(leadNotificationService).notifyNewLead(saved);
    }

    @Test
    void createLead_duplicateEmail_throwsDuplicateResourceException() {
        LeadRequestDTO dto = validRequest();
        when(leadRepository.existsByEmailAndClientIdStr(dto.getEmail(), dto.getClientId())).thenReturn(true);

        assertThatThrownBy(() -> leadService.createLead(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(dto.getEmail());

        verify(leadRepository, never()).save(any());
    }

    @Test
    void createLead_invalidEmail_throwsInvalidLeadException() {
        LeadRequestDTO dto = validRequest();
        dto.setEmail("not-an-email");

        assertThatThrownBy(() -> leadService.createLead(dto))
                .isInstanceOf(InvalidLeadException.class);
    }

    @Test
    void createLead_nullEmail_throwsInvalidLeadException() {
        LeadRequestDTO dto = validRequest();
        dto.setEmail(null);

        assertThatThrownBy(() -> leadService.createLead(dto))
                .isInstanceOf(InvalidLeadException.class);
    }

    @Test
    void createLead_blankClientId_throwsInvalidLeadException() {
        LeadRequestDTO dto = validRequest();
        dto.setClientId("  ");

        assertThatThrownBy(() -> leadService.createLead(dto))
                .isInstanceOf(InvalidLeadException.class);
    }

    @Test
    void createLead_nullClientId_throwsInvalidLeadException() {
        LeadRequestDTO dto = validRequest();
        dto.setClientId(null);

        assertThatThrownBy(() -> leadService.createLead(dto))
                .isInstanceOf(InvalidLeadException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLeads_noFilters_returnsPagedResponse() {
        Lead lead = Lead.builder().id(1L).email("test@example.com").clientIdStr("1").build();
        LeadResponseDTO dto = new LeadResponseDTO();
        dto.setEmail("test@example.com");

        Page<Lead> page = new PageImpl<>(List.of(lead));
        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(leadMapper.toDto(lead)).thenReturn(dto);

        PagedResponse<LeadResponseDTO> result = leadService.getLeads(null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void updateLeadStatus_validId_updatesAndReturnsDto() {
        Lead lead = Lead.builder().id(1L).email("test@example.com").clientIdStr("1").status(LeadStatus.NEW).build();
        LeadResponseDTO dto = new LeadResponseDTO();
        dto.setEmail("test@example.com");

        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toDto(lead)).thenReturn(dto);

        LeadResponseDTO result = leadService.updateLeadStatus(1L, LeadStatus.CONTACTED);

        assertThat(result).isNotNull();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONTACTED);
        verify(leadRepository).save(lead);
    }

    @Test
    void updateLeadStatus_leadNotFound_throwsResourceNotFoundException() {
        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.updateLeadStatus(99L, LeadStatus.CONTACTED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}

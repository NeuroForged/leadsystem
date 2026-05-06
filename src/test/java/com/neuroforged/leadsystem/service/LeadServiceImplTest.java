package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.mapper.LeadMapper;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
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
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientId(dto.getClientId()).build();
        LeadResponseDTO responseDto = new LeadResponseDTO();
        responseDto.setEmail(dto.getEmail());

        when(leadRepository.existsByEmailAndClientId(dto.getEmail(), dto.getClientId())).thenReturn(false);
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
        when(leadRepository.existsByEmailAndClientId(dto.getEmail(), dto.getClientId())).thenReturn(true);

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
}

package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.impl.LeadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private LeadServiceImpl leadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(leadService, "fallbackEmail", "admin@test.com");
    }

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

        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        LeadResponseDTO result = leadService.createLead(dto);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void createLead_duplicateEmail_throwsDuplicateResourceException() {
        LeadRequestDTO dto = validRequest();
        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(true);

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
    void sendNotification_clientHasRecipients_sendsToClientEmails() {
        LeadRequestDTO dto = validRequest();
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientId("1").build();
        Client client = new Client();
        client.setId(1L);
        client.setNotificationEmails("owner@client.com, manager@client.com");

        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        leadService.createLead(dto);

        ArgumentCaptor<String[]> recipientsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(recipientsCaptor.capture(), any(), any());
        assertThat(recipientsCaptor.getValue()).containsExactly("owner@client.com", "manager@client.com");
    }

    @Test
    void sendNotification_clientHasNoRecipients_fallsBackToAdminEmail() {
        LeadRequestDTO dto = validRequest();
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientId("1").build();
        Client client = new Client();
        client.setId(1L);
        client.setNotificationEmails(null);

        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        leadService.createLead(dto);

        ArgumentCaptor<String[]> recipientsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(recipientsCaptor.capture(), any(), any());
        assertThat(recipientsCaptor.getValue()).containsExactly("admin@test.com");
    }

    @Test
    void sendNotification_clientNotFound_fallsBackToAdminEmail() {
        LeadRequestDTO dto = validRequest();
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientId("1").build();

        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        leadService.createLead(dto);

        ArgumentCaptor<String[]> recipientsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(recipientsCaptor.capture(), any(), any());
        assertThat(recipientsCaptor.getValue()).containsExactly("admin@test.com");
    }

    @Test
    void sendNotification_nonNumericClientId_fallsBackToAdminEmail() {
        LeadRequestDTO dto = validRequest();
        dto.setClientId("chatbot-abc");
        Lead saved = Lead.builder().id(1L).email(dto.getEmail()).clientId("chatbot-abc").build();

        when(leadRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenReturn(saved);

        leadService.createLead(dto);

        ArgumentCaptor<String[]> recipientsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(recipientsCaptor.capture(), any(), any());
        assertThat(recipientsCaptor.getValue()).containsExactly("admin@test.com");
    }
}

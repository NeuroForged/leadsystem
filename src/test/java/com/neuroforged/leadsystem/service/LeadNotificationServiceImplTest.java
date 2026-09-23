package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.impl.LeadNotificationServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadNotificationServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LeadNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "fallbackEmail", "admin@test.com");
    }

    private Lead leadForClient(String clientId) {
        // LSB-155: getClientIdStr() is now derived from Client FK.
        // Attach a Client when clientId is numeric so the service can resolve it;
        // for non-numeric IDs leave client null (mirrors legacy chatbot-abc rows).
        Client client = null;
        try {
            client = new Client();
            client.setId(Long.parseLong(clientId));
        } catch (NumberFormatException e) {
            client = null;
        }
        return Lead.builder()
                .id(1L)
                .email("lead@example.com")
                .client(client)
                .businessName("Acme")
                .build();
    }

    @Test
    void sendNotification_clientHasRecipients_sendsToClientEmails() {
        Client client = new Client();
        client.setId(1L);
        client.setNotificationEmails("owner@client.com, manager@client.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        notificationService.notifyNewLead(leadForClient("1"));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(captor.capture(), any(), any());
        assertThat(captor.getValue()).containsExactly("owner@client.com", "manager@client.com");
    }

    @Test
    void sendNotification_clientHasNoRecipients_fallsBackToAdminEmail() {
        Client client = new Client();
        client.setId(1L);
        client.setNotificationEmails(null);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        notificationService.notifyNewLead(leadForClient("1"));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(captor.capture(), any(), any());
        assertThat(captor.getValue()).containsExactly("admin@test.com");
    }

    @Test
    void sendNotification_clientNotFound_fallsBackToAdminEmail() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        notificationService.notifyNewLead(leadForClient("1"));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(captor.capture(), any(), any());
        assertThat(captor.getValue()).containsExactly("admin@test.com");
    }

    @Test
    void sendNotification_nonNumericClientId_fallsBackToAdminEmail() {
        notificationService.notifyNewLead(leadForClient("chatbot-abc"));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(emailService).sendLeadToMultiple(captor.capture(), any(), any());
        assertThat(captor.getValue()).containsExactly("admin@test.com");
    }

    @Test
    void notification_forPatientLead_hasNoNullsAndListsCapturedFields() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        com.neuroforged.leadsystem.entity.Lead lead = leadForClient("1");
        lead.setLeadScore(null);
        lead.setMonthlyLeads(null);
        lead.setCostPerLead(null);
        lead.setClientValue(null);
        lead.setConversionRate(null);
        lead.setCapturedFields(new java.util.LinkedHashMap<>(java.util.Map.of("treatment_interest", "Invisalign")));

        notificationService.notifyNewLead(lead);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendLeadToMultiple(any(), subject.capture(), body.capture());
        assertThat(subject.getValue()).doesNotContain("null").contains("unscored");
        assertThat(body.getValue()).doesNotContain("null").doesNotContain("Cost Per Lead")
                .contains("treatment interest: Invisalign");
    }
}

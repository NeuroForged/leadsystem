package com.neuroforged.leadsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.CalendlyWebhookLog;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.MeetingStatus;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.CalendlyWebhookLogRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.impl.CalendlyWebhookServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendlyWebhookServiceImplTest {

    @Mock private CalendlyWebhookLogRepository webhookLogRepository;
    @Mock private CalendlyMeetingRepository calendlyMeetingRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmailService emailService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private CalendlyWebhookServiceImpl service;

    private CalendlyWebhookPayload buildPayload(String eventType, String inviteeEmail,
                                                 String start, String end,
                                                 String eventUri, String oldEventUri) {
        CalendlyWebhookPayload.Payload.Invitee invitee = new CalendlyWebhookPayload.Payload.Invitee();
        invitee.setEmail(inviteeEmail);

        CalendlyWebhookPayload.Payload.ScheduledEvent scheduled = new CalendlyWebhookPayload.Payload.ScheduledEvent();
        scheduled.setStartTime(start);
        scheduled.setEndTime(end);

        CalendlyWebhookPayload.Payload.EventType eventTypeObj = new CalendlyWebhookPayload.Payload.EventType();
        eventTypeObj.setName("30 Min Meeting");

        CalendlyWebhookPayload.Payload inner = new CalendlyWebhookPayload.Payload();
        inner.setInvitee(invitee);
        inner.setScheduledEvent(scheduled);
        inner.setEventType(eventTypeObj);
        inner.setEvent(eventUri);
        inner.setOldEvent(oldEventUri);

        CalendlyWebhookPayload payload = new CalendlyWebhookPayload();
        payload.setEvent(eventType);
        payload.setPayload(inner);
        return payload;
    }

    @Test
    void inviteeCreated_savesMeetingWithScheduledStatus() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "lead@example.com",
                "2026-05-01T10:00:00Z", "2026-05-01T10:30:00Z",
                "https://calendly.com/events/abc123", null);

        when(clientRepository.findByPrimaryEmail("lead@example.com")).thenReturn(Optional.empty());

        service.handleWebhook(payload, Map.of());

        ArgumentCaptor<CalendlyMeeting> captor = ArgumentCaptor.forClass(CalendlyMeeting.class);
        verify(calendlyMeetingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingStatus.SCHEDULED);
        assertThat(captor.getValue().getInviteeEmail()).isEqualTo("lead@example.com");
        assertThat(captor.getValue().getCalendlyUri()).isEqualTo("https://calendly.com/events/abc123");
    }

    @Test
    void inviteeCreated_matchingClient_linkedToMeeting() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "client@example.com",
                "2026-05-01T10:00:00Z", "2026-05-01T10:30:00Z",
                "https://calendly.com/events/xyz", null);

        Client client = new Client();
        client.setId(1L);
        client.setPrimaryEmail("client@example.com");
        when(clientRepository.findByPrimaryEmail("client@example.com")).thenReturn(Optional.of(client));

        service.handleWebhook(payload, Map.of());

        ArgumentCaptor<CalendlyMeeting> captor = ArgumentCaptor.forClass(CalendlyMeeting.class);
        verify(calendlyMeetingRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isEqualTo(client);
    }

    @Test
    void inviteeCreated_noMatchingClient_meetingSavedWithNullClient() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "unknown@example.com",
                "2026-05-01T10:00:00Z", "2026-05-01T10:30:00Z",
                "https://calendly.com/events/xyz", null);

        when(clientRepository.findByPrimaryEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.handleWebhook(payload, Map.of());

        ArgumentCaptor<CalendlyMeeting> captor = ArgumentCaptor.forClass(CalendlyMeeting.class);
        verify(calendlyMeetingRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isNull();
    }

    @Test
    void inviteeCanceled_existingMeeting_setsStatusCancelled() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.canceled", "lead@example.com",
                null, null, "https://calendly.com/events/abc123", null);

        CalendlyMeeting existing = CalendlyMeeting.builder()
                .calendlyUri("https://calendly.com/events/abc123")
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(calendlyMeetingRepository.findByCalendlyUri("https://calendly.com/events/abc123"))
                .thenReturn(Optional.of(existing));

        service.handleWebhook(payload, Map.of());

        assertThat(existing.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
        verify(calendlyMeetingRepository).save(existing);
    }

    @Test
    void inviteeCanceled_noMatchingMeeting_doesNotThrow() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.canceled", "lead@example.com",
                null, null, "https://calendly.com/events/missing", null);

        when(calendlyMeetingRepository.findByCalendlyUri("https://calendly.com/events/missing"))
                .thenReturn(Optional.empty());

        service.handleWebhook(payload, Map.of());

        verify(calendlyMeetingRepository, never()).save(any());
    }

    @Test
    void inviteeRescheduled_updatesUriAndTimesAndStatus() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.rescheduled", "lead@example.com",
                "2026-05-02T10:00:00Z", "2026-05-02T10:30:00Z",
                "https://calendly.com/events/new", "https://calendly.com/events/old");

        CalendlyMeeting existing = CalendlyMeeting.builder()
                .calendlyUri("https://calendly.com/events/old")
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(calendlyMeetingRepository.findByCalendlyUri("https://calendly.com/events/old"))
                .thenReturn(Optional.of(existing));

        service.handleWebhook(payload, Map.of());

        assertThat(existing.getCalendlyUri()).isEqualTo("https://calendly.com/events/new");
        assertThat(existing.getStatus()).isEqualTo(MeetingStatus.RESCHEDULED);
        verify(calendlyMeetingRepository).save(existing);
    }

    @Test
    void inviteeRescheduled_missingOldUri_doesNotThrow() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.rescheduled", "lead@example.com",
                "2026-05-02T10:00:00Z", "2026-05-02T10:30:00Z",
                "https://calendly.com/events/new", null);

        service.handleWebhook(payload, Map.of());

        verify(calendlyMeetingRepository, never()).save(any());
    }

    @Test
    void unknownEventType_doesNotThrow_logsWarning() {
        CalendlyWebhookPayload payload = new CalendlyWebhookPayload();
        payload.setEvent("invitee.unknown_future_event");
        payload.setPayload(new CalendlyWebhookPayload.Payload());

        service.handleWebhook(payload, Map.of());

        verify(calendlyMeetingRepository, never()).save(any());
    }

    @Test
    void handleWebhook_success_logsSuccessTrue() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "lead@example.com",
                "2026-05-01T10:00:00Z", "2026-05-01T10:30:00Z",
                "https://calendly.com/events/abc", null);

        when(clientRepository.findByPrimaryEmail(any())).thenReturn(Optional.empty());

        service.handleWebhook(payload, Map.of());

        ArgumentCaptor<CalendlyWebhookLog> logCaptor = ArgumentCaptor.forClass(CalendlyWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().isSuccess()).isTrue();
    }

    @Test
    void handleWebhook_exception_logsSuccessFalseWithErrorDetails() {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "lead@example.com",
                "not-a-valid-datetime", "not-a-valid-datetime",
                "https://calendly.com/events/abc", null);

        service.handleWebhook(payload, Map.of());

        ArgumentCaptor<CalendlyWebhookLog> logCaptor = ArgumentCaptor.forClass(CalendlyWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().isSuccess()).isFalse();
        assertThat(logCaptor.getValue().getErrorDetails()).isNotBlank();
    }

    @Test
    void retryFailedWebhooks_noFailed_doesNothing() {
        when(webhookLogRepository.findBySuccessFalseAndRetryCountLessThan(3))
                .thenReturn(Collections.emptyList());

        service.retryFailedWebhooks();

        verify(webhookLogRepository, never()).save(any());
    }

    @Test
    void retryFailedWebhooks_succeeds_setsSuccessTrue() throws Exception {
        CalendlyWebhookPayload payload = buildPayload(
                "invitee.created", "lead@example.com",
                "2026-05-01T10:00:00Z", "2026-05-01T10:30:00Z",
                "https://calendly.com/events/retry", null);

        CalendlyWebhookLog log = CalendlyWebhookLog.builder()
                .event("invitee.created")
                .payload("{}")
                .success(false)
                .retryCount(0)
                .build();

        when(webhookLogRepository.findBySuccessFalseAndRetryCountLessThan(3)).thenReturn(List.of(log));
        when(objectMapper.readValue("{}", CalendlyWebhookPayload.class)).thenReturn(payload);
        when(clientRepository.findByPrimaryEmail(any())).thenReturn(Optional.empty());

        service.retryFailedWebhooks();

        assertThat(log.isSuccess()).isTrue();
        verify(webhookLogRepository).save(log);
    }

    @Test
    void retryFailedWebhooks_fails_incrementsRetryCount() throws Exception {
        CalendlyWebhookLog log = CalendlyWebhookLog.builder()
                .event("invitee.created")
                .payload("{}")
                .success(false)
                .retryCount(0)
                .build();

        when(webhookLogRepository.findBySuccessFalseAndRetryCountLessThan(3)).thenReturn(List.of(log));
        when(objectMapper.readValue("{}", CalendlyWebhookPayload.class))
                .thenThrow(new RuntimeException("parse error"));

        service.retryFailedWebhooks();

        assertThat(log.getRetryCount()).isEqualTo(1);
        verify(webhookLogRepository).save(log);
    }

    @Test
    void retryFailedWebhooks_maxRetries_notifiesAdmin() throws Exception {
        CalendlyWebhookLog log = CalendlyWebhookLog.builder()
                .event("invitee.created")
                .payload("{}")
                .success(false)
                .retryCount(2)
                .build();

        when(webhookLogRepository.findBySuccessFalseAndRetryCountLessThan(3)).thenReturn(List.of(log));
        when(objectMapper.readValue("{}", CalendlyWebhookPayload.class))
                .thenThrow(new RuntimeException("still failing"));

        service.retryFailedWebhooks();

        verify(emailService).notifyAdminOfWebhookFailure(any());
    }
}

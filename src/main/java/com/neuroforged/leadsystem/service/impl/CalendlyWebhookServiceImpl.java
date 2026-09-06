package com.neuroforged.leadsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;
import com.neuroforged.leadsystem.entity.*;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.CalendlyWebhookLogRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendlyWebhookServiceImpl implements CalendlyWebhookService {

    private static final int MAX_RETRIES = 3;

    private final CalendlyWebhookLogRepository webhookLogRepository;
    private final CalendlyMeetingRepository calendlyMeetingRepository;
    private final ClientRepository clientRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final BusinessEventLogger eventLogger;

    @Override
    public void handleWebhook(CalendlyWebhookPayload payload, Map<String, String> headers) {
        log.info("Handling Calendly webhook: {}", payload.getEvent());
        CalendlyWebhookLog log_ = CalendlyWebhookLog.builder()
                .event(payload.getEvent())
                .headers(headers.toString())
                .payload(serialise(payload))
                .receivedAt(ZonedDateTime.now(ZoneId.of("UTC")))
                .retryCount(0)
                .build();

        try {
            processPayload(payload);
            log_.setSuccess(true);
        } catch (Exception ex) {
            log.error("Failed to process Calendly webhook: {}", payload.getEvent(), ex);
            log_.setSuccess(false);
            log_.setErrorDetails(ex.getMessage());
        }

        webhookLogRepository.save(log_);
    }

    /** The stored payload is re-parsed by the retry scheduler, so it must be JSON — not
     *  Lombok's toString(), which made every retry fail on JsonParseException before
     *  processing was even attempted. */
    private String serialise(CalendlyWebhookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise Calendly webhook payload for the retry log: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public CalendlyWebhookService.RetryResult retryFailedWebhooks() {
        List<CalendlyWebhookLog> failed = webhookLogRepository
                .findBySuccessFalseAndRetryCountLessThan(MAX_RETRIES);

        if (failed.isEmpty()) {
            return new CalendlyWebhookService.RetryResult(0, 0);
        }

        log.debug("Retrying {} failed Calendly webhook(s)", failed.size());

        int retried = 0;
        int deadLettered = 0;

        for (CalendlyWebhookLog entry : failed) {
            try {
                CalendlyWebhookPayload payload = objectMapper.readValue(entry.getPayload(), CalendlyWebhookPayload.class);
                processPayload(payload);
                entry.setSuccess(true);
                entry.setErrorDetails(null);
                retried++;
                log.debug("Webhook retry succeeded for log id={}", entry.getId());
            } catch (Exception ex) {
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setErrorDetails(ex.getMessage());
                log.warn("Webhook retry failed (attempt {}) for log id={}: {}", entry.getRetryCount(), entry.getId(), ex.getMessage());

                if (entry.getRetryCount() >= MAX_RETRIES) {
                    deadLettered++;
                    log.error("Webhook dead-lettered after {} attempts for log id={}", MAX_RETRIES, entry.getId());
                    emailService.notifyAdminOfWebhookFailure(
                            "Calendly webhook permanently failed after " + MAX_RETRIES + " attempts.\n" +
                            "Event: " + entry.getEvent() + "\n" +
                            "Error: " + ex.getMessage());
                }
            }
            webhookLogRepository.save(entry);
        }

        return new CalendlyWebhookService.RetryResult(retried, deadLettered);
    }

    private void processPayload(CalendlyWebhookPayload payload) {
        String eventType = payload.getEvent();

        switch (eventType) {
            case "invitee.created" -> handleCreated(payload);
            case "invitee.canceled" -> handleCanceled(payload);
            case "invitee.rescheduled" -> handleRescheduled(payload);
            default -> log.warn("Unhandled Calendly event type: {}", eventType);
        }
    }

    private void handleCreated(CalendlyWebhookPayload payload) {
        CalendlyWebhookPayload.Payload p = payload.getPayload();
        String uri = p.getEvent();

        // App-level idempotency check (fast path before the DB round-trip)
        if (calendlyMeetingRepository.findByCalendlyUri(uri).isPresent()) {
            log.info("Idempotent skip — meeting already exists for uri={}", uri);
            return;
        }

        String inviteeEmail = p.getInvitee().getEmail();
        ZonedDateTime start = ZonedDateTime.parse(p.getScheduledEvent().getStartTime());
        ZonedDateTime end = ZonedDateTime.parse(p.getScheduledEvent().getEndTime());
        Optional<Client> client = clientRepository.findByPrimaryEmail(inviteeEmail);

        CalendlyMeeting meeting = CalendlyMeeting.builder()
                .calendlyUri(uri)
                .eventType(p.getEventType().getName())
                .inviteeEmail(inviteeEmail)
                .inviteeName(p.getInvitee().getName())
                .startTime(start)
                .endTime(end)
                .status(MeetingStatus.SCHEDULED)
                .client(client.orElse(null))
                .build();

        try {
            calendlyMeetingRepository.save(meeting);
            String clientName = client.map(Client::getName).orElse(null);
            eventLogger.meetingBooked(clientName, inviteeEmail, p.getEventType().getName(), start);
            log.debug("Created CalendlyMeeting for invitee={}", inviteeEmail);
            client.ifPresent(c -> notificationService.notify(c.getId(), NotificationEventType.MEETING_BOOKED,
                    Map.of("Invitee", inviteeEmail,
                            "Name", p.getInvitee().getName() != null ? p.getInvitee().getName() : "",
                            "Start", start.toString(),
                            "Event Type", p.getEventType().getName() != null ? p.getEventType().getName() : "")));
        } catch (DataIntegrityViolationException e) {
            // Race condition — another thread/node inserted the same URI concurrently; treat as idempotent
            log.info("Idempotent skip (concurrent insert) — meeting already exists for uri={}", uri);
        }
    }

    private void handleCanceled(CalendlyWebhookPayload payload) {
        CalendlyWebhookPayload.Payload p = payload.getPayload();
        String uri = p.getEvent();
        String inviteeEmail = p.getInvitee() != null ? p.getInvitee().getEmail() : "unknown";
        String eventTypeName = p.getEventType() != null ? p.getEventType().getName() : null;

        calendlyMeetingRepository.findByCalendlyUri(uri).ifPresentOrElse(meeting -> {
            meeting.setStatus(MeetingStatus.CANCELLED);
            calendlyMeetingRepository.save(meeting);
            String clientName = meeting.getClient() != null ? meeting.getClient().getName() : null;
            eventLogger.meetingCancelled(clientName, inviteeEmail, eventTypeName);
            log.debug("Marked meeting CANCELLED: uri={}", uri);
        }, () -> log.warn("Received canceled event but no meeting found for uri={}", uri));
    }

    private void handleRescheduled(CalendlyWebhookPayload payload) {
        CalendlyWebhookPayload.Payload p = payload.getPayload();
        String oldUri = p.getOldEvent();
        String newUri = p.getEvent();

        if (oldUri == null) {
            log.warn("Rescheduled event missing old_event URI — cannot update meeting");
            return;
        }

        calendlyMeetingRepository.findByCalendlyUri(oldUri).ifPresentOrElse(meeting -> {
            meeting.setCalendlyUri(newUri);
            meeting.setStartTime(ZonedDateTime.parse(p.getScheduledEvent().getStartTime()));
            meeting.setEndTime(ZonedDateTime.parse(p.getScheduledEvent().getEndTime()));
            meeting.setStatus(MeetingStatus.RESCHEDULED);
            calendlyMeetingRepository.save(meeting);
            log.info("Rescheduled meeting: old={} new={}", oldUri, newUri);
        }, () -> log.warn("Received rescheduled event but no meeting found for uri={}", oldUri));
    }
}

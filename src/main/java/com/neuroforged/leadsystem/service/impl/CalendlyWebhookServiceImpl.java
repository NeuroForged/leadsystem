package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.CalendlyWebhookLog;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.CalendlyWebhookLogRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendlyWebhookServiceImpl implements CalendlyWebhookService {

    private final CalendlyWebhookLogRepository webhookLogRepository;
    private final CalendlyMeetingRepository calendlyMeetingRepository;
    private final ClientRepository clientRepository;

    @Override
    public void handleWebhook(CalendlyWebhookPayload webhookPayload, Map<String, String> headers) {
        log.info("Handling Calendly webhook: {}", webhookPayload.getEvent());

        try {
            String inviteeEmail = webhookPayload.getPayload().getInvitee().getEmail();
            ZonedDateTime start = ZonedDateTime.parse(webhookPayload.getPayload().getScheduledEvent().getStartTime());
            ZonedDateTime end = ZonedDateTime.parse(webhookPayload.getPayload().getScheduledEvent().getEndTime());

            Optional<Client> clientOpt = clientRepository.findByPrimaryEmail(inviteeEmail);
            Client client = clientOpt.orElse(null);

            CalendlyMeeting meeting = new CalendlyMeeting();
            meeting.setCalendlyUri(webhookPayload.getPayload().getEvent());
            meeting.setEventType(webhookPayload.getPayload().getEventType().getName());
            meeting.setInviteeEmail(inviteeEmail);
            meeting.setStartTime(start);
            meeting.setEndTime(end);
            meeting.setClient(client);

            calendlyMeetingRepository.save(meeting);

            webhookLogRepository.save(CalendlyWebhookLog.builder()
                    .event(webhookPayload.getEvent())
                    .headers(headers.toString())
                    .payload(webhookPayload.toString())
                    .receivedAt(ZonedDateTime.now(ZoneId.of("UTC")))
                    .success(true)
                    .retryCount(0)
                    .build());

            log.info("Calendly webhook processed and meeting saved for invitee: {}", inviteeEmail);
        } catch (Exception ex) {
            log.error("Failed to process Calendly webhook", ex);
            webhookLogRepository.save(CalendlyWebhookLog.builder()
                    .event(webhookPayload.getEvent())
                    .headers(headers.toString())
                    .payload(webhookPayload.toString())
                    .receivedAt(ZonedDateTime.now(ZoneId.of("UTC")))
                    .success(false)
                    .errorDetails(ex.getMessage())
                    .retryCount(0)
                    .build());
        }
    }

    @Override
    public void retryFailedWebhooks() {
        // Not implemented yet
    }
}

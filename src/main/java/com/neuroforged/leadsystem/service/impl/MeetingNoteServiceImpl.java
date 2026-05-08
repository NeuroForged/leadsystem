package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.FirefliesWebhookPayload;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.MeetingNote;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.MeetingNoteRepository;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.MeetingNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingNoteServiceImpl implements MeetingNoteService {

    private final MeetingNoteRepository meetingNoteRepository;
    private final CalendlyMeetingRepository calendlyMeetingRepository;
    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Value("${neuroforged.admin.email}")
    private String fallbackEmail;

    @Override
    @Transactional
    public void processWebhook(Long clientId, FirefliesWebhookPayload payload) {
        FirefliesWebhookPayload.Meeting ffMeeting = payload.getMeeting();
        if (ffMeeting == null) {
            log.warn("[FIREFLIES] Payload has no meeting object — clientId={}", clientId);
            return;
        }

        String firefliesId = payload.getMeetingId() != null ? payload.getMeetingId() : ffMeeting.getId();
        if (firefliesId == null) {
            log.warn("[FIREFLIES] Cannot determine firefliesId — clientId={}", clientId);
            return;
        }

        if (meetingNoteRepository.existsByFirefliesId(firefliesId)) {
            log.info("[FIREFLIES] Duplicate — firefliesId={} already processed, skipping", firefliesId);
            return;
        }

        ZonedDateTime meetingStart = toUtc(ffMeeting.getStartTime());
        if (meetingStart == null) {
            log.warn("[FIREFLIES] Missing startTime — firefliesId={}", firefliesId);
            return;
        }

        Optional<CalendlyMeeting> matched = matchCalendlyMeeting(clientId, ffMeeting, meetingStart);
        if (matched.isEmpty()) {
            log.warn("[FIREFLIES] No CalendlyMeeting match — firefliesId={} clientId={} start={}",
                    firefliesId, clientId, meetingStart);
            return;
        }

        CalendlyMeeting calendlyMeeting = matched.get();
        String summary = buildSummaryText(ffMeeting);
        String transcript = ffMeeting.getTranscript();

        MeetingNote note = MeetingNote.builder()
                .meeting(calendlyMeeting)
                .firefliesId(firefliesId)
                .summary(summary)
                .transcript(transcript)
                .build();

        meetingNoteRepository.save(note);
        log.info("[FIREFLIES] Saved MeetingNote id={} linked to CalendlyMeeting id={} firefliesId={}",
                note.getId(), calendlyMeeting.getId(), firefliesId);

        sendNotification(clientId, ffMeeting, summary);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Optional<CalendlyMeeting> matchCalendlyMeeting(Long clientId,
                                                            FirefliesWebhookPayload.Meeting ffMeeting,
                                                            ZonedDateTime start) {
        List<FirefliesWebhookPayload.Participant> participants = ffMeeting.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return Optional.empty();
        }

        ZonedDateTime from = start.minusMinutes(10);
        ZonedDateTime to   = start.plusMinutes(10);

        for (FirefliesWebhookPayload.Participant p : participants) {
            String email = p.getEmail();
            if (email == null || email.isBlank()) continue;

            List<CalendlyMeeting> candidates =
                    calendlyMeetingRepository.findByInviteeEmailAndStartTimeBetween(email, from, to);

            Optional<CalendlyMeeting> forClient = candidates.stream()
                    .filter(m -> m.getClient() != null && clientId.equals(m.getClient().getId()))
                    .findFirst();

            if (forClient.isPresent()) {
                return forClient;
            }
        }
        return Optional.empty();
    }

    private String buildSummaryText(FirefliesWebhookPayload.Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(meeting.getTitle() != null ? meeting.getTitle() : "—").append("\n\n");

        FirefliesWebhookPayload.Summary s = meeting.getSummary();
        if (s != null) {
            if (s.getOverview() != null && !s.getOverview().isBlank()) {
                sb.append("Overview:\n").append(s.getOverview()).append("\n\n");
            }
            if (s.getActionItems() != null && !s.getActionItems().isBlank()) {
                sb.append("Action Items:\n").append(s.getActionItems()).append("\n\n");
            }
            if (s.getKeywords() != null && !s.getKeywords().isBlank()) {
                sb.append("Keywords: ").append(s.getKeywords()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void sendNotification(Long clientId, FirefliesWebhookPayload.Meeting meeting, String summaryText) {
        String subject = "Meeting Notes Ready — " + (meeting.getTitle() != null ? meeting.getTitle() : "Untitled Meeting");
        String body = "Your meeting notes from Fireflies.ai are now available.\n\n" + summaryText;

        String[] recipients = resolveRecipients(clientId);
        try {
            emailService.sendLeadToMultiple(recipients, subject, body);
        } catch (Exception e) {
            log.warn("[FIREFLIES] Failed to send notification email for clientId={}: {}", clientId, e.getMessage());
        }
    }

    private String[] resolveRecipients(Long clientId) {
        return clientRepository.findById(clientId)
                .map(Client::getNotificationEmails)
                .filter(emails -> emails != null && !emails.isBlank())
                .map(emails -> Arrays.stream(emails.split(","))
                        .map(String::trim)
                        .filter(e -> !e.isBlank())
                        .toArray(String[]::new))
                .orElse(new String[]{fallbackEmail});
    }

    private ZonedDateTime toUtc(Long epochMillis) {
        if (epochMillis == null) return null;
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}

package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.CalendlyWebhookLog;
import com.neuroforged.leadsystem.dto.ScheduledEventDTO;
import com.neuroforged.leadsystem.dto.InviteeDTO;
import com.neuroforged.leadsystem.calendly.CalendlyEventSource;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.CalendlyWebhookLogRepository;
import com.neuroforged.leadsystem.client.CalendlyApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CalendlyPollingJob {

    private final CalendlyAccountRepository calendlyAccountRepository;
    private final CalendlyMeetingRepository calendlyMeetingRepository;
    private final CalendlyWebhookLogRepository calendlyWebhookLogRepository;
    private final CalendlyApiClient calendlyApiClient;

    @Scheduled(fixedDelayString = "${calendly.poll.fixedDelayMs:90000}")
    public void pollCalendly() {
        List<CalendlyAccount> accounts = calendlyAccountRepository.findByPollEnabledTrue();
        Instant now = Instant.now();

        for (CalendlyAccount account : accounts) {
            Instant lastPolled = account.getLastPolledAt();
            if (lastPolled == null) {
                lastPolled = now.minus(Duration.ofDays(7));
            }
            Instant minStart = lastPolled.minus(Duration.ofMinutes(5));
            Instant maxStart = now.plus(Duration.ofDays(30));

            try {
                List<ScheduledEventDTO> events = calendlyApiClient.listScheduledEvents(account, minStart, maxStart);
                int upserts = 0;
                for (ScheduledEventDTO eventDto : events) {
                    String uri = eventDto.getScheduledEventUri();
                    Instant startInstant = eventDto.getStartTime();
                    ZonedDateTime startTime = startInstant != null ? ZonedDateTime.ofInstant(startInstant, ZoneId.of("UTC")) : null;
                    if (uri == null || startTime == null) {
                        continue;
                    }
                    Optional<CalendlyMeeting> existingOpt = calendlyMeetingRepository.findByCalendlyUriAndStartTime(uri, startTime);
                    CalendlyMeeting meeting = existingOpt.orElseGet(CalendlyMeeting::new);
                    meeting.setCalendlyUri(uri);
                    meeting.setStartTime(startTime);
                    if (eventDto.getEndTime() != null) {
                        meeting.setEndTime(ZonedDateTime.ofInstant(eventDto.getEndTime(), ZoneId.of("UTC")));
                    }
                    meeting.setEventType(eventDto.getEventTypeName());
                    // set clientId from account
                    meeting.setClientId(account.getClientId());
                    calendlyMeetingRepository.save(meeting);
                    upserts++;
                }

                account.setLastPolledAt(now);
                calendlyAccountRepository.save(account);

                CalendlyWebhookLog logRecord = new CalendlyWebhookLog();
                logRecord.setSource(CalendlyEventSource.POLLING);
                logRecord.setReceivedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                logRecord.setPayload("Polling ok: " + upserts + " events from " + minStart + " to " + maxStart);
                calendlyWebhookLogRepository.save(logRecord);
            } catch (Exception ex) {
                log.error("Error polling Calendly for account {}", account.getId(), ex);
                CalendlyWebhookLog logRecord = new CalendlyWebhookLog();
                logRecord.setSource(CalendlyEventSource.POLLING);
                logRecord.setReceivedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                logRecord.setErrorDetails(ex.getMessage());
                calendlyWebhookLogRepository.save(logRecord);
            }
        }
    }
}

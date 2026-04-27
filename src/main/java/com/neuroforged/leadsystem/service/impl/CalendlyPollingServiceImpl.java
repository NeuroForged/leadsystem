package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.dto.CalendlyEventInviteesResponse;
import com.neuroforged.leadsystem.dto.CalendlyScheduledEventsResponse;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.MeetingStatus;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.CalendlyPollingService;
import com.neuroforged.leadsystem.service.CalendlyTokenRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendlyPollingServiceImpl implements CalendlyPollingService {

    private static final int DEFAULT_LOOKBACK_HOURS = 24;

    private final CalendlyAccountRepository accountRepository;
    private final CalendlyMeetingRepository meetingRepository;
    private final ClientRepository clientRepository;
    private final CalendlyApiClient apiClient;
    private final CalendlyTokenRefreshService tokenRefreshService;

    @Override
    public void pollAllAccounts() {
        List<CalendlyAccount> accounts = accountRepository.findAllByUsePollingTrueAndRequiresReauthFalse();

        if (accounts.isEmpty()) {
            return;
        }

        log.info("Polling {} Calendly account(s) for new events", accounts.size());

        for (CalendlyAccount account : accounts) {
            try {
                pollAccount(account);
            } catch (Exception ex) {
                log.error("Polling failed for clientId={}: {}", account.getClientId(), ex.getMessage(), ex);
            }
        }
    }

    private void pollAccount(CalendlyAccount account) {
        CalendlyAccount fresh = tokenRefreshService.ensureFreshToken(account.getClientId());

        ZonedDateTime since = account.getLastPolledAt() != null
                ? account.getLastPolledAt().atZone(ZoneId.of("UTC"))
                : ZonedDateTime.now(ZoneId.of("UTC")).minusHours(DEFAULT_LOOKBACK_HOURS);

        log.info("Polling Calendly events for clientId={} since {}", account.getClientId(), since);

        syncEvents(fresh.getAccessToken(), fresh.getOrganization(), since, MeetingStatus.SCHEDULED);
        syncEvents(fresh.getAccessToken(), fresh.getOrganization(), since, MeetingStatus.CANCELLED);

        account.setLastPolledAt(LocalDateTime.now(ZoneId.of("UTC")));
        accountRepository.save(account);
    }

    private void syncEvents(String accessToken, String organization, ZonedDateTime since, MeetingStatus targetStatus) {
        String apiStatus = (targetStatus == MeetingStatus.SCHEDULED) ? "active" : "canceled";

        CalendlyScheduledEventsResponse response;
        try {
            response = apiClient.fetchScheduledEvents(accessToken, organization, since, apiStatus);
        } catch (Exception ex) {
            log.warn("Failed to fetch {} events from Calendly: {}", apiStatus, ex.getMessage());
            return;
        }

        if (response == null || response.getCollection() == null) {
            return;
        }

        for (CalendlyScheduledEventsResponse.Event event : response.getCollection()) {
            try {
                processEvent(accessToken, event, targetStatus);
            } catch (Exception ex) {
                log.warn("Failed to process polled event uri={}: {}", event.getUri(), ex.getMessage());
            }
        }
    }

    private void processEvent(String accessToken, CalendlyScheduledEventsResponse.Event event, MeetingStatus targetStatus) {
        String eventUuid = extractUuid(event.getUri());

        if (targetStatus == MeetingStatus.CANCELLED) {
            meetingRepository.findByCalendlyUri(event.getUri()).ifPresent(meeting -> {
                if (meeting.getStatus() != MeetingStatus.CANCELLED) {
                    meeting.setStatus(MeetingStatus.CANCELLED);
                    meetingRepository.save(meeting);
                    log.info("Polling marked meeting CANCELLED: uri={}", event.getUri());
                }
            });
            return;
        }

        CalendlyEventInviteesResponse inviteesResponse = apiClient.fetchEventInvitees(accessToken, eventUuid);
        if (inviteesResponse == null || inviteesResponse.getCollection() == null) {
            return;
        }

        ZonedDateTime start = ZonedDateTime.parse(event.getStartTime());
        ZonedDateTime end = ZonedDateTime.parse(event.getEndTime());

        for (CalendlyEventInviteesResponse.Invitee invitee : inviteesResponse.getCollection()) {
            meetingRepository.findByCalendlyUri(event.getUri()).ifPresentOrElse(
                    existing -> {
                        existing.setStartTime(start);
                        existing.setEndTime(end);
                        meetingRepository.save(existing);
                    },
                    () -> {
                        CalendlyMeeting meeting = CalendlyMeeting.builder()
                                .calendlyUri(event.getUri())
                                .eventType(event.getName())
                                .inviteeEmail(invitee.getEmail())
                                .startTime(start)
                                .endTime(end)
                                .status(MeetingStatus.SCHEDULED)
                                .client(clientRepository.findByPrimaryEmail(invitee.getEmail()).orElse(null))
                                .build();
                        meetingRepository.save(meeting);
                        log.info("Polling created meeting for invitee={} uri={}", invitee.getEmail(), event.getUri());
                    }
            );
        }
    }

    private String extractUuid(String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }
}

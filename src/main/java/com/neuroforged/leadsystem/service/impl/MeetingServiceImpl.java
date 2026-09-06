package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.MeetingResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.MeetingOutcome;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final CalendlyMeetingRepository meetingRepository;
    private final LeadRepository leadRepository;

    @Override
    public PagedResponse<MeetingResponseDTO> getMeetings(Long clientId, String from, String to, String inviteeEmail, int page, int size) {
        // Cap like LeadController does; size came straight from the query string.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));

        ZonedDateTime fromDt = from != null ? java.time.LocalDate.parse(from).atStartOfDay(ZoneId.of("UTC")) : null;
        ZonedDateTime toDt = to != null ? java.time.LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.of("UTC")) : null;
        String invitee = (inviteeEmail != null && !inviteeEmail.isBlank()) ? inviteeEmail.trim() : null;

        // Single query-level filter (invitee included) so pagination counts are correct and
        // matches across all pages are returned; client is fetch-joined to avoid N+1.
        Page<CalendlyMeeting> meetingPage = meetingRepository.search(clientId, fromDt, toDt, invitee, pageable);

        Map<String, Long> leadIdByEmailClient = resolveLeadIds(meetingPage.getContent());
        Page<MeetingResponseDTO> mapped = meetingPage.map(m -> toDto(m, leadIdByEmailClient));
        return PagedResponse.from(mapped);
    }

    /** One IN-query to resolve leadId for every (inviteeEmail, clientId) pair on the page. */
    private Map<String, Long> resolveLeadIds(List<CalendlyMeeting> meetings) {
        Set<String> emails = new LinkedHashSet<>();
        Set<Long> clientIds = new LinkedHashSet<>();
        for (CalendlyMeeting m : meetings) {
            if (m.getInviteeEmail() != null && m.getClient() != null) {
                emails.add(m.getInviteeEmail());
                clientIds.add(m.getClient().getId());
            }
        }
        if (emails.isEmpty() || clientIds.isEmpty()) {
            return Map.of();
        }
        return leadRepository
                .findIdEmailClientByEmailsAndClientIds(List.copyOf(emails), List.copyOf(clientIds))
                .stream()
                .collect(Collectors.toMap(
                        row -> leadKey((String) row[1], (Long) row[2]),
                        row -> (Long) row[0],
                        (a, b) -> a));
    }

    private static String leadKey(String email, Long clientId) {
        return email + "|" + clientId;
    }

    @Override
    public MeetingResponseDTO getMeeting(Long id) {
        CalendlyMeeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + id));
        return toDto(meeting);
    }

    @Override
    public MeetingResponseDTO updateOutcome(Long id, MeetingOutcome outcome) {
        CalendlyMeeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + id));
        meeting.setOutcome(outcome);
        return toDto(meetingRepository.save(meeting));
    }

    /** Single-meeting path (get-by-id / update): one targeted lookup is fine. */
    private MeetingResponseDTO toDto(CalendlyMeeting m) {
        Long leadId = null;
        if (m.getInviteeEmail() != null && m.getClient() != null) {
            leadId = leadRepository.findByEmailAndClient_Id(m.getInviteeEmail(), m.getClient().getId())
                    .map(l -> l.getId())
                    .orElse(null);
        }
        return toDto(m, leadId);
    }

    /** List path: leadId already batch-resolved into {@code leadIds}. */
    private MeetingResponseDTO toDto(CalendlyMeeting m, Map<String, Long> leadIds) {
        Long leadId = (m.getInviteeEmail() != null && m.getClient() != null)
                ? leadIds.get(leadKey(m.getInviteeEmail(), m.getClient().getId()))
                : null;
        return toDto(m, leadId);
    }

    private MeetingResponseDTO toDto(CalendlyMeeting m, Long leadId) {
        return MeetingResponseDTO.builder()
                .id(m.getId())
                .calendlyUri(m.getCalendlyUri())
                .eventType(m.getEventType())
                .startTime(m.getStartTime())
                .endTime(m.getEndTime())
                .inviteeEmail(m.getInviteeEmail())
                .inviteeName(m.getInviteeName())
                .status(m.getStatus() != null ? m.getStatus().name() : null)
                .outcome(m.getOutcome() != null ? m.getOutcome().name() : null)
                .clientId(m.getClient() != null ? m.getClient().getId() : null)
                .clientName(m.getClient() != null ? m.getClient().getName() : null)
                .leadId(leadId)
                .createdAt(m.getStartTime() != null ? m.getStartTime().toLocalDateTime() : null)
                .build();
    }
}

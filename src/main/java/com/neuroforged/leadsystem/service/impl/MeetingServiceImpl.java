package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.MeetingResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final CalendlyMeetingRepository meetingRepository;
    private final LeadRepository leadRepository;

    @Override
    public PagedResponse<MeetingResponseDTO> getMeetings(Long clientId, String from, String to, String inviteeEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        ZonedDateTime fromDt = from != null ? java.time.LocalDate.parse(from).atStartOfDay(ZoneId.of("UTC")) : null;
        ZonedDateTime toDt = to != null ? java.time.LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.of("UTC")) : null;

        Page<CalendlyMeeting> meetingPage;
        if (clientId != null && fromDt != null) {
            meetingPage = meetingRepository.findByClient_IdAndStartTimeBetween(clientId, fromDt, toDt, pageable);
        } else if (clientId != null) {
            meetingPage = meetingRepository.findByClient_Id(clientId, pageable);
        } else if (fromDt != null) {
            meetingPage = meetingRepository.findByStartTimeBetween(fromDt, toDt, pageable);
        } else {
            meetingPage = meetingRepository.findAll(pageable);
        }

        // Apply inviteeEmail filter in memory if needed
        List<MeetingResponseDTO> dtos;
        if (inviteeEmail != null && !inviteeEmail.isBlank()) {
            String emailFilter = inviteeEmail.toLowerCase();
            dtos = meetingPage.getContent().stream()
                    .filter(m -> m.getInviteeEmail() != null && m.getInviteeEmail().toLowerCase().contains(emailFilter))
                    .map(this::toDto)
                    .toList();
            Page<MeetingResponseDTO> filtered = new PageImpl<>(dtos, pageable, dtos.size());
            return PagedResponse.from(filtered);
        }

        Page<MeetingResponseDTO> mapped = meetingPage.map(this::toDto);
        return PagedResponse.from(mapped);
    }

    @Override
    public MeetingResponseDTO getMeeting(Long id) {
        CalendlyMeeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + id));
        return toDto(meeting);
    }

    private MeetingResponseDTO toDto(CalendlyMeeting m) {
        Long leadId = null;
        if (m.getInviteeEmail() != null && m.getClient() != null) {
            leadId = leadRepository.findByEmailAndClientId(m.getInviteeEmail(), String.valueOf(m.getClient().getId()))
                    .map(l -> l.getId())
                    .orElse(null);
        }

        return MeetingResponseDTO.builder()
                .id(m.getId())
                .calendlyUri(m.getCalendlyUri())
                .eventType(m.getEventType())
                .startTime(m.getStartTime())
                .endTime(m.getEndTime())
                .inviteeEmail(m.getInviteeEmail())
                .inviteeName(m.getInviteeName())
                .status(m.getStatus() != null ? m.getStatus().name() : null)
                .clientId(m.getClient() != null ? m.getClient().getId() : null)
                .clientName(m.getClient() != null ? m.getClient().getName() : null)
                .leadId(leadId)
                .createdAt(m.getStartTime() != null ? m.getStartTime().toLocalDateTime() : null)
                .build();
    }
}

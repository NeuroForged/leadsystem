package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.MeetingResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.MeetingOutcome;

public interface MeetingService {
    PagedResponse<MeetingResponseDTO> getMeetings(Long clientId, String from, String to, String inviteeEmail, int page, int size);
    MeetingResponseDTO getMeeting(Long id);
    MeetingResponseDTO updateOutcome(Long id, MeetingOutcome outcome);
}

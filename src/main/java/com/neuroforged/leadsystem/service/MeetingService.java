package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.MeetingResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;

public interface MeetingService {
    PagedResponse<MeetingResponseDTO> getMeetings(Long clientId, String from, String to, String inviteeEmail, int page, int size);
    MeetingResponseDTO getMeeting(Long id);
}

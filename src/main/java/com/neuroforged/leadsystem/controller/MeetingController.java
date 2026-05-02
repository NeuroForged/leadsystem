package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.MeetingResponseDTO;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<MeetingResponseDTO>> getMeetings(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String inviteeEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(meetingService.getMeetings(clientId, from, to, inviteeEmail, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeetingResponseDTO> getMeeting(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.getMeeting(id));
    }
}

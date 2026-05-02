package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponseDTO {
    private Long id;
    private String calendlyUri;
    private String eventType;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String inviteeEmail;
    private String inviteeName;
    private String status;
    private Long clientId;
    private String clientName;
    private Long leadId;
    private LocalDateTime createdAt;
}

package com.neuroforged.leadsystem.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEventDTO {
    private String scheduledEventUri;
    private Instant startTime;
    private Instant endTime;
    private String eventTypeName;
}

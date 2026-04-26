package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendlyMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String calendlyUri;
    private String eventType;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}

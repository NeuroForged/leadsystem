package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.ZonedDateTime;

@FilterDef(name = "longClientFilter", parameters = @ParamDef(name = "clientId", type = Long.class))
@Filter(name = "longClientFilter", condition = "client_id = :clientId")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendlyMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String calendlyUri;
    private String eventType;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String inviteeEmail;
    private String inviteeName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MeetingOutcome outcome;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}

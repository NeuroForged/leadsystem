package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@Data
@Table(indexes = { @Index(name = "ux_calendlymeeting_uri_start", columnList = "calendlyUri,startTime", unique = true) })
public class CalendlyMeeting {

   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String calendlyUri;
    private String eventType;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String inviteeEmail;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}

package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
public class CalendlyWebhookLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 10000)
    private String payload;

    @Column(length = 5000)
    private String headers;


    private String event;
    private boolean success;
    private int retryCount;
    private ZonedDateTime receivedAt;

    @Column(length = 2000)
    private String errorDetails;
}

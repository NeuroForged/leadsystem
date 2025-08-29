package com.neuroforged.leadsystem.entity;

import com.neuroforged.leadsystem.calendly.CalendlyEventSource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendlyWebhookLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String payload;

    @Lob
    private String headers;

    private String event;
    private boolean success;
    private int retryCount;
    private ZonedDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    private CalendlyEventSource source;

    @Lob
    private String errorDetails;
}

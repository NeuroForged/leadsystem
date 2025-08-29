package com.neuroforged.leadsystem.entity;
import com.neuroforged.leadsystem.calendly.CalendlyEventSource;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

// test java.time.ZonedDateTime;
//test commit via chatgpt
@Entity
@Data
@Builder
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

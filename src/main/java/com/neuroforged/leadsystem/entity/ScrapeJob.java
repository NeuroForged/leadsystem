package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private String scraperJobId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScrapeJobStatus status = ScrapeJobStatus.PENDING;

    @Column(nullable = false, length = 500)
    private String url;

    @Builder.Default
    private Integer maxPages = 1000;

    private String initiatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    private Integer scrapedCount;
    private Integer errorCount;

    @Column(length = 500)
    private String files;

    @Column(length = 2000)
    private String errorMessage;

    @Builder.Default
    private boolean reused = false;
}

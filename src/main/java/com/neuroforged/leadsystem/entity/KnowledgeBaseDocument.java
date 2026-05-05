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
public class KnowledgeBaseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrape_job_id")
    private ScrapeJob scrapeJob;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer wordCount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fetchedAt;
}

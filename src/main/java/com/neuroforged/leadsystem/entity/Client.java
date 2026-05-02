package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String primaryEmail;
    private String notificationEmails;
    private String websiteUrl;

    @Column(unique = true, updatable = false, nullable = false, length = 64)
    private String apiKey;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastScrapedAt;

    @PrePersist
    private void generateApiKey() {
        if (apiKey == null) {
            apiKey = UUID.randomUUID().toString().replace("-", "");
        }
    }
}

package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CalendlyIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String state;
    private boolean completed;

    /** When the state was issued. States are single-use and short-lived (see
     *  ClientOAuthStateServiceImpl). */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}

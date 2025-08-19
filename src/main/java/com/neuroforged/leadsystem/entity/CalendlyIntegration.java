package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CalendlyIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String state;
    private boolean completed;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}

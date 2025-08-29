package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ClientAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long clientId;
    @Column(nullable = false, length = 64)
    private String questionKey;
    @Lob
    private String value;
    private java.time.Instant updatedAt;
}

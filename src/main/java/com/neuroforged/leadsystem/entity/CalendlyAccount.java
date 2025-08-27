package com.neuroforged.leadsystem.entity;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendlyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accessToken;
    private String refreshToken;
    private String owner;
    private String ownerType;
    private String organization;

        private Instant lastPolledAt;

    @Column(nullable = false)
    private boolean pollEnabled = true;

    @Column(nullable = false)
    private int pollIntervalSeconds = 180;
@Column(unique = true)
    private Long clientId;
}

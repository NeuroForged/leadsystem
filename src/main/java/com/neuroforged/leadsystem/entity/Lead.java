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
@Table(
        name = "lead",
        indexes = {
                @Index(name = "idx_client_id", columnList = "client_id"),
                @Index(name = "idx_lead_score", columnList = "lead_score")
        }
)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

//    @Column(unique = true)
    private String email;

    private String businessName;
    private String businessType;
    private String customerType;
    private String trafficSource;

    private Integer monthlyLeads;

    private Double conversionRate;
    private Double  costPerLead;
    private Double clientValue;

    private Integer leadScore;

    @Column(length = 1000)
    private String leadChallenge;

    private String clientId;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}

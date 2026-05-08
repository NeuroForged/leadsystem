package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

@FilterDef(name = "clientFilter", parameters = @ParamDef(name = "clientId", type = String.class))
@Filter(name = "clientFilter", condition = "client_id_str = :clientId")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "lead",
        indexes = {
                @Index(name = "idx_lead_client_id_str", columnList = "client_id_str"),
                @Index(name = "idx_lead_score",         columnList = "lead_score"),
                @Index(name = "idx_lead_client_id_fk",  columnList = "client_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lead_email_client", columnNames = {"email", "client_id_str"})
        }
)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

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

    /** Legacy string-form of the client ID (kept for backward compat). Use {@link #client} for FK access. */
    @Column(name = "client_id_str")
    private String clientIdStr;

    /** FK to {@link Client} — populated on write from clientIdStr. Nullable for rows predating Phase 1. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(STRING)
    private LeadStatus status;

    @Column(length = 1000)
    private String relevantKbSnippet;

    private String assignedTo;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}

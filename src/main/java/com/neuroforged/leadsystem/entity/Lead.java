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
@Filter(name = "clientFilter", condition = "client_id = :clientId")
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
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lead_email_client", columnNames = {"email", "client_id"})
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

    private String clientId;

    @Enumerated(STRING)
    private LeadStatus status;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}

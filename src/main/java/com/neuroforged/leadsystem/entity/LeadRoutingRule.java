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
@Table(name = "lead_routing_rule", indexes = {
        @Index(name = "idx_routing_rule_client", columnList = "client_id")
})
public class LeadRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    /** Lead field name to match against (e.g. "businessType", "customerType", "trafficSource"). */
    @Column(name = "match_field", nullable = false, length = 100)
    private String matchField;

    /** Value to match (case-insensitive equality). */
    @Column(name = "match_value", nullable = false, length = 255)
    private String matchValue;

    /** Assignee email or name when rule matches. */
    @Column(name = "assign_to", nullable = false, length = 255)
    private String assignTo;

    /** Lower priority number wins first. */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

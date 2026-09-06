package com.neuroforged.leadsystem.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

@Filter(name = "longClientFilter", condition = "client_id = :clientId")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "lead",
        indexes = {
                @Index(name = "idx_lead_score",         columnList = "lead_score"),
                @Index(name = "idx_lead_client_id_fk",  columnList = "client_id")
        },
        // LSB-153: in prod this is a partial unique INDEX on (email, client_id)
        // WHERE client_id IS NOT NULL (defined in Flyway V19). JPA cannot model
        // partial indexes, so this @UniqueConstraint is here primarily so the
        // H2-backed test DB (ddl-auto: create-drop) gets an equivalent constraint.
        // Hibernate `validate` in prod does not enforce unique constraints, so
        // the partial-index semantics from Flyway take precedence.
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

    /** FK to {@link Client} — populated on write. Nullable for legacy rows predating LSB-89. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(STRING)
    private LeadStatus status;

    /** Everything the chatbot captured, verbatim (BUG-3). The typed columns above are
     *  the agency-template projection; vertical templates (dental, real estate, …)
     *  capture fields that have no column, and used to be discarded. */
    @JdbcTypeCode(SqlTypes.JSON)   // jsonb on Postgres; portable on H2 (tests) — no columnDefinition
    @Column(name = "captured_fields")
    private Map<String, String> capturedFields;

    @Column(length = 1000)
    private String relevantKbSnippet;

    private String assignedTo;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * LSB-155: legacy string-form of the client ID. The underlying column was
     * dropped in V20, so this is derived from the {@link #client} FK. Kept as a
     * read-only convenience so existing call sites (notifications, routing,
     * metrics, webhook payloads) don't need a Lombok-level refactor.
     */
    @jakarta.persistence.Transient
    public String getClientIdStr() {
        return client != null ? String.valueOf(client.getId()) : null;
    }
}

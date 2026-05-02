package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.repository.projection.DateCount;
import com.neuroforged.leadsystem.repository.projection.LabelCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByEmailAndClientId(String email, String clientId);

    boolean existsByBusinessName(String businessName);

    Optional<Lead> findByEmail(String email);

    Optional<Lead> findByBusinessName(String businessName);

    List<Lead> findByClientId(String clientId);

    Page<Lead> findByClientId(String clientId, Pageable pageable);

    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);

    Page<Lead> findByClientIdAndStatus(String clientId, LeadStatus status, Pageable pageable);

    Optional<Lead> findByEmailAndClientId(String email, String clientId);

    // ── Analytics aggregation queries ────────────────────────────────────────

    @Query(value = """
            SELECT TO_CHAR(created_at::date, 'YYYY-MM-DD') AS date, CAST(COUNT(*) AS BIGINT) AS count
            FROM lead
            WHERE (:clientId IS NULL OR client_id = :clientId)
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS DATE))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS DATE) + INTERVAL '1 day')
            GROUP BY created_at::date
            ORDER BY created_at::date
            """, nativeQuery = true)
    List<DateCount> findLeadVolumeByDate(
            @Param("clientId") String clientId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);

    @Query(value = """
            SELECT traffic_source AS label, CAST(COUNT(*) AS BIGINT) AS count
            FROM lead
            WHERE traffic_source IS NOT NULL
              AND (:clientId IS NULL OR client_id = :clientId)
            GROUP BY traffic_source
            ORDER BY count DESC
            """, nativeQuery = true)
    List<LabelCount> findCountsByTrafficSource(@Param("clientId") String clientId);

    @Query(value = """
            SELECT business_type AS label, CAST(COUNT(*) AS BIGINT) AS count
            FROM lead
            WHERE business_type IS NOT NULL
              AND (:clientId IS NULL OR client_id = :clientId)
            GROUP BY business_type
            ORDER BY count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<LabelCount> findCountsByBusinessType(@Param("clientId") String clientId);

    @Query(value = """
            SELECT CAST(status AS TEXT) AS label, CAST(COUNT(*) AS BIGINT) AS count
            FROM lead
            WHERE status IS NOT NULL
              AND (:clientId IS NULL OR client_id = :clientId)
            GROUP BY status
            """, nativeQuery = true)
    List<LabelCount> findCountsByStatus(@Param("clientId") String clientId);

    @Query(value = """
            SELECT
                CASE
                    WHEN lead_score >= 80 THEN 'High'
                    WHEN lead_score >= 60 THEN 'Medium'
                    ELSE 'Low'
                END AS label,
                CAST(COUNT(*) AS BIGINT) AS count
            FROM lead
            WHERE lead_score IS NOT NULL
              AND (:clientId IS NULL OR client_id = :clientId)
            GROUP BY label
            ORDER BY MIN(lead_score) DESC
            """, nativeQuery = true)
    List<LabelCount> findCountsByScoreBand(@Param("clientId") String clientId);

    @Query(value = """
            SELECT COUNT(*) FROM lead
            WHERE (:clientId IS NULL OR client_id = :clientId)
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS DATE))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS DATE) + INTERVAL '1 day')
            """, nativeQuery = true)
    long countFiltered(
            @Param("clientId") String clientId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);

    @Query(value = """
            SELECT COALESCE(AVG(lead_score), 0) FROM lead
            WHERE lead_score IS NOT NULL
              AND (:clientId IS NULL OR client_id = :clientId)
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS DATE))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS DATE) + INTERVAL '1 day')
            """, nativeQuery = true)
    double avgLeadScore(
            @Param("clientId") String clientId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);

    @Query(value = """
            SELECT COUNT(*) FROM lead
            WHERE lead_score >= 80
              AND (:clientId IS NULL OR client_id = :clientId)
              AND (:fromDate IS NULL OR created_at >= CAST(:fromDate AS DATE))
              AND (:toDate IS NULL OR created_at <= CAST(:toDate AS DATE) + INTERVAL '1 day')
            """, nativeQuery = true)
    long countHighQuality(
            @Param("clientId") String clientId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);
}

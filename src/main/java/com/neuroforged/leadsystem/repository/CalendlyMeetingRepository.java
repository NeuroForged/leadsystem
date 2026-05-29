package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendlyMeetingRepository extends JpaRepository<CalendlyMeeting, Long> {
    Optional<CalendlyMeeting> findByCalendlyUri(String calendlyUri);
    Page<CalendlyMeeting> findByClient_Id(Long clientId, Pageable pageable);
    Page<CalendlyMeeting> findByStartTimeBetween(ZonedDateTime from, ZonedDateTime to, Pageable pageable);
    Page<CalendlyMeeting> findByClient_IdAndStartTimeBetween(Long clientId, ZonedDateTime from, ZonedDateTime to, Pageable pageable);
    List<CalendlyMeeting> findByInviteeEmail(String email);
    List<CalendlyMeeting> findByClient_Id(Long clientId);

    /**
     * Unified, query-level meeting search with all filters optional. Pushing the invitee filter
     * into SQL (rather than filtering a fetched page in memory) keeps pagination counts correct
     * and matches invitees across all pages. {@code LEFT JOIN FETCH m.client} eliminates the
     * per-row lazy-load of the client association.
     */
    @Query(value = """
            SELECT m FROM CalendlyMeeting m
            LEFT JOIN FETCH m.client
            WHERE (:clientId IS NULL OR m.client.id = :clientId)
              AND (:from IS NULL OR m.startTime >= :from)
              AND (:to IS NULL OR m.startTime < :to)
              AND (:invitee IS NULL OR LOWER(m.inviteeEmail) LIKE LOWER(CONCAT('%', :invitee, '%')))
            """,
            countQuery = """
            SELECT COUNT(m) FROM CalendlyMeeting m
            WHERE (:clientId IS NULL OR m.client.id = :clientId)
              AND (:from IS NULL OR m.startTime >= :from)
              AND (:to IS NULL OR m.startTime < :to)
              AND (:invitee IS NULL OR LOWER(m.inviteeEmail) LIKE LOWER(CONCAT('%', :invitee, '%')))
            """)
    Page<CalendlyMeeting> search(
            @Param("clientId") Long clientId,
            @Param("from") ZonedDateTime from,
            @Param("to") ZonedDateTime to,
            @Param("invitee") String invitee,
            Pageable pageable);

    /** For Fireflies matching: find meetings with matching invitee email in a time window (±10 min). */
    List<CalendlyMeeting> findByInviteeEmailAndStartTimeBetween(
            String inviteeEmail, ZonedDateTime from, ZonedDateTime to);
}

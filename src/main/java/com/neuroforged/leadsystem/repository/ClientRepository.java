package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.projection.ClientSummaryRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByPrimaryEmail(String email);

    @Query(value = """
            SELECT
                c.id                                    AS clientId,
                c.name                                  AS clientName,
                COALESCE(l.total_leads, 0)              AS totalLeads,
                COALESCE(m.total_meetings, 0)           AS totalMeetings,
                COALESCE(l.avg_score, 0.0)              AS avgLeadScore
            FROM client c
            LEFT JOIN (
                SELECT CAST(client_id AS BIGINT) AS cid,
                       COUNT(*)                  AS total_leads,
                       AVG(lead_score)           AS avg_score
                FROM lead
                GROUP BY client_id
            ) l ON l.cid = c.id
            LEFT JOIN (
                SELECT client_id,
                       COUNT(*) AS total_meetings
                FROM calendly_meeting
                GROUP BY client_id
            ) m ON m.client_id = c.id
            ORDER BY c.name
            """, nativeQuery = true)
    List<ClientSummaryRow> fetchClientSummaries();
}

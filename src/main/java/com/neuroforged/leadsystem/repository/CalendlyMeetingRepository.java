package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

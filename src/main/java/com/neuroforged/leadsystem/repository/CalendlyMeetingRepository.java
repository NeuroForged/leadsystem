package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.ZonedDateTime;

public interface CalendlyMeetingRepository extends JpaRepository<CalendlyMeeting, Long> {
    Optional<CalendlyMeeting> findByCalendlyUriAndStartTime(String calendlyUri, ZonedDateTime startTime);
}

package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalendlyMeetingRepository extends JpaRepository<CalendlyMeeting, Long> {
    Optional<CalendlyMeeting> findByCalendlyUri(String calendlyUri);
}

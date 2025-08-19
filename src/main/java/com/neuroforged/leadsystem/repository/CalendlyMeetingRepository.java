package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendlyMeetingRepository extends JpaRepository<CalendlyMeeting, Long> {
}

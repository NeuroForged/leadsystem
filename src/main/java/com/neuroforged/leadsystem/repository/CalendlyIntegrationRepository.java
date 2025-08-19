package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalendlyIntegrationRepository extends JpaRepository<CalendlyIntegration, Long> {
    Optional<CalendlyIntegration> findByState(String state);
}

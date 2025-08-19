package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendlyWebhookLogRepository extends JpaRepository<CalendlyWebhookLog, Long> {
}

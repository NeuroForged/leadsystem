package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendlyWebhookLogRepository extends JpaRepository<CalendlyWebhookLog, Long> {
    List<CalendlyWebhookLog> findBySuccessFalseAndRetryCountLessThan(int maxRetries);
}

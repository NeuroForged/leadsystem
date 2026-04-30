package com.neuroforged.leadsystem.health;

import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.CalendlyWebhookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalendlyHealthIndicator implements HealthIndicator {

    private static final int MAX_RETRIES = 3;

    private final CalendlyAccountRepository accountRepository;
    private final CalendlyWebhookLogRepository webhookLogRepository;

    @Override
    public Health health() {
        long reAuthRequired = accountRepository.countByRequiresReauthTrue();
        long pendingRetries = webhookLogRepository.countBySuccessFalseAndRetryCountLessThan(MAX_RETRIES);

        Health.Builder builder = (reAuthRequired > 0) ? Health.down() : Health.up();

        return builder
                .withDetail("accountsRequiringReauth", reAuthRequired)
                .withDetail("webhooksPendingRetry", pendingRetries)
                .build();
    }
}

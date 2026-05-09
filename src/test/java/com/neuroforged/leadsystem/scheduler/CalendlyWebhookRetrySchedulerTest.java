package com.neuroforged.leadsystem.scheduler;

import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendlyWebhookRetrySchedulerTest {

    @Mock
    private CalendlyWebhookService webhookService;

    @Mock
    private LeadSystemMetrics metrics;

    @Mock
    private BusinessEventLogger eventLogger;

    @InjectMocks
    private CalendlyWebhookRetryScheduler scheduler;

    @Test
    void retryFailedWebhooks_callsWebhookService() {
        when(webhookService.retryFailedWebhooks()).thenReturn(new CalendlyWebhookService.RetryResult(0, 0));

        scheduler.retryFailedWebhooks();

        verify(webhookService).retryFailedWebhooks();
    }

    @Test
    void retryFailedWebhooks_recordsSchedulerMetric() {
        when(webhookService.retryFailedWebhooks()).thenReturn(new CalendlyWebhookService.RetryResult(0, 0));

        scheduler.retryFailedWebhooks();

        verify(metrics).recordSchedulerRun("calendly-webhook-retry");
    }

    @Test
    void retryFailedWebhooks_logsEventWithResults() {
        when(webhookService.retryFailedWebhooks()).thenReturn(new CalendlyWebhookService.RetryResult(2, 1));

        scheduler.retryFailedWebhooks();

        verify(eventLogger).schedulerRun(eq("webhook-retry"), any(Map.class));
    }
}

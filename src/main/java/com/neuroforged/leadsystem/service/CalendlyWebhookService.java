package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;

import java.util.Map;

public interface CalendlyWebhookService {
    void handleWebhook(CalendlyWebhookPayload payload, Map<String, String> headers);
    void retryFailedWebhooks();
}

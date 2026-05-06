package com.neuroforged.leadsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.service.OutboundWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboundWebhookServiceImpl implements OutboundWebhookService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Async
    @Override
    public void notifyWebhook(Lead lead, Client client) {
        String webhookUrl = client.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String payload = buildPayload(lead);
        if (payload == null) {
            log.warn("Webhook skipped for lead {} — payload serialization failed", lead.getId());
            return;
        }

        WebClient webClient = webClientBuilder.build();

        var requestSpec = webClient.post()
                .uri(webhookUrl)
                .header("Content-Type", "application/json");

        String secret = client.getWebhookSecret();
        if (secret != null && !secret.isBlank()) {
            String signature = hmacSha256(payload, secret);
            requestSpec = requestSpec.header("X-Alchemize-Signature", signature);
        }

        try {
            String status = requestSpec
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .map(response -> response.getStatusCode().toString())
                    .block();
            log.info("Webhook delivered to {} for lead {} - status {}", webhookUrl, lead.getId(), status);
        } catch (Exception e) {
            log.warn("Webhook delivery failed to {} for lead {}: {}", webhookUrl, lead.getId(), e.getMessage());
        }
    }

    private String buildPayload(Lead lead) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("leadId", lead.getId());
        payload.put("email", lead.getEmail());
        payload.put("name", lead.getFirstName() == null ? "" : lead.getFirstName());
        payload.put("phone", null);
        payload.put("clientId", lead.getClientId());
        payload.put("createdAt", lead.getCreatedAt() == null ? null : lead.getCreatedAt().toString());
        payload.put("status", lead.getStatus() == null ? null : lead.getStatus().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload for lead {}", lead.getId(), e);
            return null;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 signature", e);
            return "";
        }
    }
}

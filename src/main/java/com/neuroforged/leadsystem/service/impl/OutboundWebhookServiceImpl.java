package com.neuroforged.leadsystem.service.impl;

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

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboundWebhookServiceImpl implements OutboundWebhookService {

    private final WebClient.Builder webClientBuilder;

    @Async
    @Override
    public void notifyWebhook(Lead lead, Client client) {
        String webhookUrl = client.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String payload = buildPayload(lead);

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
        return "{\"leadId\":" + lead.getId()
                + ",\"email\":\"" + lead.getEmail() + "\""
                + ",\"name\":\"" + nullSafe(lead.getFirstName()) + "\""
                + ",\"phone\":null"
                + ",\"clientId\":\"" + lead.getClientId() + "\""
                + ",\"createdAt\":\"" + lead.getCreatedAt() + "\""
                + ",\"status\":\"" + lead.getStatus() + "\""
                + "}";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
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

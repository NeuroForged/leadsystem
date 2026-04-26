package com.neuroforged.leadsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/calendly/webhook")
@RequiredArgsConstructor
@Slf4j
public class CalendlyWebhookController {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    private final CalendlyWebhookService calendlyWebhookService;
    private final ObjectMapper objectMapper;

    @Value("${calendly.webhook-signing-key}")
    private String webhookSigningKey;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawBody,
                                              @RequestHeader Map<String, String> headers) {
        String signatureHeader = headers.get("calendly-webhook-signature");
        if (signatureHeader == null) {
            log.warn("Rejected webhook: missing Calendly-Webhook-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!isSignatureValid(signatureHeader, rawBody)) {
            log.warn("Rejected webhook: invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CalendlyWebhookPayload payload = objectMapper.readValue(rawBody, CalendlyWebhookPayload.class);
            log.info("Handling Calendly webhook: {}", payload.getEvent());
            calendlyWebhookService.handleWebhook(payload, headers);
        } catch (Exception e) {
            log.error("Failed to deserialize or handle Calendly webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok().build();
    }

    private boolean isSignatureValid(String signatureHeader, String rawBody) {
        try {
            String timestamp = null;
            String receivedHmac = null;

            for (String part : signatureHeader.split(",")) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    receivedHmac = part.substring(3);
                }
            }

            if (timestamp == null || receivedHmac == null) {
                log.warn("Malformed Calendly-Webhook-Signature header: {}", signatureHeader);
                return false;
            }

            long webhookTime = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - webhookTime) > TIMESTAMP_TOLERANCE_SECONDS) {
                log.warn("Rejected webhook: timestamp too old or too far in future (t={})", timestamp);
                return false;
            }

            String dataToSign = timestamp + "." + rawBody;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSigningKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String computedHmac = HexFormat.of().formatHex(computed);

            return computedHmac.equals(receivedHmac);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
}

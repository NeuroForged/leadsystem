package com.neuroforged.leadsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.FirefliesWebhookPayload;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.MeetingNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/fireflies/webhook")
@RequiredArgsConstructor
@Slf4j
public class FirefliesWebhookController {

    private static final String SIGNATURE_HEADER = "X-Fireflies-Webhook-Secret";

    private final MeetingNoteService meetingNoteService;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/{clientId}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable Long clientId,
            @RequestBody String rawBody,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String providedSecret) {

        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            log.warn("[FIREFLIES] Unknown clientId={}", clientId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Client client = clientOpt.get();
        String configuredSecret = client.getFirefliesWebhookSecret();

        if (configuredSecret == null || configuredSecret.isBlank()) {
            log.warn("[FIREFLIES] No webhook secret configured for clientId={} — rejecting", clientId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!configuredSecret.equals(providedSecret)) {
            log.warn("[FIREFLIES] Invalid secret for clientId={}", clientId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            FirefliesWebhookPayload payload = objectMapper.readValue(rawBody, FirefliesWebhookPayload.class);
            log.info("[FIREFLIES] Received event={} meetingId={} clientId={}",
                    payload.getType(), payload.getMeetingId(), clientId);
            meetingNoteService.processWebhook(clientId, payload);
        } catch (Exception e) {
            log.error("[FIREFLIES] Failed to process webhook for clientId={}", clientId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.ok().build();
    }
}

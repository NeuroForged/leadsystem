package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.CalendlyWebhookPayload;
import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/calendly/webhook")
@RequiredArgsConstructor
@Slf4j
public class CalendlyWebhookController {

    private final CalendlyWebhookService calendlyWebhookService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody CalendlyWebhookPayload payload,
                                              @RequestHeader Map<String, String> headers) {
        log.info("Handling Calendly Webhook Request");
        calendlyWebhookService.handleWebhook(payload, headers);
        return ResponseEntity.ok().build();
    }
}

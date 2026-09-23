package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.service.CalendlyTokenConnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/calendly")
@RequiredArgsConstructor
public class CalendlyTokenController {

    private final CalendlyTokenConnectService connectService;

    /** Connect a client's Calendly with a personal access token (admin only). */
    @PostMapping("/{clientId}/token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> connect(@PathVariable Long clientId, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(connectService.connect(clientId, body.get("token")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
        }
    }
}

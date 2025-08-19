package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.CalendlyOAuthRequest;
import com.neuroforged.leadsystem.service.CalendlyAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/calendly/oauth/callback")
public class CalendlyOAuthCallbackController {

    private final CalendlyAuthService authService;

    public CalendlyOAuthCallbackController(CalendlyAuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<String> handleCallback(@RequestParam String code, @RequestParam String state) {
        log.info("Handling callback for Calendly OAuth");
        CalendlyOAuthRequest request = new CalendlyOAuthRequest(code, state);
        try {
            authService.handleOAuthCallback(request);
        } catch (Exception e ) {
            return ResponseEntity.internalServerError().body("Error handling Calendly OAuth URL");
        }
        return ResponseEntity.ok("Calendly OAuth successful. You can close this window.");
    }
}

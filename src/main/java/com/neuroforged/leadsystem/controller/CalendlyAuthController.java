package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.CalendlyAuthRequest;
import com.neuroforged.leadsystem.dto.CalendlyAuthResponse;
import com.neuroforged.leadsystem.service.CalendlyAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendly")
@RequiredArgsConstructor
@Slf4j
public class CalendlyAuthController {

    private final CalendlyAuthService calendlyAuthService;

    @PostMapping("/authorize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalendlyAuthResponse> authorize(@RequestBody CalendlyAuthRequest request) {
        log.info("Reached Controller. Attempting to Authentication URL for {}.", request.getClientId());
        return ResponseEntity.ok(calendlyAuthService.generateAuthorizationUrl(request.getClientId()));
    }
}
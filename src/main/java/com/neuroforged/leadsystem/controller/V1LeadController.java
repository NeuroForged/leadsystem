package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.config.ApiTokenFilter;
import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versioned chatbot-facing lead submission endpoint.
 * Admin/portal endpoints remain unversioned at /api/leads.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class V1LeadController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<LeadResponseDTO> createLead(
            @Valid @RequestBody LeadRequestDTO leadRequestDTO,
            HttpServletRequest request) {
        leadRequestDTO.sanitize();
        Long apiKeyClientId = (Long) request.getAttribute(ApiTokenFilter.API_KEY_CLIENT_ID_ATTR);
        if (apiKeyClientId != null && !String.valueOf(apiKeyClientId).equals(leadRequestDTO.getClientId())) {
            throw new InvalidLeadException("clientId in request does not match the authenticated API key's client.");
        }
        log.info("[v1] Received lead creation request for email: {}", leadRequestDTO.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(leadRequestDTO));
    }
}

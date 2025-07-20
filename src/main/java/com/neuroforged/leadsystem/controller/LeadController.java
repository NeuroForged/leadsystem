package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    // Accessible by Voiceflow bot or Postman with X-Api-Key
    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<LeadResponseDTO> createLead(@Valid @RequestBody LeadRequestDTO leadRequestDTO) {
        leadRequestDTO.sanitize();

        log.info("Received lead creation request for email: {}", leadRequestDTO.getEmail());

        LeadResponseDTO saved = leadService.createLead(leadRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Admins only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeadResponseDTO>> getAllLeads() {
        log.info("Fetching all leads");
        List<LeadResponseDTO> leads = leadService.getAllLeads();

        if (leads.isEmpty()) {
            log.warn("No leads found in the database");
        }

        return ResponseEntity.ok(leads);
    }

    // View leads by clientId — for dashboard use
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LeadResponseDTO>> getLeadsByClientId(@PathVariable String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidLeadException("Client ID must be provided.");
        }

        log.info("Fetching leads for clientId: {}", clientId);
        return ResponseEntity.ok(leadService.getLeadsByClientId(clientId));
    }

    // Get a specific lead
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LeadResponseDTO> getLeadById(@PathVariable Long id) {
        log.info("Fetching lead by ID: {}", id);
        return ResponseEntity.ok(leadService.getLeadById(id));
    }
}

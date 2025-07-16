package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    // Accessible by Voiceflow bot or Postman with X-Api-Key
    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<LeadResponseDTO> createLead(@RequestBody LeadRequestDTO leadRequestDTO) {
        LeadResponseDTO saved = leadService.createLead(leadRequestDTO);
        return ResponseEntity.ok(saved);
    }

    // Admins only (JWT token must have ADMIN role)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeadResponseDTO>> getAllLeads() {
        return ResponseEntity.ok(leadService.getAllLeads());
    }

    // View leads by clientId — for dashboard use
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LeadResponseDTO>> getLeadsByClientId(@PathVariable String clientId) {
        return ResponseEntity.ok(leadService.getLeadsByClientId(clientId));
    }

    // Get a specific lead
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LeadResponseDTO> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }
}

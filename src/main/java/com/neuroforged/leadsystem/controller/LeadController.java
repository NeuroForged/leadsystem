package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.LeadStatusUpdateRequest;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import com.neuroforged.leadsystem.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<LeadResponseDTO> createLead(@Valid @RequestBody LeadRequestDTO leadRequestDTO) {
        leadRequestDTO.sanitize();
        log.info("Received lead creation request for email: {}", leadRequestDTO.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(leadRequestDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<PagedResponse<LeadResponseDTO>> getLeads(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String resolvedClientId = AuthPrincipalUtil.resolveStringClientIdForCaller(clientId);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        log.info("Fetching leads — clientId={}, status={}, page={}, size={}", resolvedClientId, status, page, size);
        return ResponseEntity.ok(leadService.getLeads(resolvedClientId, status, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeadResponseDTO> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody LeadStatusUpdateRequest request) {
        log.info("Updating status for lead ID: {} to {}", id, request.getStatus());
        return ResponseEntity.ok(leadService.updateLeadStatus(id, request.getStatus()));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<?> getLeadsByClientId(@PathVariable String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidLeadException("Client ID must be provided.");
        }
        AuthPrincipalUtil.assertCanAccessStringClient(clientId);
        log.info("Fetching leads for clientId: {}", clientId);
        return ResponseEntity.ok(leadService.getLeadsByClientId(clientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<LeadResponseDTO> getLeadById(@PathVariable Long id) {
        log.info("Fetching lead by ID: {}", id);
        LeadResponseDTO lead = leadService.getLeadById(id);
        AuthPrincipalUtil.assertCanAccessStringClient(lead.getClientId());
        return ResponseEntity.ok(lead);
    }
}

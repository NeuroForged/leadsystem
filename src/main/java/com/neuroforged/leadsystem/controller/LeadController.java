package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.config.ApiTokenFilter;
import com.neuroforged.leadsystem.dto.AddCommentRequest;
import com.neuroforged.leadsystem.dto.LeadCommentDto;
import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.dto.LeadStatusUpdateRequest;
import com.neuroforged.leadsystem.dto.PagedResponse;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.exception.InvalidLeadException;
import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import com.neuroforged.leadsystem.service.LeadCommentService;
import com.neuroforged.leadsystem.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "leadScore", "email", "status", "businessName", "customerType");

    private final LeadService leadService;
    private final LeadCommentService leadCommentService;

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
        log.info("Received lead creation request for email: {}", leadRequestDTO.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(leadRequestDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<PagedResponse<LeadResponseDTO>> getLeads(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Sort resolvedSort = parseSort(sort);
        Long resolvedClientId = AuthPrincipalUtil.resolveClientIdForCaller(clientId);
        Pageable pageable = PageRequest.of(page, size, resolvedSort);
        log.info("Fetching leads -- clientId={}, status={}, search={}, from={}, to={}, page={}, size={}, sort={}",
                resolvedClientId, status, search, from, to, page, size, sort);
        return ResponseEntity.ok(leadService.getLeads(resolvedClientId, status, search, from, to, pageable));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by("createdAt").descending();
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir   = parts.length > 1 ? parts[1].trim() : "desc";
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new InvalidLeadException("Invalid sort field '" + field + "'. Allowed: " + ALLOWED_SORT_FIELDS);
        }
        return dir.equalsIgnoreCase("asc") ? Sort.by(field).ascending() : Sort.by(field).descending();
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
    public ResponseEntity<?> getLeadsByClientId(@PathVariable Long clientId) {
        AuthPrincipalUtil.assertCanAccessClient(clientId);
        log.info("Fetching leads for clientId: {}", clientId);
        return ResponseEntity.ok(leadService.getLeadsByClientId(clientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<LeadResponseDTO> getLeadById(@PathVariable Long id) {
        log.info("Fetching lead by ID: {}", id);
        LeadResponseDTO lead = leadService.getLeadById(id);
        assertCanAccessLead(lead);
        return ResponseEntity.ok(lead);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<LeadCommentDto>> getComments(@PathVariable Long id) {
        LeadResponseDTO lead = leadService.getLeadById(id);
        assertCanAccessLead(lead);
        return ResponseEntity.ok(leadCommentService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<LeadCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        LeadResponseDTO lead = leadService.getLeadById(id);
        assertCanAccessLead(lead);
        String email = AuthPrincipalUtil.currentEmail();
        String role = AuthPrincipalUtil.currentRole();
        log.info("Adding comment to lead {} by {}", id, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadCommentService.addComment(id, request.getContent(), email, role));
    }

    /** Resolves the lead's clientId (stored as String for DTO compat) to Long and asserts access. */
    private void assertCanAccessLead(LeadResponseDTO lead) {
        if (lead.getClientId() != null) {
            AuthPrincipalUtil.assertCanAccessClient(Long.parseLong(lead.getClientId()));
        }
    }
}

package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.entity.AuditEvent;
import com.neuroforged.leadsystem.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping
    public Page<AuditEvent> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    @GetMapping("/by-actor")
    public Page<AuditEvent> byActor(@RequestParam String email,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditEventRepository.findByActorEmail(email, pageable);
    }

    @GetMapping("/by-entity")
    public Page<AuditEvent> byEntity(@RequestParam String type, @RequestParam String id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditEventRepository.findByEntityTypeAndEntityId(type, id, pageable);
    }
}

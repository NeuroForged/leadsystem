package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.KbDocumentDto;
import com.neuroforged.leadsystem.dto.KbFetchJobStatus;
import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import com.neuroforged.leadsystem.service.KbFetchStatusStore;
import com.neuroforged.leadsystem.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients/{clientId}/kb")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbFetchStatusStore kbFetchStatusStore;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<KbDocumentDto>> list(@PathVariable Long clientId) {
        AuthPrincipalUtil.assertCanAccessClient(clientId);
        return ResponseEntity.ok(knowledgeBaseService.listByClient(clientId));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KbFetchJobStatus> fetch(@PathVariable Long clientId) {
        String jobId = UUID.randomUUID().toString();
        log.info("Starting async KB fetch for clientId={}, jobId={}", clientId, jobId);
        kbFetchStatusStore.put(jobId, KbFetchJobStatus.pending(jobId));
        knowledgeBaseService.fetchAsync(clientId, jobId);
        return ResponseEntity.accepted().body(KbFetchJobStatus.pending(jobId));
    }

    @GetMapping("/fetch/status/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KbFetchJobStatus> fetchStatus(
            @PathVariable Long clientId,
            @PathVariable String jobId) {
        KbFetchJobStatus status = kbFetchStatusStore.get(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<KbDocumentDto>> search(
            @PathVariable Long clientId,
            @RequestParam String q) {
        AuthPrincipalUtil.assertCanAccessClient(clientId);
        return ResponseEntity.ok(knowledgeBaseService.search(clientId, q));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clear(@PathVariable Long clientId) {
        knowledgeBaseService.clearByClient(clientId);
        return ResponseEntity.noContent().build();
    }
}

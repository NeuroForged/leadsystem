package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.KbDocumentDto;
import com.neuroforged.leadsystem.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{clientId}/kb")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KbDocumentDto>> list(@PathVariable Long clientId) {
        return ResponseEntity.ok(knowledgeBaseService.listByClient(clientId));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KbDocumentDto>> fetch(@PathVariable Long clientId) {
        log.info("Fetching KB for clientId={}", clientId);
        return ResponseEntity.ok(knowledgeBaseService.fetchAndStore(clientId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KbDocumentDto>> search(
            @PathVariable Long clientId,
            @RequestParam String q) {
        return ResponseEntity.ok(knowledgeBaseService.search(clientId, q));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clear(@PathVariable Long clientId) {
        knowledgeBaseService.clearByClient(clientId);
        return ResponseEntity.noContent().build();
    }
}

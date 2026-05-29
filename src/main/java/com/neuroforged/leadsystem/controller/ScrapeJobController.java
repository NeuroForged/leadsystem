package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;
import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import com.neuroforged.leadsystem.service.ScrapeJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scrape-jobs")
@RequiredArgsConstructor
public class ScrapeJobController {

    private final ScrapeJobService scrapeJobService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<ScrapeJobDto>> listByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(scrapeJobService.listByClient(
                AuthPrincipalUtil.resolveClientIdForCaller(clientId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<ScrapeJobDto> getJob(@PathVariable Long id) {
        ScrapeJobDto job = scrapeJobService.getJob(id);
        AuthPrincipalUtil.assertCanAccessClient(job.getClientId());
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeJobDto> syncJob(@PathVariable Long id) {
        return ResponseEntity.ok(scrapeJobService.syncStatus(id));
    }

    /**
     * PORTAL-315: proxies the KB zip download through the backend so the scraper
     * API key stays server-side instead of shipping in the browser bundle. Tenant
     * access is enforced against the job's owning client before streaming.
     */
    @GetMapping("/{id}/zip")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<byte[]> downloadZip(@PathVariable Long id) {
        ScrapeJobService.ScrapeJobZip zip = scrapeJobService.downloadZip(id);
        AuthPrincipalUtil.assertCanAccessClient(zip.clientId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("knowledge-" + id + ".zip")
                                .build().toString())
                .body(zip.data());
    }
}

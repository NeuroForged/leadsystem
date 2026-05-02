package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;
import com.neuroforged.leadsystem.service.ScrapeJobService;
import lombok.RequiredArgsConstructor;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScrapeJobDto>> listByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(scrapeJobService.listByClient(clientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeJobDto> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(scrapeJobService.getJob(id));
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeJobDto> syncJob(@PathVariable Long id) {
        return ResponseEntity.ok(scrapeJobService.syncStatus(id));
    }
}

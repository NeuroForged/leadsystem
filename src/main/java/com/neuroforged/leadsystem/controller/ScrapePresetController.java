package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ScrapePresetDto;
import com.neuroforged.leadsystem.service.ScrapePresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scrape-presets")
@RequiredArgsConstructor
public class ScrapePresetController {

    private final ScrapePresetService scrapePresetService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScrapePresetDto>> listByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(scrapePresetService.listByClient(clientId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapePresetDto> create(@Valid @RequestBody ScrapePresetDto dto) {
        return ResponseEntity.ok(scrapePresetService.create(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scrapePresetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

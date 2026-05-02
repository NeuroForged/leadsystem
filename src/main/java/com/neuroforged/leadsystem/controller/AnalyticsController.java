package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.*;
import com.neuroforged.leadsystem.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/clients/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientSummaryDTO>> getClientSummary() {
        return ResponseEntity.ok(analyticsService.getClientSummary());
    }

    @GetMapping("/leads/kpis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeadKpiDTO> getLeadKpis(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(analyticsService.getLeadKpis(clientId, from, to));
    }

    @GetMapping("/leads/volume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeadVolumeDTO>> getLeadVolume(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(analyticsService.getLeadVolume(clientId, from, to));
    }

    @GetMapping("/leads/by-traffic-source")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupCountDTO>> getLeadsByTrafficSource(@RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(analyticsService.getLeadsByTrafficSource(clientId));
    }

    @GetMapping("/leads/by-score-band")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupCountDTO>> getLeadsByScoreBand(@RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(analyticsService.getLeadsByScoreBand(clientId));
    }

    @GetMapping("/leads/by-business-type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupCountDTO>> getLeadsByBusinessType(@RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(analyticsService.getLeadsByBusinessType(clientId));
    }

    @GetMapping("/leads/pipeline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupCountDTO>> getLeadsByPipelineStatus(@RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(analyticsService.getLeadsByPipelineStatus(clientId));
    }

    @GetMapping("/leads/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopLeadDTO>> getTopLeads(
            @RequestParam(required = false) Long clientId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopLeads(clientId, limit));
    }
}

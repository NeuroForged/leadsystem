package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.dto.ScrapeJobDto;
import com.neuroforged.leadsystem.service.ClientService;
import com.neuroforged.leadsystem.service.ScrapeJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;
    private final ScrapeJobService scrapeJobService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientDto>> listClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDto> createClient(@Valid @RequestBody ClientDto clientDto) {
        log.info("Controller for creating new client (Admin Only)");
        return ResponseEntity.ok(clientService.createClient(clientDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDto> getClient(@PathVariable Long id) {
        log.info("Controller for getting client by ID (Admin Only)");
        return ResponseEntity.ok(clientService.getClientDtoById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDto> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDto clientDto) {
        log.info("Controller for updating client ID: {} (Admin Only)", id);
        return ResponseEntity.ok(clientService.updateClient(id, clientDto));
    }

    @PostMapping("/{id}/scrape")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeJobDto> triggerScrape(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Triggering scrape for client ID: {}", id);
        ClientDto client = clientService.getClientDtoById(id);
        String initiatedBy = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(scrapeJobService.createJob(id, client.getWebsiteUrl(), 1000, initiatedBy));
    }

    @PatchMapping("/{id}/scrape-timestamp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateScrapeTimestamp(@PathVariable Long id) {
        clientService.updateScrapeTimestamp(id);
        return ResponseEntity.noContent().build();
    }
}

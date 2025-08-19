package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDto> createClient(@RequestBody ClientDto clientDto) {
        log.info("Controller for creating new client (Admin Only)");
        return ResponseEntity.ok(clientService.createClient(clientDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDto> getClient(@PathVariable Long id) {
        log.info("Controller for getting client by ID (Admin Only)");
        return ResponseEntity.ok(clientService.getClientDtoById(id));
    }
}

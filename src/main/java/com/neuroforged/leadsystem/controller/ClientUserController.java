package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ClientUserDto;
import com.neuroforged.leadsystem.dto.CreateClientUserRequest;
import com.neuroforged.leadsystem.service.ClientUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/clients/{clientId}/users")
@RequiredArgsConstructor
public class ClientUserController {

    private final ClientUserService clientUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientUserDto> createClientUser(
            @PathVariable Long clientId,
            @Valid @RequestBody CreateClientUserRequest request) {
        log.info("Creating CLIENT user for client {}", clientId);
        return ResponseEntity.ok(clientUserService.createClientUser(clientId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientUserDto>> listClientUsers(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientUserService.listClientUsers(clientId));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClientUser(
            @PathVariable Long clientId,
            @PathVariable Long userId) {
        clientUserService.deleteClientUser(clientId, userId);
        return ResponseEntity.noContent().build();
    }
}

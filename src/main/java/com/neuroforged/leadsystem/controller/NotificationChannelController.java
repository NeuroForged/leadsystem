package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.CreateNotificationChannelRequest;
import com.neuroforged.leadsystem.dto.NotificationChannelDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.NotificationChannel;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.NotificationChannelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{clientId}/notification-channels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationChannelController {

    private final NotificationChannelRepository channelRepository;
    private final ClientRepository clientRepository;

    @GetMapping
    public List<NotificationChannelDto> list(@PathVariable Long clientId) {
        return channelRepository.findByClientId(clientId).stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationChannelDto create(@PathVariable Long clientId,
                                         @Valid @RequestBody CreateNotificationChannelRequest req) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        NotificationChannel channel = NotificationChannel.builder()
                .client(client)
                .channelType(req.getChannelType())
                .destination(req.getDestination())
                .events(req.getEvents())
                .build();

        return toDto(channelRepository.save(channel));
    }

    @DeleteMapping("/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long clientId, @PathVariable Long channelId) {
        NotificationChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification channel not found: " + channelId));
        if (!channel.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Notification channel not found: " + channelId);
        }
        channelRepository.delete(channel);
    }

    private NotificationChannelDto toDto(NotificationChannel nc) {
        NotificationChannelDto dto = new NotificationChannelDto();
        dto.setId(nc.getId());
        dto.setClientId(nc.getClient().getId());
        dto.setChannelType(nc.getChannelType());
        dto.setDestination(nc.getDestination());
        dto.setEvents(nc.getEvents());
        dto.setCreatedAt(nc.getCreatedAt());
        return dto;
    }
}

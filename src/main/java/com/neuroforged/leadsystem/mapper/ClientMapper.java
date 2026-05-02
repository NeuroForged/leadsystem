package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientMapper {

    private final CalendlyAccountRepository calendlyAccountRepository;

    public ClientDto toDto(Client client) {
        if (client == null) return null;
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setPrimaryEmail(client.getPrimaryEmail());
        dto.setWebsiteUrl(client.getWebsiteUrl());
        dto.setApiKey(client.getApiKey());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        dto.setLastScrapedAt(client.getLastScrapedAt());

        String emails = client.getNotificationEmails();
        dto.setNotificationEmails(emails == null || emails.isBlank()
                ? List.of()
                : Arrays.asList(emails.split(",\\s*")));

        dto.setCalendlyConnected(calendlyAccountRepository.findByClientId(client.getId()).isPresent());

        return dto;
    }

    public Client toEntity(ClientDto dto) {
        if (dto == null) return null;
        Client client = new Client();
        client.setId(dto.getId());
        client.setName(dto.getName());
        client.setPrimaryEmail(dto.getPrimaryEmail());
        client.setWebsiteUrl(dto.getWebsiteUrl());

        List<String> emails = dto.getNotificationEmails();
        client.setNotificationEmails(emails == null || emails.isEmpty()
                ? null
                : String.join(",", emails));

        return client;
    }
}

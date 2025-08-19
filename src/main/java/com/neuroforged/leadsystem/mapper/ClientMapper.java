package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientMapper {

    public ClientDto toDto(Client client) {
        log.info("Converting from Client to dto");
        if (client == null) return null;
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setPrimaryEmail(client.getPrimaryEmail());
        return dto;
    }

    public Client toEntity(ClientDto dto) {
        log.info("Converting from dto to Client");
        if (dto == null) return null;
        Client client = new Client();
        client.setId(dto.getId());
        client.setName(dto.getName());
        client.setPrimaryEmail(dto.getPrimaryEmail());
        return client;
    }
}

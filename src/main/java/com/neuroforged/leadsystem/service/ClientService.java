package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;

import java.util.Optional;

public interface ClientService {
    ClientDto createClient(ClientDto dto);
    ClientDto getClientDtoById(Long clientId);
    Optional<Client> getClientById(Long clientId);
    Optional<Client> findClientByEmail(String email);
}

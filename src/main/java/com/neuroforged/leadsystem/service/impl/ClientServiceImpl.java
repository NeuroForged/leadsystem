package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.mapper.ClientMapper;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    public ClientDto createClient(ClientDto dto) {
        Client client = clientMapper.toEntity(dto);
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    @Override
    public ClientDto getClientDtoById(Long clientId) {
        return clientRepository.findById(clientId)
                .map(clientMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
    }

    @Override
    public ClientDto updateClient(Long clientId, ClientDto dto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
        client.setName(dto.getName());
        client.setPrimaryEmail(dto.getPrimaryEmail());
        client.setNotificationEmails(dto.getNotificationEmails());
        client.setWebsiteUrl(dto.getWebsiteUrl());
        return clientMapper.toDto(clientRepository.save(client));
    }

    @Override
    public Optional<Client> getClientById(Long clientId) {
        return clientRepository.findById(clientId);
    }

    @Override
    public Optional<Client> findClientByEmail(String email) {
        return clientRepository.findByPrimaryEmail(email);
    }

    @Override
    public List<ClientDto> getAllClients() {
        return clientRepository.findAll().stream().map(clientMapper::toDto).toList();
    }

    @Override
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) throw new ResourceNotFoundException("Client not found: " + id);
        clientRepository.deleteById(id);
    }

    @Override
    public void updateScrapeTimestamp(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        client.setLastScrapedAt(LocalDateTime.now());
        clientRepository.save(client);
    }
}

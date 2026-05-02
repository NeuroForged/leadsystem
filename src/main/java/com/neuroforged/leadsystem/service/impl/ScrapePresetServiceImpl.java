package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.ScrapePresetDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.ScrapePreset;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.ScrapePresetRepository;
import com.neuroforged.leadsystem.service.ScrapePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapePresetServiceImpl implements ScrapePresetService {

    private final ScrapePresetRepository scrapePresetRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ScrapePresetDto create(ScrapePresetDto dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + dto.getClientId()));

        ScrapePreset preset = ScrapePreset.builder()
                .client(client)
                .name(dto.getName())
                .url(dto.getUrl())
                .maxPages(dto.getMaxPages() != null ? dto.getMaxPages() : 1000)
                .build();

        return toDto(scrapePresetRepository.save(preset));
    }

    @Override
    public List<ScrapePresetDto> listByClient(Long clientId) {
        return scrapePresetRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!scrapePresetRepository.existsById(id)) {
            throw new ResourceNotFoundException("ScrapePreset not found: " + id);
        }
        scrapePresetRepository.deleteById(id);
    }

    private ScrapePresetDto toDto(ScrapePreset p) {
        return ScrapePresetDto.builder()
                .id(p.getId())
                .clientId(p.getClient() != null ? p.getClient().getId() : null)
                .name(p.getName())
                .url(p.getUrl())
                .maxPages(p.getMaxPages())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

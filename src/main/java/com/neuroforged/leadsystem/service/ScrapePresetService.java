package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ScrapePresetDto;

import java.util.List;

public interface ScrapePresetService {
    ScrapePresetDto create(ScrapePresetDto dto);
    List<ScrapePresetDto> listByClient(Long clientId);
    void delete(Long id);
}

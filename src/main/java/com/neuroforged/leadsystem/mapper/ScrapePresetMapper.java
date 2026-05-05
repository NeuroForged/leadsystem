package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.ScrapePresetDto;
import com.neuroforged.leadsystem.entity.ScrapePreset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScrapePresetMapper {

    @Mapping(source = "client.id", target = "clientId")
    ScrapePresetDto toDto(ScrapePreset preset);
}

package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.ScrapeJobDto;
import com.neuroforged.leadsystem.entity.ScrapeJob;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScrapeJobMapper {

    @Mapping(source = "client.id", target = "clientId")
    ScrapeJobDto toDto(ScrapeJob job);
}

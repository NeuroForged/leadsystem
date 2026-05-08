package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(source = "clientIdStr", target = "clientId")
    LeadResponseDTO toDto(Lead lead);
}

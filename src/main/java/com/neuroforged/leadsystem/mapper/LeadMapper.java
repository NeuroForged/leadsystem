package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    // LSB-155: derive LeadResponseDTO.clientId (String) from the FK directly,
    // since the legacy client_id_str column has been dropped.
    @Mapping(
            target = "clientId",
            expression = "java(lead.getClient() != null ? String.valueOf(lead.getClient().getId()) : null)"
    )
    LeadResponseDTO toDto(Lead lead);
}

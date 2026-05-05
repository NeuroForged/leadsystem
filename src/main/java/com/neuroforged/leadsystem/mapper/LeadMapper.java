package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.LeadResponseDTO;
import com.neuroforged.leadsystem.entity.Lead;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    LeadResponseDTO toDto(Lead lead);
}

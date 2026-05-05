package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.KbDocumentDto;
import com.neuroforged.leadsystem.entity.KnowledgeBaseDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KbDocumentMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "scrapeJob.id", target = "scrapeJobId")
    KbDocumentDto toDto(KnowledgeBaseDocument doc);
}

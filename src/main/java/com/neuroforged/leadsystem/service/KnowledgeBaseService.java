package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.KbDocumentDto;

import java.util.List;

public interface KnowledgeBaseService {
    List<KbDocumentDto> fetchAndStore(Long clientId);
    List<KbDocumentDto> listByClient(Long clientId);
    List<KbDocumentDto> search(Long clientId, String q);
    void clearByClient(Long clientId);
}

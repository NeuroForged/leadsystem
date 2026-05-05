package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.KbDocumentDto;

import java.util.List;

public interface KnowledgeBaseService {
    List<KbDocumentDto> fetchAndStore(Long clientId);
    void fetchAsync(Long clientId, String jobId);
    List<KbDocumentDto> listByClient(Long clientId);
    List<KbDocumentDto> search(Long clientId, String q);
    void clearByClient(Long clientId);
}

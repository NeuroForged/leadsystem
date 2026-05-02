package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.KbDocumentDto;
import com.neuroforged.leadsystem.entity.KnowledgeBaseDocument;
import com.neuroforged.leadsystem.entity.ScrapeJob;
import com.neuroforged.leadsystem.entity.ScrapeJobStatus;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.KnowledgeBaseDocumentRepository;
import com.neuroforged.leadsystem.repository.ScrapeJobRepository;
import com.neuroforged.leadsystem.service.KnowledgeBaseService;
import com.neuroforged.leadsystem.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseDocumentRepository kbRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ClientRepository clientRepository;
    private final ScraperService scraperService;

    @Override
    @Transactional
    public List<KbDocumentDto> fetchAndStore(Long clientId) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        ScrapeJob latestJob = scrapeJobRepository
                .findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .filter(j -> j.getStatus() == ScrapeJobStatus.DONE && j.getScraperJobId() != null)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No completed scrape job found for client " + clientId));

        byte[] zipBytes = scraperService.downloadZip(latestJob.getScraperJobId());
        if (zipBytes == null || zipBytes.length == 0) {
            throw new ResourceNotFoundException("Scraper returned empty ZIP for job " + latestJob.getScraperJobId());
        }

        kbRepository.deleteByClientId(clientId);

        List<KnowledgeBaseDocument> docs = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".md")) {
                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    int wordCount = content.isBlank() ? 0
                            : Arrays.stream(content.trim().split("\\s+")).mapToInt(w -> 1).sum();

                    docs.add(KnowledgeBaseDocument.builder()
                            .client(client)
                            .scrapeJob(latestJob)
                            .filename(name)
                            .content(content)
                            .wordCount(wordCount)
                            .build());
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.error("Failed to extract KB zip for client {}: {}", clientId, e.getMessage());
            throw new RuntimeException("Failed to extract KB zip: " + e.getMessage(), e);
        }

        kbRepository.saveAll(docs);
        log.info("Stored {} KB documents for clientId={}", docs.size(), clientId);
        return docs.stream().map(this::toDto).toList();
    }

    @Override
    public List<KbDocumentDto> listByClient(Long clientId) {
        return kbRepository.findByClientIdOrderByFilenameAsc(clientId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<KbDocumentDto> search(Long clientId, String q) {
        if (q == null || q.isBlank()) return listByClient(clientId);
        return kbRepository.searchByClientId(clientId, q)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void clearByClient(Long clientId) {
        kbRepository.deleteByClientId(clientId);
    }

    private KbDocumentDto toDto(KnowledgeBaseDocument doc) {
        return KbDocumentDto.builder()
                .id(doc.getId())
                .clientId(doc.getClient() != null ? doc.getClient().getId() : null)
                .scrapeJobId(doc.getScrapeJob() != null ? doc.getScrapeJob().getId() : null)
                .filename(doc.getFilename())
                .content(doc.getContent())
                .wordCount(doc.getWordCount())
                .fetchedAt(doc.getFetchedAt())
                .build();
    }
}

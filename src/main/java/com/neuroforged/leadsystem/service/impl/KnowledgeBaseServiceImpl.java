package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.KbDocumentDto;
import com.neuroforged.leadsystem.dto.KbFetchJobStatus;
import com.neuroforged.leadsystem.entity.KnowledgeBaseDocument;
import com.neuroforged.leadsystem.entity.ScrapeJob;
import com.neuroforged.leadsystem.entity.ScrapeJobStatus;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.mapper.KbDocumentMapper;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.KnowledgeBaseDocumentRepository;
import com.neuroforged.leadsystem.repository.ScrapeJobRepository;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.service.KbFetchStatusStore;
import com.neuroforged.leadsystem.service.KnowledgeBaseService;
import com.neuroforged.leadsystem.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    // Zip-bomb guards for the scraper KB archive. The source is internal/trusted,
    // but reading via readAllBytes() with no cap means a compromised/buggy scraper
    // response could exhaust memory. Decompress through a bounded copy instead.
    private static final int MAX_ZIP_ENTRIES = 5_000;
    private static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;        // 10 MB per .md doc
    private static final long MAX_TOTAL_INFLATED_BYTES = 200L * 1024 * 1024; // 200 MB total

    private final KnowledgeBaseDocumentRepository kbRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ClientRepository clientRepository;
    private final ScraperService scraperService;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbFetchStatusStore statusStore;
    private final LeadSystemMetrics metrics;

    @Override
    @Transactional
    public List<KbDocumentDto> fetchAndStore(Long clientId) {
        io.micrometer.core.instrument.Timer.Sample sample = metrics.startKbFetchTimer();
        boolean success = false;
        try {
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
                int entryCount = 0;
                long totalInflated = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    if (++entryCount > MAX_ZIP_ENTRIES) {
                        throw new IllegalStateException("KB zip exceeds max entry count " + MAX_ZIP_ENTRIES);
                    }
                    String name = entry.getName();
                    if (name.endsWith(".md")) {
                        byte[] bytes = readEntryBounded(zis);
                        totalInflated += bytes.length;
                        if (totalInflated > MAX_TOTAL_INFLATED_BYTES) {
                            throw new IllegalStateException(
                                    "KB zip exceeds max total inflated size " + MAX_TOTAL_INFLATED_BYTES + " bytes");
                        }
                        String content = new String(bytes, StandardCharsets.UTF_8);
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
            success = true;
            return docs.stream().map(kbDocumentMapper::toDto).toList();
        } finally {
            metrics.stopKbFetchTimer(sample, String.valueOf(clientId), success);
        }
    }

    /**
     * Reads a single zip entry into memory, aborting if the inflated size exceeds
     * {@link #MAX_ENTRY_BYTES}. Guards against a single oversized/zip-bomb entry without
     * trusting the (forgeable) {@link ZipEntry#getSize()} header.
     */
    private static byte[] readEntryBounded(ZipInputStream zis) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = zis.read(chunk)) != -1) {
            total += read;
            if (total > MAX_ENTRY_BYTES) {
                throw new IllegalStateException("KB zip entry exceeds max size " + MAX_ENTRY_BYTES + " bytes");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    @Async("backgroundTaskExecutor")
    @Override
    public void fetchAsync(Long clientId, String jobId) {
        statusStore.put(jobId, KbFetchJobStatus.running(jobId));
        try {
            List<KbDocumentDto> docs = fetchAndStore(clientId);
            statusStore.put(jobId, KbFetchJobStatus.done(jobId, docs.size()));
        } catch (Exception e) {
            log.error("Async KB fetch failed for clientId={}: {}", clientId, e.getMessage());
            statusStore.put(jobId, KbFetchJobStatus.error(jobId, e.getMessage()));
        }
    }

    @Override
    public List<KbDocumentDto> listByClient(Long clientId) {
        return kbRepository.findByClientIdOrderByFilenameAsc(clientId)
                .stream().map(kbDocumentMapper::toDto).toList();
    }

    @Override
    public List<KbDocumentDto> search(Long clientId, String q) {
        if (q == null || q.isBlank()) return listByClient(clientId);
        return kbRepository.searchByClientId(clientId, q)
                .stream().map(kbDocumentMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void clearByClient(Long clientId) {
        kbRepository.deleteByClientId(clientId);
    }
}


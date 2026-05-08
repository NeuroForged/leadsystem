package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.entity.KnowledgeBaseDocument;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.KnowledgeBaseDocumentRepository;
import com.neuroforged.leadsystem.service.LeadEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadEnrichmentServiceImpl implements LeadEnrichmentService {

    private static final int SNIPPET_MAX_LENGTH = 1000;

    private final KnowledgeBaseDocumentRepository kbRepository;
    private final LeadSystemMetrics metrics;

    @Override
    public void enrich(Lead lead) {
        String clientId;
        try {
            clientId = lead.getClientIdStr();
            Long clientLongId = Long.parseLong(clientId);

            String query = buildQuery(lead);
            if (query.isBlank()) {
                log.debug("LeadEnrichment: no query terms for lead id={}, skipping", lead.getId());
                metrics.recordEnrichmentOutcome("skipped");
                return;
            }

            List<KnowledgeBaseDocument> results = kbRepository.searchByClientId(clientLongId, query);
            if (results.isEmpty()) {
                log.debug("LeadEnrichment: no KB match for clientId={} query='{}'", clientLongId, query);
                metrics.recordEnrichmentOutcome("miss");
                return;
            }

            String snippet = truncate(results.get(0).getContent(), SNIPPET_MAX_LENGTH);
            lead.setRelevantKbSnippet(snippet);
            metrics.recordEnrichmentOutcome("hit");
            log.debug("LeadEnrichment: attached KB snippet ({} chars) from '{}' to lead id={}",
                    snippet.length(), results.get(0).getFilename(), lead.getId());

        } catch (NumberFormatException e) {
            log.warn("LeadEnrichment: could not parse clientId '{}' — skipping", lead.getClientIdStr());
            metrics.recordEnrichmentOutcome("skipped");
        } catch (Exception e) {
            log.warn("LeadEnrichment: failed for lead id={}: {}", lead.getId(), e.getMessage());
            metrics.recordEnrichmentOutcome("error");
        }
    }

    private String buildQuery(Lead lead) {
        return Stream.of(lead.getBusinessType(), lead.getCustomerType(),
                        truncate(lead.getLeadChallenge(), 60))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}

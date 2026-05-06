package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Lead;

public interface LeadEnrichmentService {

    /**
     * Searches the client's knowledge base for content relevant to the lead
     * and updates lead.relevantKbSnippet in-place (does not persist).
     */
    void enrich(Lead lead);
}

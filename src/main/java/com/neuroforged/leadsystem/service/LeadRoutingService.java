package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Lead;

public interface LeadRoutingService {

    /**
     * Evaluates routing rules for the lead's client and sets lead.assignedTo
     * in-place if a rule matches. Does not persist.
     */
    void route(Lead lead);
}

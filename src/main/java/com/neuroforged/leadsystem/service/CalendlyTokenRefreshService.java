package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.CalendlyAccount;

public interface CalendlyTokenRefreshService {

    /**
     * Returns a CalendlyAccount with a guaranteed-fresh access token.
     * Refreshes transparently if the token is expired or within 10 minutes of expiry.
     * Marks the account requiresReauth=true and notifies admin if refresh fails.
     */
    CalendlyAccount ensureFreshToken(Long clientId);
}

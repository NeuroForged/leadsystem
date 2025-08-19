package com.neuroforged.leadsystem.client;

import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;

public interface CalendlyApiClient {

    /**
     * Builds an authorization URL for Calendly with the given OAuth state.
     */
    String exchangeAuthCodeForTokens(String state);

    /**
     * Exchanges the authorization code for an access + refresh token.
     */
    CalendlyTokenResponse exchangeCodeForToken(String code);
}

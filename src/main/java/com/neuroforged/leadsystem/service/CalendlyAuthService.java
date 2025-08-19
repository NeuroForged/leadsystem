package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.CalendlyAuthResponse;
import com.neuroforged.leadsystem.dto.CalendlyOAuthRequest;

public interface CalendlyAuthService {
    CalendlyAuthResponse generateAuthorizationUrl(Long clientId);
    void handleOAuthCallback(CalendlyOAuthRequest request);
}

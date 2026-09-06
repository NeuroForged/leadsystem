package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.dto.CalendlyAuthResponse;
import com.neuroforged.leadsystem.dto.CalendlyOAuthRequest;
import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.entity.CalendlyIntegration;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.CalendlyIntegrationRepository;
import com.neuroforged.leadsystem.service.CalendlyAuthService;
import com.neuroforged.leadsystem.service.ClientOAuthStateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendlyAuthServiceImpl implements CalendlyAuthService {

    private final CalendlyApiClient calendlyApiClient;
    private final ClientOAuthStateService clientOAuthStateService;
    private final CalendlyIntegrationRepository calendlyIntegrationRepository;
    private final CalendlyAccountRepository calendlyAccountRepository;

    @Override
    @Transactional
    public CalendlyAuthResponse generateAuthorizationUrl(Long clientId) {
        log.info("Generating Calendly authorization URL for clientId={}", clientId);
        String state = UUID.randomUUID().toString();

        // Persist the state with clientId. Do not log the state value (CSRF token).
        clientOAuthStateService.saveOAuthState(state, clientId);
        String authorizationUrl = calendlyApiClient.exchangeAuthCodeForTokens(state);

        return new CalendlyAuthResponse(authorizationUrl, state);
    }

    @Override
    @Transactional
    public void handleOAuthCallback(CalendlyOAuthRequest request) {
        // Never log the OAuth authorization code or CSRF state (single-use creds / replayable).
        log.info("Handling Calendly OAuth callback");
        Optional<Long> clientIdOpt = clientOAuthStateService.findClientIdByOAuthState(request.getState());

        if (clientIdOpt.isEmpty()) {
            log.warn("Invalid or expired OAuth state on Calendly callback");
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }

        Long clientId = clientIdOpt.get();
        CalendlyTokenResponse tokenResponse = calendlyApiClient.exchangeCodeForToken(request.getCode());
        // LSB-157: never log full tokens. Mask via LogMasking.mask() so Loki/Grafana
        // log access can't be used to harvest customer Calendly creds.
        log.info("Calendly OAuth token exchange OK clientId={} owner={} ownerType={} organization={} accessToken={} refreshToken={}",
                clientId,
                tokenResponse.getOwner(),
                tokenResponse.getOwnerType(),
                tokenResponse.getOrganization(),
                com.neuroforged.leadsystem.security.LogMasking.mask(tokenResponse.getAccessToken()),
                com.neuroforged.leadsystem.security.LogMasking.mask(tokenResponse.getRefreshToken()));
        // Upsert: client_id is UNIQUE, and re-authentication (the path the "please
        // reconnect" email sends the admin down) used to insert a second row and 500.
        CalendlyAccount account = calendlyAccountRepository.findByClientId(clientId)
                .orElseGet(() -> CalendlyAccount.builder().clientId(clientId).build());
        account.setAccessToken(tokenResponse.getAccessToken());
        account.setRefreshToken(tokenResponse.getRefreshToken());
        account.setOwner(tokenResponse.getOwner());
        account.setOwnerType(tokenResponse.getOwnerType());
        account.setOrganization(tokenResponse.getOrganization());
        account.setTokenIssuedAt(LocalDateTime.now());
        account.setRequiresReauth(false);

        calendlyAccountRepository.save(account);
        calendlyIntegrationRepository.findByState(request.getState()).ifPresent(integration -> {
            integration.setCompleted(true);
            calendlyIntegrationRepository.save(integration);
        });

        log.info("Calendly OAuth completed and account saved for clientId={}", clientId);
    }
}

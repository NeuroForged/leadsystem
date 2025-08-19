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
        log.info("CalendlyAuthService.generateAuthorizationUrl()");
        String state = UUID.randomUUID().toString();

        // Persist the state with clientId
        clientOAuthStateService.saveOAuthState(state, clientId);
        log.info("after saved state: {}, clientId: {}", state, clientId);
        String authorizationUrl = calendlyApiClient.exchangeAuthCodeForTokens(state);

        return new CalendlyAuthResponse(authorizationUrl, state);
    }

    @Override
    @Transactional
    public void handleOAuthCallback(CalendlyOAuthRequest request) {
        log.info("Handling Calendly OAuth callback with code={} and state={}", request.getCode(), request.getState());
        Optional<Long> clientIdOpt = clientOAuthStateService.findClientIdByOAuthState(request.getState());

        log.info("ClientIdOpt = {}", clientIdOpt);
        if (clientIdOpt.isEmpty()) {
            log.warn("Invalid or expired OAuth state: {}", request.getState());
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }

        Long clientId = clientIdOpt.get();
        log.info("client id: {}", clientId);
        CalendlyTokenResponse tokenResponse = calendlyApiClient.exchangeCodeForToken(request.getCode());
        log.info("token response:\n access token: {}\nrefresh token: {}\nowner: {}\nownterType: {}\norganization: {}\nclientId: {} "
        ,tokenResponse.getAccessToken(),tokenResponse.getRefreshToken(), tokenResponse.getOwner(), tokenResponse.getOwnerType(), tokenResponse.getOrganization(), clientId);
        CalendlyAccount account = CalendlyAccount.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .owner(tokenResponse.getOwner())
                .ownerType(tokenResponse.getOwnerType())
                .organization(tokenResponse.getOrganization())
                .clientId(clientId)
                .build();

        calendlyAccountRepository.save(account);
        log.info("calendlyintegrationrepository.findbystate({})",request.getState());
        calendlyIntegrationRepository.findByState(request.getState()).ifPresent(integration -> {
            log.info("client: {}, id: {}, state: {}", integration.getClient(), integration.getId(), integration.getState());
            integration.setCompleted(true);
            calendlyIntegrationRepository.save(integration);
        });

        log.info("Calendly OAuth completed and account saved for clientId={}", clientId);
    }
}

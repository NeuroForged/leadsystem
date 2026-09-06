package com.neuroforged.leadsystem.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import com.neuroforged.leadsystem.entity.CalendlyIntegration;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.CalendlyIntegrationRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.ClientOAuthStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientOAuthStateServiceImpl implements ClientOAuthStateService {

    private final CalendlyIntegrationRepository calendlyIntegrationRepository;
    private final ClientRepository clientRepository;

    /** An OAuth state is a one-time, short-lived token. Anything older than this, or
     *  already used, must not bind a Calendly account to a client. */
    private static final Duration STATE_TTL = Duration.ofMinutes(15);

    @Override
    public Optional<Long> findClientIdByOAuthState(String state) {
        return calendlyIntegrationRepository.findByState(state)
                .filter(integration -> !integration.isCompleted())
                .filter(integration -> integration.getCreatedAt() != null
                        && integration.getCreatedAt().plus(STATE_TTL).isAfter(LocalDateTime.now()))
                .map(integration -> integration.getClient().getId());
    }

    @Override
    public void saveOAuthState(String state, Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        CalendlyIntegration integration = new CalendlyIntegration();
        integration.setState(state);
        integration.setClient(client);
        integration.setCompleted(false);

        calendlyIntegrationRepository.save(integration);
        log.info("Persisted OAuth state for clientId={}", clientId);
    }
}

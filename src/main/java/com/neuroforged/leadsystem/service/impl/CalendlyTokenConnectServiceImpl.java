package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.CalendlyTokenConnectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendlyTokenConnectServiceImpl implements CalendlyTokenConnectService {

    private final CalendlyApiClient apiClient;
    private final CalendlyAccountRepository accountRepository;
    private final ClientRepository clientRepository;

    @Override
    public Map<String, String> connect(Long clientId, String token) {
        if (!clientRepository.existsById(clientId)) throw new ResourceNotFoundException("Client not found: " + clientId);
        if (token == null || token.isBlank()) throw new IllegalArgumentException("A Calendly personal access token is required.");

        Map<String, Object> user;
        try {
            user = apiClient.fetchCurrentUser(token.trim());
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("Calendly rejected that token (HTTP " + e.getStatusCode().value() + ").");
        }
        Object uri = user.get("uri");
        Object org = user.get("current_organization");
        if (uri == null || org == null) throw new IllegalArgumentException("Calendly did not return an account for that token.");

        CalendlyAccount account = accountRepository.findByClientId(clientId).orElseGet(CalendlyAccount::new);
        account.setClientId(clientId);
        account.setAccessToken(token.trim());
        account.setRefreshToken(null);            // personal access tokens do not expire or refresh
        account.setOwner(uri.toString());
        account.setOwnerType("users");
        account.setOrganization(org.toString());
        account.setTokenIssuedAt(LocalDateTime.now());
        account.setRequiresReauth(false);
        account.setUsePolling(true);               // no public webhook endpoint needed
        account.setLastPolledAt(null);
        accountRepository.save(account);
        log.info("Calendly connected by personal access token for clientId={} owner={}", clientId, uri);
        return Map.of(
                "name", String.valueOf(user.getOrDefault("name", "")),
                "schedulingUrl", String.valueOf(user.getOrDefault("scheduling_url", "")));
    }
}

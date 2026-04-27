package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.service.CalendlyTokenRefreshService;
import com.neuroforged.leadsystem.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalendlyTokenRefreshServiceImpl implements CalendlyTokenRefreshService {

    private static final int TOKEN_EXPIRY_MINUTES = 120;
    private static final int REFRESH_BUFFER_MINUTES = 10;

    private final CalendlyAccountRepository calendlyAccountRepository;
    private final CalendlyApiClient calendlyApiClient;
    private final EmailService emailService;

    @Override
    public CalendlyAccount ensureFreshToken(Long clientId) {
        CalendlyAccount account = calendlyAccountRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("No Calendly account for clientId: " + clientId));

        if (account.isRequiresReauth()) {
            throw new IllegalStateException("Calendly account requires re-authentication for clientId: " + clientId);
        }

        if (needsRefresh(account)) {
            account = refresh(account, clientId);
        }

        return account;
    }

    private boolean needsRefresh(CalendlyAccount account) {
        LocalDateTime issuedAt = account.getTokenIssuedAt();
        if (issuedAt == null) {
            return true;
        }
        LocalDateTime expiresAt = issuedAt.plusMinutes(TOKEN_EXPIRY_MINUTES - REFRESH_BUFFER_MINUTES);
        return LocalDateTime.now().isAfter(expiresAt);
    }

    private CalendlyAccount refresh(CalendlyAccount account, Long clientId) {
        log.info("Refreshing Calendly access token for clientId={}", clientId);
        try {
            CalendlyTokenResponse response = calendlyApiClient.refreshAccessToken(account.getRefreshToken());
            account.setAccessToken(response.getAccessToken());
            account.setRefreshToken(response.getRefreshToken());
            account.setTokenIssuedAt(LocalDateTime.now());
            calendlyAccountRepository.save(account);
            log.info("Calendly access token refreshed for clientId={}", clientId);
        } catch (Exception e) {
            log.error("Calendly token refresh failed for clientId={}", clientId, e);
            account.setRequiresReauth(true);
            calendlyAccountRepository.save(account);
            emailService.notifyAdminOfWebhookFailure(
                    "Calendly re-authentication required for clientId=" + clientId +
                    ". The refresh token has expired. Please reconnect the Calendly account. Error: " + e.getMessage());
            throw new IllegalStateException("Calendly token refresh failed for clientId: " + clientId, e);
        }
        return account;
    }
}

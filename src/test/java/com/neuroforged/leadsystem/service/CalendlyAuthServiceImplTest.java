package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.dto.CalendlyAuthResponse;
import com.neuroforged.leadsystem.dto.CalendlyOAuthRequest;
import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.CalendlyIntegrationRepository;
import com.neuroforged.leadsystem.service.impl.CalendlyAuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendlyAuthServiceImplTest {

    @Mock private CalendlyApiClient calendlyApiClient;
    @Mock private ClientOAuthStateService clientOAuthStateService;
    @Mock private CalendlyIntegrationRepository calendlyIntegrationRepository;
    @Mock private CalendlyAccountRepository calendlyAccountRepository;

    @InjectMocks
    private CalendlyAuthServiceImpl service;

    @Test
    void generateAuthorizationUrl_returnsUrlAndState() {
        when(calendlyApiClient.exchangeAuthCodeForTokens(anyString()))
                .thenReturn("https://calendly.com/oauth/authorize?client_id=test&state=abc");

        CalendlyAuthResponse response = service.generateAuthorizationUrl(1L);

        assertThat(response.getAuthorizationUrl()).contains("calendly.com");
        assertThat(response.getState()).isNotBlank();
        verify(clientOAuthStateService).saveOAuthState(anyString(), eq(1L));
    }

    @Test
    void handleOAuthCallback_validState_savesCalendlyAccount() {
        CalendlyOAuthRequest request = new CalendlyOAuthRequest("auth-code-123", "valid-state");

        CalendlyTokenResponse tokenResponse = new CalendlyTokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");
        tokenResponse.setOwner("https://api.calendly.com/users/abc");
        tokenResponse.setOwnerType("User");
        tokenResponse.setOrganization("https://api.calendly.com/organizations/xyz");

        when(clientOAuthStateService.findClientIdByOAuthState("valid-state")).thenReturn(Optional.of(1L));
        when(calendlyApiClient.exchangeCodeForToken("auth-code-123")).thenReturn(tokenResponse);
        when(calendlyIntegrationRepository.findByState("valid-state")).thenReturn(Optional.empty());

        service.handleOAuthCallback(request);

        ArgumentCaptor<CalendlyAccount> captor = ArgumentCaptor.forClass(CalendlyAccount.class);
        verify(calendlyAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccessToken()).isEqualTo("access-token");
        assertThat(captor.getValue().getClientId()).isEqualTo(1L);
    }

    @Test
    void handleOAuthCallback_invalidState_throwsIllegalArgumentException() {
        CalendlyOAuthRequest request = new CalendlyOAuthRequest("code", "expired-state");

        when(clientOAuthStateService.findClientIdByOAuthState("expired-state")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleOAuthCallback(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OAuth state");

        verify(calendlyAccountRepository, never()).save(any());
    }
}

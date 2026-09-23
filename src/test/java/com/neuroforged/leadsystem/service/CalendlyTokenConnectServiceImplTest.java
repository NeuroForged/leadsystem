package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.client.CalendlyApiClient;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.repository.CalendlyAccountRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.impl.CalendlyTokenConnectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendlyTokenConnectServiceImplTest {

    @Mock CalendlyApiClient apiClient;
    @Mock CalendlyAccountRepository accountRepository;
    @Mock ClientRepository clientRepository;
    @InjectMocks CalendlyTokenConnectServiceImpl service;

    @Test
    void connect_validToken_storesAPollingAccountWithoutRefreshToken() {
        when(clientRepository.existsById(2L)).thenReturn(true);
        when(apiClient.fetchCurrentUser("pat-123")).thenReturn(Map.of(
                "uri", "https://api.calendly.com/users/u1", "current_organization", "https://api.calendly.com/organizations/o1",
                "name", "Josh", "scheduling_url", "https://calendly.com/josh"));
        when(accountRepository.findByClientId(2L)).thenReturn(Optional.empty());

        Map<String, String> out = service.connect(2L, " pat-123 ");

        ArgumentCaptor<CalendlyAccount> saved = ArgumentCaptor.forClass(CalendlyAccount.class);
        verify(accountRepository).save(saved.capture());
        CalendlyAccount a = saved.getValue();
        assertThat(a.getClientId()).isEqualTo(2L);
        assertThat(a.getAccessToken()).isEqualTo("pat-123");
        assertThat(a.getRefreshToken()).isNull();
        assertThat(a.isUsePolling()).isTrue();
        assertThat(a.isRequiresReauth()).isFalse();
        assertThat(a.getOrganization()).endsWith("/o1");
        assertThat(out).containsEntry("schedulingUrl", "https://calendly.com/josh");
    }

    @Test
    void connect_rejectedToken_isABadRequestNotAStoredAccount() {
        when(clientRepository.existsById(2L)).thenReturn(true);
        when(apiClient.fetchCurrentUser(any())).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.connect(2L, "bad")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("401");
        verify(accountRepository, never()).save(any());
    }
}

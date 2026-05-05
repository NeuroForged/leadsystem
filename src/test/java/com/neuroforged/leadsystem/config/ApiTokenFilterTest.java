package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.ClientRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTokenFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ApiTokenFilter filter;

    private static final String VALID_KEY = "valid-api-key-123";
    private static final Long CLIENT_ID = 42L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        lenient().when(rateLimitService.tryConsume(any())).thenReturn(true);

        Client client = new Client();
        client.setId(CLIENT_ID);
        lenient().when(clientRepository.findByApiKey(eq(VALID_KEY))).thenReturn(Optional.of(client));
        lenient().when(clientRepository.findByApiKey(argThat(k -> !VALID_KEY.equals(k)))).thenReturn(Optional.empty());
    }

    @Test
    void validApiKey_onLeadsPath_setsInternalAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        request.addHeader("X-Api-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("internal-bot");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL"));
        assertThat(request.getAttribute(ApiTokenFilter.API_KEY_CLIENT_ID_ATTR)).isEqualTo(CLIENT_ID);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidApiKey_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        request.addHeader("X-Api-Key", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingApiKey_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void validApiKey_nonLeadsPath_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clients");
        request.addHeader("X-Api-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}

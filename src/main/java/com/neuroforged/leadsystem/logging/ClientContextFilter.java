package com.neuroforged.leadsystem.logging;

import com.neuroforged.leadsystem.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.neuroforged.leadsystem.config.ApiTokenFilter.API_KEY_CLIENT_ID_ATTR;

/**
 * Reads the resolved principal from the SecurityContext and places clientId + userId
 * into MDC so every downstream log line carries those fields automatically.
 *
 * Runs after Spring Security's filter chain has resolved the principal
 * (JWT or API key).  LSB-134.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ClientContextFilter extends OncePerRequestFilter {

    public static final String MDC_CLIENT_ID = "clientId";
    public static final String MDC_USER_ID   = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {
        try {
            populateMdc(request);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CLIENT_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private void populateMdc(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            // JWT-authenticated admin or client user
            if (principal.getClientId() != null) {
                MDC.put(MDC_CLIENT_ID, String.valueOf(principal.getClientId()));
            }
            MDC.put(MDC_USER_ID, principal.getUsername());
            return;
        }

        // API-key authenticated chatbot — clientId stored as request attribute
        Object apiClientId = request.getAttribute(API_KEY_CLIENT_ID_ATTR);
        if (apiClientId != null) {
            MDC.put(MDC_CLIENT_ID, String.valueOf(apiClientId));
            MDC.put(MDC_USER_ID, "api-bot");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }
}

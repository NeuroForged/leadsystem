package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.repository.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    public static final String API_KEY_CLIENT_ID_ATTR = "apiKeyClientId";

    @Value("${neuroforged.tokens.internal}")
    private String internalToken;

    private final RateLimitService rateLimitService;
    private final ClientRepository clientRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getRequestURI();
        String apiKey = request.getHeader("X-Api-Key");

        if (path.startsWith("/api/leads") && apiKey != null) {
            Long resolvedClientId = null;

            if (apiKey.equals(internalToken)) {
                // Global shared token — no client scoping
                log.debug("Authenticated via global internal token for path: {}", path);
            } else {
                // Try per-client API key
                var client = clientRepository.findByApiKey(apiKey);
                if (client.isPresent()) {
                    resolvedClientId = client.get().getId();
                    log.debug("Authenticated via client API key for clientId={}, path={}", resolvedClientId, path);
                } else {
                    chain.doFilter(request, response);
                    return;
                }
            }

            if (!rateLimitService.tryConsume(apiKey)) {
                log.warn("Rate limit exceeded for API key on path: {}", path);
                long retryAfter = rateLimitService.getSecondsUntilRefill(apiKey);
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"retryAfterSeconds\":" + retryAfter + "}");
                return;
            }

            if (resolvedClientId != null) {
                request.setAttribute(API_KEY_CLIENT_ID_ATTR, resolvedClientId);
            }

            UserDetails userDetails = User.withUsername("internal-bot")
                    .password("")
                    .roles("INTERNAL")
                    .build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}

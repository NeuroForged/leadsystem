package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.repository.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates chatbot lead-submission requests via per-client API keys.
 *
 * Each Client record has a unique {@code apiKey} field generated at creation time.
 * The chatbot sends it as {@code X-Api-Key: <key>}. This filter looks up the matching
 * Client, attaches its ID as {@value #API_KEY_CLIENT_ID_ATTR} on the request, and
 * grants the INTERNAL Spring Security role.
 *
 * Requests with an unrecognised or missing key fall through to the JWT filter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    /** Request attribute name carrying the authenticated client's Long ID. */
    public static final String API_KEY_CLIENT_ID_ATTR = "apiKeyClientId";

    private final RateLimitService rateLimitService;
    private final ClientRepository clientRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getRequestURI();
        String apiKey = request.getHeader("X-Api-Key");

        if ((path.startsWith("/api/leads") || path.startsWith("/api/v1/leads")) && apiKey != null) {
            var client = clientRepository.findByApiKey(apiKey);
            if (client.isEmpty()) {
                log.warn("Rejected X-Api-Key request — key not found for path: {}", path);
                chain.doFilter(request, response);
                return;
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

            Long clientId = client.get().getId();
            request.setAttribute(API_KEY_CLIENT_ID_ATTR, clientId);
            log.debug("Authenticated via client API key for clientId={}, path={}", clientId, path);

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

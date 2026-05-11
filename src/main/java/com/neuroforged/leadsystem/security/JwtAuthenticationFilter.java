package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final LeadSystemMetrics metrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("Attempting to authenticate via Bearer token or cookie");

        String jwt = extractFromHeader(request);
        if (jwt == null) {
            jwt = extractFromCookie(request, "alchemize_at");
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email;
        try {
            email = jwtUtil.extractUsername(jwt);
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            metrics.recordAuthFailure("invalid_token");
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                metrics.recordAuthFailure("user_not_found");
            } else if (!jwtUtil.validateToken(jwt)) {
                metrics.recordAuthFailure("invalid_token");
            } else if ("refresh".equals(jwtUtil.extractTokenType(jwt))) {
                metrics.recordAuthFailure("refresh_token_used");
            } else {
                // LSB-162: log (don't reject) tokens missing iss/aud during the grace
                // window. Once the warn count drops to zero in Loki we can flip
                // neuroforged.jwt.enforce-iss-aud=true to reject mismatched tokens.
                if (!jwtUtil.hasValidIssAud(jwt)) {
                    log.warn("JWT missing iss/aud claims (grace window) email={}", email);
                    metrics.recordAuthFailure("missing_iss_aud_grace");
                }
                CustomUserPrincipal principal = new CustomUserPrincipal(
                        user.get().getEmail(),
                        user.get().getPassword(),
                        user.get().getRole(),
                        user.get().getClientId()
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

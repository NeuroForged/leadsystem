package com.neuroforged.leadsystem.config;

import jakarta.servlet.*;
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

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenFilter implements Filter {

    @Value("${neuroforged.tokens.internal}")
    private String internalToken;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.info("Attempting to authenticate via X-Api-Key");
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();
        String apiKey = httpReq.getHeader("X-Api-Key");
        log.info("internal token: {}\nprovided token: {}", internalToken, apiKey);
        if (path.startsWith("/api/leads") && apiKey != null && apiKey.equals(internalToken)) {
            // 🔐 Authenticate this request manually
            UserDetails userDetails = User.withUsername("internal-bot")
                    .password("") // password not needed
                    .roles("INTERNAL")
                    .build();
            log.info("Authenticated by X-Api-Key");
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpReq));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        log.info("Could not Authenticate by X-Api-Key");
        chain.doFilter(request, response);
    }
}

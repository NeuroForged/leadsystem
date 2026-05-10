package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enables Hibernate's "longClientFilter" on Lead and related queries for CLIENT-role requests,
 * providing automatic row-level tenant isolation as a safety net.
 * ADMIN requests do not enable the filter and can see all tenants' data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (AuthPrincipalUtil.isClient()) {
            Long clientId = AuthPrincipalUtil.currentClientId();
            if (clientId != null) {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("longClientFilter").setParameter("clientId", clientId);
                log.debug("Enabled longClientFilter for clientId={}", clientId);
            }
        }
        return true;
    }
}

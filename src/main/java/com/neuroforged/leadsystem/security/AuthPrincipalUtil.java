package com.neuroforged.leadsystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthPrincipalUtil {

    private AuthPrincipalUtil() {}

    public static Long currentClientId() {
        CustomUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getClientId();
    }

    public static String currentRole() {
        CustomUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getRole();
    }

    public static String currentEmail() {
        CustomUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getUsername();
    }

    public static boolean isClient() {
        return UserRole.isClient(currentRole());
    }

    public static boolean isAdmin() {
        return UserRole.isAdmin(currentRole());
    }

    private static CustomUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object p = auth.getPrincipal();
        return p instanceof CustomUserPrincipal cup ? cup : null;
    }
}

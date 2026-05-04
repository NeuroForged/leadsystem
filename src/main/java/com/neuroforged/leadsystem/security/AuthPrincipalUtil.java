package com.neuroforged.leadsystem.security;

import org.springframework.security.access.AccessDeniedException;
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

    /**
     * For endpoints that take an optional clientId filter:
     *  - ADMIN: pass through (may be null = all clients)
     *  - CLIENT: force to the caller's clientId; deny if a different one was requested
     */
    public static Long resolveClientIdForCaller(Long requested) {
        if (!isClient()) {
            return requested;
        }
        Long mine = currentClientId();
        if (mine == null) {
            throw new AccessDeniedException("CLIENT user has no associated clientId");
        }
        if (requested != null && !requested.equals(mine)) {
            throw new AccessDeniedException("Cannot access another client's data");
        }
        return mine;
    }

    /**
     * String variant for endpoints that filter by Lead.clientId (String).
     * The caller's Long clientId is rendered to its String form for comparison.
     */
    public static String resolveStringClientIdForCaller(String requested) {
        if (!isClient()) {
            return requested;
        }
        Long mine = currentClientId();
        if (mine == null) {
            throw new AccessDeniedException("CLIENT user has no associated clientId");
        }
        String mineAsString = mine.toString();
        if (requested != null && !requested.isBlank() && !requested.equals(mineAsString)) {
            throw new AccessDeniedException("Cannot access another client's data");
        }
        return mineAsString;
    }

    /**
     * For get-by-id endpoints: assert the loaded entity's clientId is reachable by the caller.
     * ADMIN always passes; CLIENT must match their own clientId.
     */
    public static void assertCanAccessClient(Long clientId) {
        if (!isClient()) {
            return;
        }
        Long mine = currentClientId();
        if (mine == null || !mine.equals(clientId)) {
            throw new AccessDeniedException("Cannot access another client's data");
        }
    }

    public static void assertCanAccessStringClient(String clientId) {
        if (!isClient()) {
            return;
        }
        Long mine = currentClientId();
        if (mine == null || !mine.toString().equals(clientId)) {
            throw new AccessDeniedException("Cannot access another client's data");
        }
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

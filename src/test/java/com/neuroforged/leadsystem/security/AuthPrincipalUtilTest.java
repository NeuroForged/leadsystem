package com.neuroforged.leadsystem.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPrincipalUtilTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String role, Long clientId) {
        CustomUserPrincipal principal = new CustomUserPrincipal("user@test.com", "pw", role, clientId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "pw", principal.getAuthorities()));
    }

    // ----- currentClientId / currentRole / currentEmail -----

    @Test
    void noAuth_returnsNullsAndFalse() {
        assertThat(AuthPrincipalUtil.currentClientId()).isNull();
        assertThat(AuthPrincipalUtil.currentRole()).isNull();
        assertThat(AuthPrincipalUtil.currentEmail()).isNull();
        assertThat(AuthPrincipalUtil.isAdmin()).isFalse();
        assertThat(AuthPrincipalUtil.isClient()).isFalse();
    }

    @Test
    void adminAuth_returnsAdminRoleAndEmail() {
        authenticate("ADMIN", null);
        assertThat(AuthPrincipalUtil.currentRole()).isEqualTo("ADMIN");
        assertThat(AuthPrincipalUtil.currentEmail()).isEqualTo("user@test.com");
        assertThat(AuthPrincipalUtil.currentClientId()).isNull();
        assertThat(AuthPrincipalUtil.isAdmin()).isTrue();
        assertThat(AuthPrincipalUtil.isClient()).isFalse();
    }

    @Test
    void clientAuth_returnsClientRoleAndClientId() {
        authenticate("CLIENT", 42L);
        assertThat(AuthPrincipalUtil.currentRole()).isEqualTo("CLIENT");
        assertThat(AuthPrincipalUtil.currentClientId()).isEqualTo(42L);
        assertThat(AuthPrincipalUtil.isClient()).isTrue();
        assertThat(AuthPrincipalUtil.isAdmin()).isFalse();
    }

    // ----- resolveClientIdForCaller(Long) -----

    @Test
    void resolveClientIdForCaller_admin_passesThroughAnyValue() {
        authenticate("ADMIN", null);
        assertThat(AuthPrincipalUtil.resolveClientIdForCaller(null)).isNull();
        assertThat(AuthPrincipalUtil.resolveClientIdForCaller(99L)).isEqualTo(99L);
    }

    @Test
    void resolveClientIdForCaller_client_matchingRequestedReturnsOwn() {
        authenticate("CLIENT", 7L);
        assertThat(AuthPrincipalUtil.resolveClientIdForCaller(7L)).isEqualTo(7L);
    }

    @Test
    void resolveClientIdForCaller_client_nullRequestedReturnsOwn() {
        authenticate("CLIENT", 7L);
        assertThat(AuthPrincipalUtil.resolveClientIdForCaller(null)).isEqualTo(7L);
    }

    @Test
    void resolveClientIdForCaller_client_mismatchedRequestedThrows() {
        authenticate("CLIENT", 7L);
        assertThatThrownBy(() -> AuthPrincipalUtil.resolveClientIdForCaller(8L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("another client");
    }

    @Test
    void resolveClientIdForCaller_clientWithNoClientId_throws() {
        authenticate("CLIENT", null);
        assertThatThrownBy(() -> AuthPrincipalUtil.resolveClientIdForCaller(null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no associated clientId");
    }

    // ----- resolveStringClientIdForCaller(String) -----

    @Test
    void resolveStringClientIdForCaller_admin_passesThroughAnyValue() {
        authenticate("ADMIN", null);
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller(null)).isNull();
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller("acme")).isEqualTo("acme");
    }

    @Test
    void resolveStringClientIdForCaller_client_matchingReturnsOwn() {
        authenticate("CLIENT", 7L);
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller("7")).isEqualTo("7");
    }

    @Test
    void resolveStringClientIdForCaller_client_nullOrBlankReturnsOwn() {
        authenticate("CLIENT", 7L);
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller(null)).isEqualTo("7");
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller("")).isEqualTo("7");
        assertThat(AuthPrincipalUtil.resolveStringClientIdForCaller("   ")).isEqualTo("7");
    }

    @Test
    void resolveStringClientIdForCaller_client_mismatchedThrows() {
        authenticate("CLIENT", 7L);
        assertThatThrownBy(() -> AuthPrincipalUtil.resolveStringClientIdForCaller("8"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveStringClientIdForCaller_clientWithNoClientId_throws() {
        authenticate("CLIENT", null);
        assertThatThrownBy(() -> AuthPrincipalUtil.resolveStringClientIdForCaller("anything"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ----- assertCanAccessClient(Long) -----

    @Test
    void assertCanAccessClient_adminAlwaysPasses() {
        authenticate("ADMIN", null);
        AuthPrincipalUtil.assertCanAccessClient(1L);
        AuthPrincipalUtil.assertCanAccessClient(null);
    }

    @Test
    void assertCanAccessClient_clientMatchPasses() {
        authenticate("CLIENT", 5L);
        AuthPrincipalUtil.assertCanAccessClient(5L);
    }

    @Test
    void assertCanAccessClient_clientMismatchThrows() {
        authenticate("CLIENT", 5L);
        assertThatThrownBy(() -> AuthPrincipalUtil.assertCanAccessClient(6L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertCanAccessClient_clientWithNullClientIdThrows() {
        authenticate("CLIENT", null);
        assertThatThrownBy(() -> AuthPrincipalUtil.assertCanAccessClient(5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertCanAccessClient_clientNullTargetThrows() {
        authenticate("CLIENT", 5L);
        assertThatThrownBy(() -> AuthPrincipalUtil.assertCanAccessClient(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ----- assertCanAccessStringClient(String) -----

    @Test
    void assertCanAccessStringClient_adminAlwaysPasses() {
        authenticate("ADMIN", null);
        AuthPrincipalUtil.assertCanAccessStringClient("anything");
        AuthPrincipalUtil.assertCanAccessStringClient(null);
    }

    @Test
    void assertCanAccessStringClient_clientMatchPasses() {
        authenticate("CLIENT", 5L);
        AuthPrincipalUtil.assertCanAccessStringClient("5");
    }

    @Test
    void assertCanAccessStringClient_clientMismatchThrows() {
        authenticate("CLIENT", 5L);
        assertThatThrownBy(() -> AuthPrincipalUtil.assertCanAccessStringClient("6"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertCanAccessStringClient_clientWithNullClientIdThrows() {
        authenticate("CLIENT", null);
        assertThatThrownBy(() -> AuthPrincipalUtil.assertCanAccessStringClient("5"))
                .isInstanceOf(AccessDeniedException.class);
    }
}

package com.neuroforged.leadsystem.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserPrincipalTest {

    @Test
    void admin_authoritiesContainSingleRoleAdmin() {
        CustomUserPrincipal p = new CustomUserPrincipal("a@x.com", "pw", "ADMIN", null);
        assertThat(p.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void client_authoritiesContainSingleRoleClient() {
        CustomUserPrincipal p = new CustomUserPrincipal("c@x.com", "pw", "CLIENT", 9L);
        assertThat(p.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void getters_returnConstructorValues() {
        CustomUserPrincipal p = new CustomUserPrincipal("user@x.com", "secret", "CLIENT", 42L);
        assertThat(p.getUsername()).isEqualTo("user@x.com");
        assertThat(p.getPassword()).isEqualTo("secret");
        assertThat(p.getRole()).isEqualTo("CLIENT");
        assertThat(p.getClientId()).isEqualTo(42L);
    }

    @Test
    void nullClientId_isAllowedForAdmin() {
        CustomUserPrincipal p = new CustomUserPrincipal("a@x.com", "pw", "ADMIN", null);
        assertThat(p.getClientId()).isNull();
    }
}

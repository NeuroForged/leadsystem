package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-that-is-at-least-32-chars-long-for-hmac";

    private JwtUtil jwtUtil;
    private ClientRepository clientRepository;
    private User user;

    @BeforeEach
    void setUp() {
        // LSB-162: enforce-iss-aud=false → grace-mode validator, matches prod defaults.
        clientRepository = mock(ClientRepository.class);
        // LSB-170: default the mode lookup to COMPANY for tests that don't override.
        Client defaultClient = new Client();
        defaultClient.setMode("COMPANY");
        when(clientRepository.findById(anyLong())).thenReturn(Optional.of(defaultClient));
        jwtUtil = new JwtUtil(SECRET, false, clientRepository);
        user = User.builder()
                .id(1L)
                .email("admin@test.com")
                .password("hashed")
                .role("ADMIN")
                .build();
    }

    @Test
    void generateToken_containsCorrectSubject() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin@test.com");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(user) + "tampered";
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tokenSignedWithDifferentSecret_returnsFalse() {
        JwtUtil otherUtil = new JwtUtil("completely-different-secret-that-is-long-enough", false, clientRepository);
        String foreignToken = otherUtil.generateToken(user);
        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void extractUsername_validToken_returnsEmail() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(user.getEmail());
    }

    @Test
    void generateToken_withClientId_includesClientIdClaim() {
        User clientUser = User.builder()
                .id(2L)
                .email("client@test.com")
                .password("hashed")
                .role("CLIENT")
                .clientId(42L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        assertThat(jwtUtil.extractClientId(token)).isEqualTo(42L);
    }

    @Test
    void generateToken_withoutClientId_extractClientIdReturnsNull() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractClientId(token)).isNull();
    }

    @Test
    void extractClientId_handlesNumberClaim() {
        User clientUser = User.builder()
                .id(3L)
                .email("c@test.com")
                .password("hashed")
                .role("CLIENT")
                .clientId(123L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        // jjwt serialises Long claims as JSON numbers, deserialised as Number
        assertThat(jwtUtil.extractClientId(token)).isEqualTo(123L);
    }

    @Test
    void extractRole_returnsRoleClaim() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_clientRoleReturnsClient() {
        User clientUser = User.builder()
                .id(2L)
                .email("c@test.com")
                .password("hashed")
                .role("CLIENT")
                .clientId(1L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("CLIENT");
    }

    @Test
    void extractRole_returnsNullWhenRoleAbsent() {
        // Build a token that does not include the role claim
        User noRoleUser = User.builder()
                .id(99L)
                .email("noRole@test.com")
                .password("hashed")
                .role(null)
                .build();
        String token = jwtUtil.generateToken(noRoleUser);
        assertThat(jwtUtil.extractRole(token)).isNull();
    }

    // ── LSB-170: mode claim ──────────────────────────────────────────────────

    @Test
    void generateToken_clientUser_includesAgencyModeClaim() {
        Client agency = new Client();
        agency.setMode("AGENCY");
        when(clientRepository.findById(42L)).thenReturn(Optional.of(agency));

        User clientUser = User.builder()
                .id(2L).email("c@test.com").password("hashed").role("CLIENT").clientId(42L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        assertThat(jwtUtil.extractMode(token)).isEqualTo("AGENCY");
    }

    @Test
    void generateToken_clientUserDefaultsToCompanyMode() {
        Client co = new Client();
        co.setMode("COMPANY");
        when(clientRepository.findById(7L)).thenReturn(Optional.of(co));

        User clientUser = User.builder()
                .id(3L).email("c@test.com").password("hashed").role("CLIENT").clientId(7L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        assertThat(jwtUtil.extractMode(token)).isEqualTo("COMPANY");
    }

    @Test
    void generateToken_adminUserHasNoModeClaim() {
        // ADMIN tokens carry no clientId, so no mode is stamped either.
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractMode(token)).isNull();
    }

    @Test
    void generateToken_clientLookupFailureFallsBackToCompany() {
        when(clientRepository.findById(99L)).thenThrow(new RuntimeException("db down"));
        User clientUser = User.builder()
                .id(4L).email("c@test.com").password("hashed").role("CLIENT").clientId(99L)
                .build();
        String token = jwtUtil.generateToken(clientUser);
        assertThat(jwtUtil.extractMode(token)).isEqualTo("COMPANY");
    }
}

package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-that-is-at-least-32-chars-long-for-hmac";

    private JwtUtil jwtUtil;
    private User user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
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
        JwtUtil otherUtil = new JwtUtil("completely-different-secret-that-is-long-enough");
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
}

package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours
    private final long REFRESH_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7; // 7 days

    private final Key key;

    public JwtUtil(@Value("${neuroforged.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256);
        if (user.getClientId() != null) {
            builder.claim("clientId", user.getClientId());
        }
        return builder.compact();
    }

    public String generateRefreshToken(User user) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256);
        if (user.getClientId() != null) {
            builder.claim("clientId", user.getClientId());
        }
        return builder.compact();
    }

    public long getAccessTokenMaxAge() {
        return EXPIRATION_TIME / 1000;
    }

    public long getRefreshTokenMaxAge() {
        return REFRESH_EXPIRATION_TIME / 1000;
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = getClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public Long extractClientId(String token) {
        Object claim = getClaims(token).get("clientId");
        if (claim == null) {
            return null;
        }
        return claim instanceof Number n ? n.longValue() : Long.valueOf(claim.toString());
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractTokenType(String token) {
        Object type = getClaims(token).get("type");
        return type == null ? "access" : type.toString();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

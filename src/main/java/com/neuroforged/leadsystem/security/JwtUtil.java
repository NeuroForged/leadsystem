package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours
    private final long REFRESH_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7; // 7 days

    // LSB-162: pin issuer + audience so a token leaked from one service can't be
    // replayed at another (e.g. the chatbot's admin API). The validator runs in
    // permissive mode during the grace window — see `validateToken()`.
    public static final String ISSUER = "alchemize-leadsystem";
    public static final List<String> AUDIENCES = List.of(
            "alchemize-portal",
            "alchemize-chatbot-admin"
    );
    private static final Set<String> AUDIENCES_SET = Set.copyOf(AUDIENCES);

    private final Key key;
    private final boolean enforceIssAud;

    public JwtUtil(
            @org.springframework.beans.factory.annotation.Autowired
            @Value("${neuroforged.jwt.secret}") String secret,
            // LSB-162: false during the grace window — accept tokens with or without
            // iss/aud claims so old sessions don't get logged out. Flip to true after
            // existing tokens have aged out (max refresh lifetime = 7 days).
            @Value("${neuroforged.jwt.enforce-iss-aud:false}") boolean enforceIssAud) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.enforceIssAud = enforceIssAud;
    }

    public String generateToken(User user) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuer(ISSUER)
                .setAudience(String.join(",", AUDIENCES))
                .claim("role", user.getRole())
                .claim("type", "access")
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
                .setIssuer(ISSUER)
                .setAudience(String.join(",", AUDIENCES))
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
            Claims claims = getClaims(token);
            // LSB-162: enforce iss/aud once the grace window has passed. During
            // the window we still log mismatches (in JwtAuthenticationFilter) so
            // we can confirm all clients have rotated before flipping the flag.
            if (enforceIssAud) {
                if (!ISSUER.equals(claims.getIssuer())) return false;
                if (!audienceContainsAny(claims.getAudience())) return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * LSB-162: returns true if the token carries our issuer + at least one of our
     * audiences. Used by `JwtAuthenticationFilter` for warn-only logging during
     * the grace window so we can see how many old tokens are still in flight.
     */
    public boolean hasValidIssAud(String token) {
        try {
            Claims claims = getClaims(token);
            return ISSUER.equals(claims.getIssuer())
                    && audienceContainsAny(claims.getAudience());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean audienceContainsAny(String audClaim) {
        if (audClaim == null || audClaim.isBlank()) return false;
        // jjwt stores aud as a single comma-joined string (we set it as such).
        for (String aud : audClaim.split(",")) {
            if (AUDIENCES_SET.contains(aud.trim())) return true;
        }
        return false;
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

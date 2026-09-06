package com.neuroforged.leadsystem.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.repository.ClientRepository;
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

    /** Signing key: RSA private key when configured (RS256), else the shared HMAC secret. */
    private final Key key;
    /** Verification key: the RSA public key, or the same HMAC secret. */
    private final Key verifyKey;
    private final SignatureAlgorithm algorithm;
    private final RSAPublicKey rsaPublicKey;   // non-null only in RS256 mode; served at /.well-known/jwks.json
    private final boolean enforceIssAud;
    private final ClientRepository clientRepository;

    @org.springframework.beans.factory.annotation.Autowired   // two constructors: this is the one Spring uses
    public JwtUtil(
            @Value("${neuroforged.jwt.secret}") String secret,
            // LSB-162: false during the grace window — accept tokens with or without
            // iss/aud claims so old sessions don't get logged out. Flip to true after
            // existing tokens have aged out (max refresh lifetime = 7 days).
            @Value("${neuroforged.jwt.enforce-iss-aud:false}") boolean enforceIssAud,
            // Asymmetric signing: PEM (or base64 of the PEM) of an RSA private key. When
            // set, tokens are RS256 and verifiers (chatbot) only ever hold the PUBLIC key —
            // a compromised verifier can no longer mint tokens. Unset = legacy HS256.
            @Value("${neuroforged.jwt.private-key:}") String privateKeyPem,
            ClientRepository clientRepository) {
        this.enforceIssAud = enforceIssAud;
        this.clientRepository = clientRepository;
        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            PrivateKey priv = loadPrivateKey(privateKeyPem);
            RSAPublicKey pub = derivePublicKey(priv);
            this.key = priv;
            this.verifyKey = pub;
            this.rsaPublicKey = pub;
            this.algorithm = SignatureAlgorithm.RS256;
        } else {
            Key hmac = Keys.hmacShaKeyFor(secret.getBytes());
            this.key = hmac;
            this.verifyKey = hmac;
            this.rsaPublicKey = null;
            this.algorithm = SignatureAlgorithm.HS256;
        }
    }

    /** Legacy HS256-only constructor (tests + any caller that predates asymmetric signing). */
    public JwtUtil(String secret, boolean enforceIssAud, ClientRepository clientRepository) {
        this(secret, enforceIssAud, "", clientRepository);
    }

    public boolean isAsymmetric() {
        return rsaPublicKey != null;
    }

    public RSAPublicKey getRsaPublicKey() {
        return rsaPublicKey;
    }

    private static String stripPem(String pem, String label) {
        String body = pem.trim();
        if (!body.contains("-----BEGIN")) {
            // base64 of the whole PEM (convenient for env vars)
            body = new String(Base64.getDecoder().decode(body)).trim();
        }
        return body.replace("-----BEGIN " + label + "-----", "")
                   .replace("-----END " + label + "-----", "")
                   .replaceAll("\\s", "");
    }

    private static PrivateKey loadPrivateKey(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPem(pem, "PRIVATE KEY"));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("neuroforged.jwt.private-key is not a PKCS#8 RSA private key", e);
        }
    }

    private static RSAPublicKey derivePublicKey(PrivateKey priv) {
        try {
            java.security.interfaces.RSAPrivateCrtKey crt = (java.security.interfaces.RSAPrivateCrtKey) priv;
            java.security.spec.RSAPublicKeySpec spec =
                    new java.security.spec.RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Could not derive the RSA public key from the private key", e);
        }
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
                .signWith(key, algorithm);
        if (user.getClientId() != null) {
            builder.claim("clientId", user.getClientId());
            builder.claim("mode", resolveMode(user.getClientId()));
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
                .signWith(key, algorithm);
        if (user.getClientId() != null) {
            builder.claim("clientId", user.getClientId());
            builder.claim("mode", resolveMode(user.getClientId()));
        }
        return builder.compact();
    }

    // LSB-170: looks up the current product mode for the tenant. Failures default
    // to COMPANY so a transient DB blip doesn't lock the user out — the worst
    // case is the next refresh corrects it.
    private String resolveMode(Long clientId) {
        try {
            return clientRepository.findById(clientId)
                    .map(c -> c.getMode() == null ? "COMPANY" : c.getMode())
                    .orElse("COMPANY");
        } catch (Exception e) {
            return "COMPANY";
        }
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

    public String extractMode(String token) {
        Object claim = getClaims(token).get("mode");
        return claim == null ? null : claim.toString();
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
                .setSigningKey(verifyKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Publishes the RS256 verification key so verifiers (the chatbot's admin API) never hold
 * a signing secret. Returns 404 in legacy HS256 mode — there is nothing safe to publish.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtUtil jwtUtil;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        if (!jwtUtil.isAsymmetric()) {
            return ResponseEntity.notFound().build();
        }
        RSAPublicKey pub = jwtUtil.getRsaPublicKey();
        return ResponseEntity.ok(Map.of("keys", List.of(Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "leadsystem-1",
                "n", b64url(pub.getModulus()),
                "e", b64url(pub.getPublicExponent())
        ))));
    }

    private static String b64url(BigInteger v) {
        byte[] bytes = v.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {           // drop the sign byte
            byte[] t = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, t, 0, t.length);
            bytes = t;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

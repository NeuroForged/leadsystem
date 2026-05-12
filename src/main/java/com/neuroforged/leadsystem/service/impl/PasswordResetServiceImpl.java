package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.entity.PasswordResetToken;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.repository.PasswordResetTokenRepository;
import com.neuroforged.leadsystem.repository.UserRepository;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * LSB-164: password-reset service. See {@link PasswordResetService} for contract.
 *
 * <p>Token shape: 32 bytes of {@link SecureRandom} → URL-safe base64 (43 chars).
 * Stored hashed in the DB; the plaintext only ever appears in the outgoing email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${neuroforged.portal.url:https://app.alchemizeiq.com}")
    private String portalUrl;

    @Value("${neuroforged.mail.from:noreply@alchemizeiq.com}")
    private String mailFrom;

    @Override
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) return;
        String normalised = email.toLowerCase().trim();

        Optional<User> userOpt = userRepository.findByEmail(normalised);
        if (userOpt.isEmpty()) {
            // Don't leak whether the email exists — silently no-op.
            log.debug("Password-reset requested for unknown email (silently ignoring).");
            return;
        }

        // Revoke any outstanding tokens for this email before issuing a new one,
        // so an attacker who already harvested a token can't combine it with a
        // fresh request.
        tokenRepository.markAllUsedForEmail(normalised, LocalDateTime.now());

        String plaintext = generateToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .userEmail(normalised)
                .tokenHash(sha256Hex(plaintext))
                .expiresAt(LocalDateTime.now().plus(TOKEN_TTL))
                .build();
        tokenRepository.save(token);

        sendResetEmail(normalised, plaintext);
        log.info("Password-reset email dispatched email={}", normalised);
    }

    @Override
    @Transactional
    public void completeReset(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.length() < 8) {
            throw new InvalidResetTokenException("Token and a new password (min 8 chars) are required.");
        }

        String hash = sha256Hex(token);
        PasswordResetToken stored = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidResetTokenException("Token is invalid or has already been used."));

        if (stored.getUsedAt() != null) {
            throw new InvalidResetTokenException("Token has already been used.");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("Token has expired. Request a new reset email.");
        }

        User user = userRepository.findByEmail(stored.getUserEmail())
                .orElseThrow(() -> new InvalidResetTokenException("Token is invalid."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        stored.setUsedAt(LocalDateTime.now());
        tokenRepository.save(stored);

        log.info("Password reset successful email={}", stored.getUserEmail());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String generateToken() {
        byte[] buf = new byte[32];
        SECURE_RANDOM.nextBytes(buf);
        return URL_ENCODER.encodeToString(buf);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void sendResetEmail(String email, String token) {
        String link = portalUrl.replaceAll("/$", "") + "/login/reset?token=" + token;
        String body = String.format("""
                Hi,

                You (or someone using your email) requested a password reset for your Alchemize account.

                Click this link to choose a new password — it expires in 30 minutes:

                    %s

                If you didn't ask for this, you can safely ignore this email. Your password won't change.

                — Alchemize
                """, link);
        try {
            emailService.sendLeadNotification(email, "Reset your Alchemize password", body);
        } catch (Exception e) {
            // Log but don't surface — we still return silently to the caller.
            log.error("Could not send password-reset email to {}: {}", email, e.getMessage());
        }
    }
}

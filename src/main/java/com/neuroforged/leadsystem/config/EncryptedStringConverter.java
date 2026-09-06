package com.neuroforged.leadsystem.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter applying AES-256-GCM encryption to String columns.
 * Key sourced from NEUROFORGED_ENCRYPTION_KEY env var (Base64-encoded 32-byte key).
 * IV is prepended to the ciphertext (12 bytes), making each encrypted value unique.
 */
@Slf4j
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public EncryptedStringConverter() {
        // Read directly from env/system props to avoid Spring bean ordering issues
        String base64Key = System.getProperty("neuroforged.encryption-key",
                System.getenv("NEUROFORGED_ENCRYPTION_KEY"));
        if (base64Key == null || base64Key.isBlank()) {
            // Do NOT silently degrade to plaintext. The constructor stays non-fatal so
            // profiles that never touch encrypted columns still boot, but any actual
            // encrypt/decrypt with no key configured fails loudly (see requireKey()).
            log.warn("NEUROFORGED_ENCRYPTION_KEY not set — encrypted columns will fail fast on use");
            this.secretKey = null;
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    private void requireKey() {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "NEUROFORGED_ENCRYPTION_KEY is not set — refusing to read/write encrypted columns "
                            + "in plaintext. Configure a Base64-encoded 32-byte AES key.");
        }
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        requireKey();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encrypted) {
        if (encrypted == null) return null;
        requireKey();
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(encrypted);
        } catch (IllegalArgumentException e) {
            // Not Base64 at all: a pre-migration plaintext value. The only case in which
            // returning the stored string is correct.
            log.warn("Stored value is not encrypted (pre-migration plaintext); returning as-is.");
            return encrypted;
        }
        if (combined.length <= GCM_IV_LENGTH) {
            log.warn("Stored value too short to be ciphertext (pre-migration plaintext); returning as-is.");
            return encrypted;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            // A GCM tag failure means the wrong key (rotated / mis-set), not plaintext.
            // Returning the ciphertext here handed Base64 garbage to Calendly as a bearer
            // token, which then cascaded into re-auth for every client. Fail loudly.
            throw new IllegalStateException(
                    "Failed to decrypt stored value — NEUROFORGED_ENCRYPTION_KEY does not match the data", e);
        }
    }
}

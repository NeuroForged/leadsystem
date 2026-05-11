package com.neuroforged.leadsystem.service;

/**
 * LSB-164: real password-reset flow.
 *
 * <p>{@link #requestReset} is intentionally side-effect-only — it always returns
 * silently so attackers can't probe for valid emails (account enumeration).
 *
 * <p>{@link #completeReset} validates the token, updates the user's password, and
 * marks the token used. Throws {@link InvalidResetTokenException} on any failure.
 */
public interface PasswordResetService {

    /**
     * Issue a reset token (if the email exists), persist it hashed, and email
     * the user. Always returns silently — callers should send 200 regardless.
     */
    void requestReset(String email);

    /**
     * Validate the token and set the user's password to {@code newPassword}.
     *
     * @throws InvalidResetTokenException if the token is unknown, expired, or already used
     */
    void completeReset(String token, String newPassword);

    /** Thrown when a reset token is unknown, expired, or already used. */
    class InvalidResetTokenException extends RuntimeException {
        public InvalidResetTokenException(String message) { super(message); }
    }
}

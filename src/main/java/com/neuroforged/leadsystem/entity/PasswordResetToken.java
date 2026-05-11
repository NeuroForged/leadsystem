package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * LSB-164: single-use, time-bounded token issued by /auth/forgot-password and
 * consumed by /auth/reset-password.
 *
 * <p>{@code tokenHash} is the SHA-256 hex of the URL-safe random token sent in
 * the email. The plaintext token is never stored. {@code usedAt} flips on
 * successful consumption — re-use returns {@code 400 Token invalid or expired}.
 */
@Entity
@Table(
        name = "password_reset_token",
        indexes = {
                @Index(name = "idx_password_reset_token_email",      columnList = "user_email"),
                @Index(name = "idx_password_reset_token_expires_at", columnList = "expires_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 254)
    private String userEmail;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

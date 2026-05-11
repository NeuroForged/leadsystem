-- LSB-164: real password-reset flow. Replaces the stub controller methods.
--
-- Tokens are stored hashed (SHA-256) so a DB leak doesn't compromise pending
-- resets. Single-use: `used_at` is set on consumption and any subsequent
-- attempt with the same token is rejected.

CREATE TABLE password_reset_token (
    id           BIGSERIAL PRIMARY KEY,
    user_email   VARCHAR(254) NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    expires_at   TIMESTAMP    NOT NULL,
    used_at      TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_token_email      ON password_reset_token (user_email);
CREATE INDEX idx_password_reset_token_expires_at ON password_reset_token (expires_at);

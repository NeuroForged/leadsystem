-- V5: Add client_id to users table for CLIENT role scoping (PORTAL-82)
-- Nullable: only CLIENT users have a clientId; ADMIN users remain null.

ALTER TABLE users ADD COLUMN IF NOT EXISTS client_id BIGINT;

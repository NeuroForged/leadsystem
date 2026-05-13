-- LSB-170 / Phase 01 Story 1.1 Path C
-- Adds a product-mode flag to client so the chatbot UI can render company- vs
-- agency-mode features per tenant. Single source of truth: chatbot reads this
-- value from a JWT claim populated at login/refresh time.

ALTER TABLE client
    ADD COLUMN IF NOT EXISTS mode VARCHAR(20) NOT NULL DEFAULT 'COMPANY';

ALTER TABLE client
    DROP CONSTRAINT IF EXISTS client_mode_check;
ALTER TABLE client
    ADD CONSTRAINT client_mode_check CHECK (mode IN ('COMPANY', 'AGENCY'));

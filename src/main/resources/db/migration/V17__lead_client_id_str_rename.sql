-- LSB-89 Phase 1: rename lead.client_id (TEXT) → client_id_str, add client_id (BIGINT FK)

-- Drop old constraints referencing the TEXT column
ALTER TABLE lead DROP CONSTRAINT IF EXISTS uk_lead_email_client;
DROP INDEX IF EXISTS idx_client_id;

-- Rename the legacy text column
ALTER TABLE lead RENAME COLUMN client_id TO client_id_str;

-- Recreate index + unique constraint on the renamed column
CREATE INDEX idx_lead_client_id_str ON lead(client_id_str);
ALTER TABLE lead ADD CONSTRAINT uk_lead_email_client UNIQUE (email, client_id_str);

-- Backfill: set client_id FK where client_id_str is a parseable Long and the client exists
ALTER TABLE lead ADD COLUMN IF NOT EXISTS client_id BIGINT REFERENCES client(id);

UPDATE lead
SET client_id = CAST(client_id_str AS BIGINT)
WHERE client_id_str ~ '^\d+$'
  AND CAST(client_id_str AS BIGINT) IN (SELECT id FROM client);

CREATE INDEX IF NOT EXISTS idx_lead_client_id_fk ON lead(client_id);

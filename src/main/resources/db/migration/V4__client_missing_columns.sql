-- V4: Add columns to client table that existed on dev (via ddl-auto:update) but not prod
-- Prod was last deployed pre-LSB-86; api_key and last_scraped_at were never applied via migration

-- api_key: add nullable, backfill with UUIDs, then enforce NOT NULL + UNIQUE
ALTER TABLE client ADD COLUMN IF NOT EXISTS api_key VARCHAR(64);
UPDATE client SET api_key = REPLACE(gen_random_uuid()::text, '-', '') WHERE api_key IS NULL;
ALTER TABLE client ALTER COLUMN api_key SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'client_api_key_unique'
    ) THEN
        ALTER TABLE client ADD CONSTRAINT client_api_key_unique UNIQUE (api_key);
    END IF;
END$$;

-- last_scraped_at: nullable, no backfill needed
ALTER TABLE client ADD COLUMN IF NOT EXISTS last_scraped_at TIMESTAMP;

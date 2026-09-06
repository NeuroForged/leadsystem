-- OAuth states must expire. Existing rows are back-dated so every state issued before this
-- migration is already invalid (they were never single-use either).
ALTER TABLE calendly_integration ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();
UPDATE calendly_integration SET created_at = now() - INTERVAL '1 day';

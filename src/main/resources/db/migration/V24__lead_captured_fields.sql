-- BUG-3: keep everything the chatbot captured, not just the agency-template projection.
ALTER TABLE lead ADD COLUMN IF NOT EXISTS captured_fields JSONB;

ALTER TABLE calendly_meeting
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(50);

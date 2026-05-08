-- LSB-22: Meeting notes from Fireflies.ai webhook integration

CREATE TABLE meeting_note (
    id            BIGSERIAL PRIMARY KEY,
    meeting_id    BIGINT       NOT NULL REFERENCES calendly_meeting(id) ON DELETE CASCADE,
    fireflies_id  VARCHAR(255) UNIQUE,
    summary       TEXT,
    transcript    TEXT,
    received_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_meeting_note_meeting ON meeting_note(meeting_id);

-- Per-client Fireflies webhook secret for signature verification
ALTER TABLE client
    ADD COLUMN IF NOT EXISTS fireflies_webhook_secret VARCHAR(200);

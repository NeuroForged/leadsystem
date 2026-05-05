-- V6: Add UNIQUE constraint on calendly_meeting.calendly_uri (LSB-102)
-- Provides DB-level idempotency so duplicate webhook deliveries cannot
-- insert duplicate meeting rows even under concurrent retry conditions.

ALTER TABLE calendly_meeting
    ADD CONSTRAINT uq_calendly_meeting_uri UNIQUE (calendly_uri);

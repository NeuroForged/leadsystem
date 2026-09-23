-- The chatbot session that produced a lead (so the Portal can show the conversation on the lead),
-- and a plain-English explanation of the lead's score.
ALTER TABLE lead ADD COLUMN IF NOT EXISTS chat_session_id VARCHAR(64);
ALTER TABLE lead ADD COLUMN IF NOT EXISTS score_basis VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_lead_chat_session_id ON lead (chat_session_id);

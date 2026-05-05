CREATE TABLE IF NOT EXISTS lead_comment (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    author_role VARCHAR(50) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_comment_lead_id ON lead_comment(lead_id);

-- V3: Client knowledge base documents (scraped markdown content stored per client)

CREATE TABLE IF NOT EXISTS knowledge_base_document (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT REFERENCES client (id),
    scrape_job_id   BIGINT REFERENCES scrape_job (id),
    filename        VARCHAR(255) NOT NULL,
    content         TEXT,
    word_count      INTEGER,
    fetched_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_doc_client ON knowledge_base_document (client_id);

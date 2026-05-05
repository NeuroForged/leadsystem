-- V2: Add invitee_name to calendly_meeting (missed in V1 baseline), scrape_job, scrape_preset tables

ALTER TABLE calendly_meeting ADD COLUMN IF NOT EXISTS invitee_name VARCHAR(255);

CREATE TABLE IF NOT EXISTS scrape_job (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT REFERENCES client (id),
    scraper_job_id  VARCHAR(255),
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    url             VARCHAR(500) NOT NULL,
    max_pages       INTEGER      NOT NULL DEFAULT 1000,
    initiated_by    VARCHAR(255),
    created_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    scraped_count   INTEGER,
    error_count     INTEGER,
    files           VARCHAR(500),
    error_message   VARCHAR(2000),
    reused          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_scrape_job_client ON scrape_job (client_id);

CREATE TABLE IF NOT EXISTS scrape_preset (
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT REFERENCES client (id),
    name        VARCHAR(255) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    max_pages   INTEGER      NOT NULL DEFAULT 1000,
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scrape_preset_client ON scrape_preset (client_id);

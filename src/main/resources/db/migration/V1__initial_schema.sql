-- V1: Initial schema — baseline for all tables created by Hibernate ddl-auto:update
-- This migration runs on fresh databases. Existing prod databases are baselied via
-- spring.flyway.baseline-on-migrate=true (Flyway records V1 as applied without running it).

CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL PRIMARY KEY,
    email    VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS client (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255),
    primary_email       VARCHAR(255),
    notification_emails VARCHAR(255),
    website_url         VARCHAR(255),
    api_key             VARCHAR(64) NOT NULL UNIQUE,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    last_scraped_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(255),
    email           VARCHAR(255),
    business_name   VARCHAR(255),
    business_type   VARCHAR(255),
    customer_type   VARCHAR(255),
    traffic_source  VARCHAR(255),
    monthly_leads   INTEGER,
    conversion_rate DOUBLE PRECISION,
    cost_per_lead   DOUBLE PRECISION,
    client_value    DOUBLE PRECISION,
    lead_score      INTEGER,
    lead_challenge  VARCHAR(1000),
    client_id       VARCHAR(255),
    status          VARCHAR(255),
    created_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_client_id   ON lead (client_id);
CREATE INDEX IF NOT EXISTS idx_lead_score  ON lead (lead_score);
ALTER TABLE lead DROP CONSTRAINT IF EXISTS uk_lead_email_client;
ALTER TABLE lead ADD CONSTRAINT uk_lead_email_client UNIQUE (email, client_id);

CREATE TABLE IF NOT EXISTS calendly_account (
    id               BIGSERIAL PRIMARY KEY,
    access_token     VARCHAR(255),
    refresh_token    VARCHAR(255),
    owner            VARCHAR(255),
    owner_type       VARCHAR(255),
    organization     VARCHAR(255),
    client_id        BIGINT UNIQUE,
    token_issued_at  TIMESTAMP,
    requires_reauth  BOOLEAN NOT NULL DEFAULT FALSE,
    use_polling      BOOLEAN NOT NULL DEFAULT FALSE,
    last_polled_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS calendly_meeting (
    id             BIGSERIAL PRIMARY KEY,
    calendly_uri   VARCHAR(255),
    event_type     VARCHAR(255),
    start_time     TIMESTAMPTZ,
    end_time       TIMESTAMPTZ,
    invitee_email  VARCHAR(255),
    invitee_name   VARCHAR(255),
    status         VARCHAR(255),
    client_id      BIGINT REFERENCES client (id)
);

CREATE TABLE IF NOT EXISTS calendly_webhook_log (
    id             BIGSERIAL PRIMARY KEY,
    payload        VARCHAR(10000),
    headers        VARCHAR(5000),
    event          VARCHAR(255),
    success        BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    received_at    TIMESTAMPTZ,
    error_details  VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS calendly_integration (
    id        BIGSERIAL PRIMARY KEY,
    state     VARCHAR(255),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    client_id BIGINT REFERENCES client (id)
);

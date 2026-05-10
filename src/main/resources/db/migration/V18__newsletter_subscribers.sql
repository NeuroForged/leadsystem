-- LSB-152: Newsletter subscriber table for marketing website signups.
CREATE TABLE newsletter_subscriber (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    source        VARCHAR(60),
    subscribed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_newsletter_subscriber_email ON newsletter_subscriber (email);

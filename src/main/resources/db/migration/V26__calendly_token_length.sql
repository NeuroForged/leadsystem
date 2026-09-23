-- The entity has always declared 2000 characters for the encrypted Calendly tokens; the table
-- was created with 255. An encrypted Calendly token (OAuth or personal access token) is longer
-- than 255, so connecting Calendly failed with "value too long for type character varying(255)".
ALTER TABLE calendly_account ALTER COLUMN access_token TYPE VARCHAR(2000);
ALTER TABLE calendly_account ALTER COLUMN refresh_token TYPE VARCHAR(2000);

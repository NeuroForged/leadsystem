ALTER TABLE client
    ADD COLUMN IF NOT EXISTS scrape_frequency_days INTEGER;

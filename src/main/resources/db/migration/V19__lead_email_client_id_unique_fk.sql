-- LSB-153: flip uk_lead_email_client from client_id_str (TEXT) to client_id (BIGINT FK)
--
-- After LSB-89 phase 2, the application-level duplicate check uses
-- existsByEmailAndClient_Id (BIGINT FK), but the DB unique constraint
-- was still on (email, client_id_str). This migration aligns the DB with the app.

-- Drop the legacy TEXT-column constraint
ALTER TABLE lead DROP CONSTRAINT IF EXISTS uk_lead_email_client;

-- Add the new FK-based constraint
-- Partial unique index (only where client_id IS NOT NULL) so legacy rows
-- with NULL client_id remain valid until LSB-155 drops client_id_str.
CREATE UNIQUE INDEX uk_lead_email_client
    ON lead (email, client_id)
    WHERE client_id IS NOT NULL;

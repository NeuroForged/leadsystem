-- LSB-155: drop the legacy client_id_str column.
--
-- After LSB-89 (rename + FK backfill) and LSB-153 (unique constraint flip to
-- client_id FK), client_id_str is no longer used by the application.
-- LeadMapper now derives LeadResponseDTO.clientId from client.id directly.
--
-- Prerequisite: V19 must run first (it removes the only constraint that
-- referenced client_id_str — uk_lead_email_client).

-- Drop the obsolete index
DROP INDEX IF EXISTS idx_lead_client_id_str;

-- Drop the column itself
ALTER TABLE lead DROP COLUMN IF EXISTS client_id_str;

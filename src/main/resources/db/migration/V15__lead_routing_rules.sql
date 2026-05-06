ALTER TABLE lead ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(255);

CREATE TABLE lead_routing_rule (
    id           BIGSERIAL PRIMARY KEY,
    client_id    BIGINT NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    match_field  VARCHAR(100) NOT NULL,
    match_value  VARCHAR(255) NOT NULL,
    assign_to    VARCHAR(255) NOT NULL,
    priority     INT NOT NULL DEFAULT 100,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_routing_rule_client ON lead_routing_rule(client_id);

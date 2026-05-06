CREATE TABLE audit_event (
    id           BIGSERIAL PRIMARY KEY,
    actor_email  VARCHAR(255),
    action       VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    VARCHAR(100),
    request_path VARCHAR(500),
    http_method  VARCHAR(10),
    diff_json    TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor     ON audit_event(actor_email);
CREATE INDEX idx_audit_entity    ON audit_event(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_event(created_at);

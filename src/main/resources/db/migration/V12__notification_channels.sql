CREATE TABLE notification_channel (
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    channel_type VARCHAR(20) NOT NULL,
    destination VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE notification_channel_events (
    notification_channel_id BIGINT NOT NULL REFERENCES notification_channel(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (notification_channel_id, event_type)
);

CREATE INDEX idx_notif_channel_client ON notification_channel(client_id);

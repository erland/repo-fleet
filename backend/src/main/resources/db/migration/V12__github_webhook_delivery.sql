CREATE TABLE github_webhook_delivery (
    id BIGSERIAL PRIMARY KEY,
    delivery_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(128) NOT NULL,
    event_support VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_github_webhook_delivery_event_type
    ON github_webhook_delivery (event_type);

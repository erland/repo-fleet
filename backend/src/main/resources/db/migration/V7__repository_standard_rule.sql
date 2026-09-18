CREATE TABLE repository_standard_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_key VARCHAR(128) NOT NULL UNIQUE,
    rule_type VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parameters_json TEXT NOT NULL DEFAULT '{}',
    scope VARCHAR(64) NOT NULL DEFAULT 'ALL_REPOSITORIES',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_repository_standard_rule_enabled
    ON repository_standard_rule (enabled);

CREATE TABLE repository_compliance_exception (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL,
    rule_key VARCHAR(128) NOT NULL,
    reason TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_repository_compliance_exception UNIQUE (github_repository_id, rule_key),
    CONSTRAINT fk_repository_compliance_exception_repository
        FOREIGN KEY (github_repository_id)
        REFERENCES repository_identity (github_repository_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_repository_compliance_exception_rule
        FOREIGN KEY (rule_key)
        REFERENCES repository_standard_rule (rule_key)
        ON DELETE CASCADE
);

CREATE INDEX idx_repository_compliance_exception_state
    ON repository_compliance_exception (state);

CREATE INDEX idx_repository_compliance_exception_repository
    ON repository_compliance_exception (github_repository_id);

CREATE INDEX idx_repository_compliance_exception_rule
    ON repository_compliance_exception (rule_key);

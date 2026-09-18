CREATE TABLE repository_compliance_result (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL,
    rule_key VARCHAR(128) NOT NULL,
    result VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    observed_value TEXT,
    evaluated_at TIMESTAMPTZ NOT NULL,
    source_updated_at TIMESTAMPTZ,
    rule_updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_repository_compliance_result UNIQUE (github_repository_id, rule_key),
    CONSTRAINT fk_repository_compliance_result_repository
        FOREIGN KEY (github_repository_id)
        REFERENCES repository_identity (github_repository_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_repository_compliance_result_rule
        FOREIGN KEY (rule_key)
        REFERENCES repository_standard_rule (rule_key)
        ON DELETE CASCADE
);

CREATE INDEX idx_repository_compliance_result_repository
    ON repository_compliance_result (github_repository_id);

CREATE INDEX idx_repository_compliance_result_rule
    ON repository_compliance_result (rule_key);

CREATE INDEX idx_repository_compliance_result_result
    ON repository_compliance_result (result);

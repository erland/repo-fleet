CREATE TABLE repository_rule_group_assignment (
    id BIGSERIAL PRIMARY KEY,
    rule_key VARCHAR(128) NOT NULL,
    group_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_repository_rule_group_assignment UNIQUE (rule_key, group_key),
    CONSTRAINT fk_repository_rule_group_assignment_rule
        FOREIGN KEY (rule_key) REFERENCES repository_standard_rule (rule_key) ON DELETE CASCADE,
    CONSTRAINT fk_repository_rule_group_assignment_group
        FOREIGN KEY (group_key) REFERENCES repository_group (group_key) ON DELETE CASCADE
);

CREATE INDEX idx_repository_rule_group_assignment_rule
    ON repository_rule_group_assignment (rule_key);

CREATE INDEX idx_repository_rule_group_assignment_group
    ON repository_rule_group_assignment (group_key);

CREATE TABLE github_conditional_request_state (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL,
    resource_category VARCHAR(64) NOT NULL,
    etag VARCHAR(512),
    last_successful_fetch_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_github_conditional_request_state
        UNIQUE (github_repository_id, resource_category),
    CONSTRAINT fk_github_conditional_request_state_repository
        FOREIGN KEY (github_repository_id)
        REFERENCES repository_identity (github_repository_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_github_conditional_request_state_repository
    ON github_conditional_request_state (github_repository_id);

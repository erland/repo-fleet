ALTER TABLE repository_refresh_run
    ADD COLUMN reused_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN scheduled_count INTEGER NOT NULL DEFAULT 0;

CREATE TABLE github_api_diagnostics (
    id BIGINT PRIMARY KEY,
    conditional_modified_count BIGINT NOT NULL DEFAULT 0,
    conditional_not_modified_count BIGINT NOT NULL DEFAULT 0,
    conditional_cached_fresh_count BIGINT NOT NULL DEFAULT 0,
    rate_limit_remaining INTEGER,
    rate_limit_reset_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO github_api_diagnostics (
    id,
    conditional_modified_count,
    conditional_not_modified_count,
    conditional_cached_fresh_count,
    updated_at)
VALUES (1, 0, 0, 0, NOW());

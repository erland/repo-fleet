CREATE TABLE repository_refresh_run (
    id BIGSERIAL PRIMARY KEY,
    trigger_type VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    final_state VARCHAR(32) NOT NULL,
    discovered_count INTEGER NOT NULL DEFAULT 0,
    processed_count INTEGER NOT NULL DEFAULT 0,
    successful_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    failed_repository_summary TEXT,
    rate_limit_remaining INTEGER,
    rate_limit_reset_at TIMESTAMPTZ
);

CREATE INDEX idx_repository_refresh_run_started_at
    ON repository_refresh_run (started_at DESC);

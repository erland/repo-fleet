CREATE TABLE repository_refresh_job (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_repository_refresh_job_repository
      FOREIGN KEY (github_repository_id)
      REFERENCES repository_identity (github_repository_id)
      ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_repository_refresh_job_active
  ON repository_refresh_job (github_repository_id)
  WHERE state IN ('PENDING','RUNNING','RETRY');

CREATE INDEX idx_repository_refresh_job_due
  ON repository_refresh_job (state, next_attempt_at);

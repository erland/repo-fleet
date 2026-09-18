CREATE TABLE repository_identity (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL UNIQUE,
    owner_login VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(512) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    fork BOOLEAN NOT NULL DEFAULT FALSE,
    default_branch VARCHAR(255),
    github_updated_at TIMESTAMPTZ,
    github_pushed_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_repository_identity_full_name
    ON repository_identity (full_name);

CREATE INDEX idx_repository_identity_active
    ON repository_identity (active);

CREATE TABLE repository_enrichment_snapshot (
    id BIGSERIAL PRIMARY KEY,
    github_repository_id BIGINT NOT NULL UNIQUE,
    topics_json TEXT NOT NULL DEFAULT '[]',
    languages_json TEXT NOT NULL DEFAULT '[]',
    primary_language VARCHAR(255),
    license_analysis_state VARCHAR(32) NOT NULL,
    license_presence VARCHAR(32) NOT NULL,
    license_recognized BOOLEAN,
    license_key VARCHAR(255),
    license_name VARCHAR(255),
    actions_analysis_state VARCHAR(32) NOT NULL,
    workflows_present BOOLEAN,
    workflow_count INTEGER,
    release_analysis_state VARCHAR(32) NOT NULL,
    release_present BOOLEAN,
    latest_release_name VARCHAR(512),
    latest_release_tag VARCHAR(255),
    latest_release_date TIMESTAMPTZ,
    latest_release_prerelease BOOLEAN,
    activity_pushed_at TIMESTAMPTZ,
    activity_updated_at TIMESTAMPTZ,
    enrichment_state VARCHAR(32) NOT NULL,
    enrichment_message TEXT,
    last_successful_refresh_at TIMESTAMPTZ,
    last_relevant_error TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repository_enrichment_snapshot_repository
        FOREIGN KEY (github_repository_id)
        REFERENCES repository_identity (github_repository_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_repository_enrichment_snapshot_state
    ON repository_enrichment_snapshot (enrichment_state);

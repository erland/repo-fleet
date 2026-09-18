ALTER TABLE repository_identity
    ADD COLUMN change_classification VARCHAR(32),
    ADD COLUMN change_detected_at TIMESTAMPTZ;

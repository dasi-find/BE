ALTER TABLE candidate
    ADD COLUMN feedback VARCHAR(20) NULL,
    ADD COLUMN viewed_at DATETIME(6) NULL,
    ADD COLUMN notification_suppressed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN assessed_analysis_id BIGINT NULL,
    ADD COLUMN assessed_police_item_version BIGINT NULL,
    ADD CONSTRAINT ck_candidate_feedback CHECK (feedback IN ('VERY_SIMILAR', 'UNSURE', 'NOT_MINE')),
    ADD CONSTRAINT ck_candidate_suppressed CHECK (notification_suppressed IN (0, 1)),
    ADD CONSTRAINT ck_candidate_not_mine CHECK (feedback IS NULL OR feedback <> 'NOT_MINE' OR notification_suppressed = TRUE),
    ADD CONSTRAINT ck_candidate_input_version CHECK (
        (assessed_analysis_id IS NULL AND assessed_police_item_version IS NULL)
        OR (assessed_analysis_id IS NOT NULL AND assessed_analysis_id > 0
            AND assessed_police_item_version IS NOT NULL AND assessed_police_item_version >= 0)
    );

-- Legacy assessments intentionally remain unversioned until recomputed.
-- No FK to the old analysis: a search-card edit deletes that analysis.

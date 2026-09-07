ALTER TABLE search_card
    ADD COLUMN close_reason VARCHAR(40) NULL AFTER search_expires_at,
    ADD COLUMN closed_at DATETIME(6) NULL AFTER close_reason,
    ADD CONSTRAINT ck_search_card_close_reason CHECK (
        close_reason IS NULL OR close_reason IN (
            'FOUND_BY_RECOMMENDATION',
            'FOUND_OTHER_WAY',
            'SEARCH_STOPPED'
        )
    );

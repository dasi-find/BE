CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    candidate_id BIGINT NULL,
    search_card_id BIGINT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_candidate FOREIGN KEY (candidate_id) REFERENCES candidate (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_card FOREIGN KEY (search_card_id) REFERENCES search_card (id) ON DELETE CASCADE,
    CONSTRAINT ck_notification_type CHECK (type IN ('NEW_CANDIDATE', 'SEARCH_EXPIRING', 'SEARCH_EXPIRED', 'SYSTEM')),
    CONSTRAINT ck_notification_reference CHECK (
        (type = 'NEW_CANDIDATE' AND candidate_id IS NOT NULL AND search_card_id IS NULL)
        OR (type IN ('SEARCH_EXPIRING', 'SEARCH_EXPIRED') AND search_card_id IS NOT NULL AND candidate_id IS NULL)
        OR (type = 'SYSTEM' AND candidate_id IS NULL AND search_card_id IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC, id DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id, read_at, created_at DESC, id DESC);

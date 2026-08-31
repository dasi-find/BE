CREATE TABLE search_card_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    search_card_id BIGINT NULL,
    storage_key VARCHAR(500) NOT NULL,
    image_type VARCHAR(20) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_search_card_image PRIMARY KEY (id),
    CONSTRAINT uk_search_card_image_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_search_card_image_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT ck_search_card_image_type CHECK (image_type IN ('ACTUAL', 'REFERENCE')),
    CONSTRAINT ck_search_card_image_file_size CHECK (file_size > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_search_card_image_user_id
    ON search_card_image (user_id);

CREATE INDEX idx_search_card_image_search_card_id
    ON search_card_image (search_card_id);

CREATE INDEX idx_search_card_image_orphan_cleanup
    ON search_card_image (search_card_id, created_at);

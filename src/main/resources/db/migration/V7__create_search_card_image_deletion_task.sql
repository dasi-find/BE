CREATE TABLE search_card_image_deletion_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    storage_key VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_search_card_image_deletion_task PRIMARY KEY (id),
    CONSTRAINT uk_search_card_image_deletion_task_storage_key UNIQUE (storage_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE search_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    analysis_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NULL,
    material VARCHAR(100) NULL,
    feature_description TEXT NOT NULL,
    lost_date DATE NOT NULL,
    lost_start_time TIME NULL,
    lost_end_time TIME NULL,
    status VARCHAR(20) NOT NULL,
    search_expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_search_card PRIMARY KEY (id),
    CONSTRAINT uk_search_card_analysis UNIQUE (analysis_id),
    CONSTRAINT fk_search_card_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_search_card_analysis
        FOREIGN KEY (analysis_id) REFERENCES search_card_analysis (id),
    CONSTRAINT ck_search_card_status
        CHECK (status IN ('ACTIVE', 'FOUND', 'CLOSED', 'EXPIRED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_search_card_user_status
    ON search_card (user_id, status);

CREATE INDEX idx_search_card_status_expires_at
    ON search_card (status, search_expires_at);

CREATE TABLE search_card_color (
    search_card_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    color VARCHAR(50) NOT NULL,
    CONSTRAINT pk_search_card_color PRIMARY KEY (search_card_id, sort_order),
    CONSTRAINT fk_search_card_color_search_card
        FOREIGN KEY (search_card_id) REFERENCES search_card (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE lost_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    search_card_id BIGINT NOT NULL,
    place_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_lost_location PRIMARY KEY (id),
    CONSTRAINT uk_lost_location_search_card UNIQUE (search_card_id),
    CONSTRAINT fk_lost_location_search_card
        FOREIGN KEY (search_card_id) REFERENCES search_card (id) ON DELETE CASCADE,
    CONSTRAINT ck_lost_location_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_lost_location_longitude CHECK (longitude BETWEEN -180 AND 180)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE search_card_image
    ADD CONSTRAINT fk_search_card_image_search_card
        FOREIGN KEY (search_card_id) REFERENCES search_card (id) ON DELETE CASCADE;

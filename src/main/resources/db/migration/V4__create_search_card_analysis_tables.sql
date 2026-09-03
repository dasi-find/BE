CREATE TABLE search_card_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NULL,
    ocr_text VARCHAR(1000) NULL,
    model_version VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_search_card_analysis PRIMARY KEY (id),
    CONSTRAINT fk_search_card_analysis_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_search_card_analysis_user_id
    ON search_card_analysis (user_id);

CREATE TABLE search_card_analysis_color (
    analysis_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    color VARCHAR(50) NOT NULL,
    CONSTRAINT pk_search_card_analysis_color PRIMARY KEY (analysis_id, sort_order),
    CONSTRAINT fk_search_card_analysis_color_analysis
        FOREIGN KEY (analysis_id) REFERENCES search_card_analysis (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE search_card_analysis_material (
    analysis_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    material VARCHAR(50) NOT NULL,
    CONSTRAINT pk_search_card_analysis_material PRIMARY KEY (analysis_id, sort_order),
    CONSTRAINT fk_search_card_analysis_material_analysis
        FOREIGN KEY (analysis_id) REFERENCES search_card_analysis (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE search_card_analysis_feature (
    analysis_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    feature VARCHAR(500) NOT NULL,
    CONSTRAINT pk_search_card_analysis_feature PRIMARY KEY (analysis_id, sort_order),
    CONSTRAINT fk_search_card_analysis_feature_analysis
        FOREIGN KEY (analysis_id) REFERENCES search_card_analysis (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE police_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(20) NOT NULL,
    management_no VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    item_sequence INT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NULL,
    color VARCHAR(100) NULL,
    description TEXT NULL,
    found_date DATE NULL,
    found_place VARCHAR(500) NULL,
    storage_place VARCHAR(200) NULL,
    image_url VARCHAR(2048) NULL,
    original_url VARCHAR(2048) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_police_item PRIMARY KEY (id),
    CONSTRAINT uk_police_item_source_key UNIQUE (source, management_no, item_sequence),
    CONSTRAINT ck_police_item_source CHECK (source IN ('POLICE', 'PORTAL')),
    CONSTRAINT ck_police_item_sequence CHECK (item_sequence >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE candidate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    search_card_id BIGINT NOT NULL,
    police_item_id BIGINT NOT NULL,
    image_score DECIMAL(7,6) NULL,
    text_score DECIMAL(7,6) NULL,
    image_text_score DECIMAL(7,6) NULL,
    attribute_score DECIMAL(7,6) NULL,
    date_score DECIMAL(7,6) NULL,
    location_score DECIMAL(7,6) NULL,
    total_score DECIMAL(7,4) NULL,
    evidence_coverage DECIMAL(7,6) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    preprocessing_version VARCHAR(100) NOT NULL,
    score_policy_version VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_candidate PRIMARY KEY (id),
    CONSTRAINT uk_candidate_card_item UNIQUE (search_card_id, police_item_id),
    CONSTRAINT fk_candidate_card FOREIGN KEY (search_card_id) REFERENCES search_card (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_item FOREIGN KEY (police_item_id) REFERENCES police_item (id),
    CONSTRAINT ck_candidate_image CHECK (image_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_text CHECK (text_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_image_text CHECK (image_text_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_attribute CHECK (attribute_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_date CHECK (date_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_location CHECK (location_score BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_total CHECK (total_score BETWEEN 0 AND 100),
    CONSTRAINT ck_candidate_coverage CHECK (evidence_coverage BETWEEN 0 AND 1),
    CONSTRAINT ck_candidate_missing CHECK (
        image_score IS NOT NULL OR text_score IS NOT NULL OR image_text_score IS NOT NULL
        OR attribute_score IS NOT NULL OR date_score IS NOT NULL OR location_score IS NOT NULL
        OR (total_score IS NULL AND evidence_coverage = 0)
    ),
    CONSTRAINT ck_candidate_total_evidence CHECK (total_score IS NULL OR evidence_coverage > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_candidate_card_score ON candidate (search_card_id, total_score DESC, id);

CREATE TABLE candidate_evidence (
    candidate_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    evidence_type VARCHAR(20) NOT NULL,
    score_element VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    CONSTRAINT pk_candidate_evidence PRIMARY KEY (candidate_id, sort_order),
    CONSTRAINT fk_candidate_evidence_candidate FOREIGN KEY (candidate_id) REFERENCES candidate (id) ON DELETE CASCADE,
    CONSTRAINT ck_candidate_evidence_type CHECK (evidence_type IN ('MATCH', 'CONFLICT', 'MISSING')),
    CONSTRAINT ck_candidate_evidence_element CHECK (
        score_element IN ('IMAGE', 'TEXT', 'IMAGE_TEXT', 'ATTRIBUTE', 'DATE', 'LOCATION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

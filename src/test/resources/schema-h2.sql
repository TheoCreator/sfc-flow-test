DROP TABLE IF EXISTS ms_review_record;
DROP TABLE IF EXISTS ms_manuscript_asset;
DROP TABLE IF EXISTS ms_manuscript;

CREATE TABLE ms_manuscript (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    body CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    reject_review_level TINYINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_ms_manuscript_status_updated ON ms_manuscript(status, updated_at);

CREATE TABLE ms_manuscript_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manuscript_id BIGINT NOT NULL,
    storage_object_key VARCHAR(191) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ms_asset_manuscript FOREIGN KEY (manuscript_id) REFERENCES ms_manuscript(id) ON DELETE CASCADE
);

CREATE INDEX idx_ms_asset_manuscript ON ms_manuscript_asset(manuscript_id);

CREATE TABLE ms_review_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manuscript_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    opinion VARCHAR(2000),
    reject_level TINYINT,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ms_review_manuscript FOREIGN KEY (manuscript_id) REFERENCES ms_manuscript(id) ON DELETE CASCADE
);

CREATE INDEX idx_ms_review_manuscript_time ON ms_review_record(manuscript_id, created_at);

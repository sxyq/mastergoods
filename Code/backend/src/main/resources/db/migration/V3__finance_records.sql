CREATE TABLE IF NOT EXISTS finance_records (
    id BIGINT PRIMARY KEY,
    record_no VARCHAR(64) NOT NULL UNIQUE,
    type INT NOT NULL,
    category VARCHAR(64) NOT NULL,
    partner_name VARCHAR(128),
    amount DOUBLE PRECISION NOT NULL,
    method INT NOT NULL DEFAULT 1,
    notes VARCHAR(255),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_finance_records_created_at ON finance_records(created_at);
CREATE INDEX IF NOT EXISTS idx_finance_records_type ON finance_records(type);
CREATE INDEX IF NOT EXISTS idx_finance_records_partner_name ON finance_records(partner_name);

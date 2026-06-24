CREATE INDEX idx_import_jobs_status_updated_created_id
    ON import_jobs (status, updated_at, created_at, id);

CREATE INDEX idx_import_jobs_status_last_heartbeat_updated
    ON import_jobs (status, last_heartbeat_at, updated_at);

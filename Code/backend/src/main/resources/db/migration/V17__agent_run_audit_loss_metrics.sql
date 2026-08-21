ALTER TABLE agent_run_audits
    ADD COLUMN IF NOT EXISTS audit_write_dropped_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE agent_run_audits
    ADD COLUMN IF NOT EXISTS audit_write_failed_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE agent_run_audits
    ADD COLUMN IF NOT EXISTS audit_lossy BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE agent_run_audits
    ADD COLUMN IF NOT EXISTS emitted_event_count INTEGER NOT NULL DEFAULT 0;

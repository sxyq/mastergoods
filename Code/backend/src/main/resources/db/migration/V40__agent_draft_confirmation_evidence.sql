-- Persist the evidence produced when an Agent draft is confirmed or fails.
-- All fields are nullable for drafts created before this migration.
ALTER TABLE agent_drafts ADD COLUMN IF NOT EXISTS confirmed_by BIGINT;
ALTER TABLE agent_drafts ADD COLUMN IF NOT EXISTS confirmed_at BIGINT;
ALTER TABLE agent_drafts ADD COLUMN IF NOT EXISTS business_reference VARCHAR(128);
ALTER TABLE agent_drafts ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(512);

ALTER TABLE agent_drafts
    ADD CONSTRAINT fk_agent_drafts_confirmed_by
    FOREIGN KEY (confirmed_by) REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_agent_drafts_confirmation
    ON agent_drafts(confirmed_by, confirmed_at DESC, id DESC);

-- Link a draft to the Agent run that created it so confirmation evidence can
-- be traced without guessing from conversation order or payload text.
ALTER TABLE agent_drafts
    ADD COLUMN IF NOT EXISTS run_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_agent_drafts_owner_run
    ON agent_drafts(owner_user_id, run_id, id DESC);

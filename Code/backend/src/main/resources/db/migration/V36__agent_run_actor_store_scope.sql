-- These associations are nullable because historical runs have no reliable
-- actor/store source and must remain explicitly unknown.
ALTER TABLE agent_run_audits ADD COLUMN IF NOT EXISTS actor_user_id BIGINT;
ALTER TABLE agent_run_audits ADD COLUMN IF NOT EXISTS store_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_agent_run_audits_actor ON agent_run_audits(actor_user_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_run_audits_store ON agent_run_audits(store_id, started_at DESC);

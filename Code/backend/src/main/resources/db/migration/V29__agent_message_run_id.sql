ALTER TABLE agent_messages
    ADD COLUMN IF NOT EXISTS run_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_agent_messages_owner_conversation_run
    ON agent_messages(owner_user_id, conversation_id, run_id, created_at ASC, id ASC);

-- Existing messages are intentionally left unassociated. New Agent runs write
-- the run id on both the user message and the assistant terminal message.

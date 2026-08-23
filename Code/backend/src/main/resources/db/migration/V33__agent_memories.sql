-- Long term memory store scoped by owner/store. Memories are extracted
-- asynchronously after an Agent run completes and recalled by future runs
-- to inject limited context. Credentials, full auth payloads and private
-- contact fields must never be persisted here.

CREATE TABLE IF NOT EXISTS agent_memories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    store_id BIGINT,
    source_conversation_id BIGINT,
    source_message_id BIGINT,
    memory_type VARCHAR(64) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    details TEXT,
    recall_text TEXT,
    sensitivity VARCHAR(32) NOT NULL DEFAULT 'normal',
    confidence DOUBLE NOT NULL DEFAULT 0.5,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    expires_at BIGINT,
    last_accessed_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_agent_memories_owner_store_status
    ON agent_memories(owner_user_id, store_id, status);

CREATE INDEX IF NOT EXISTS idx_agent_memories_owner_status_updated
    ON agent_memories(owner_user_id, status, updated_at);

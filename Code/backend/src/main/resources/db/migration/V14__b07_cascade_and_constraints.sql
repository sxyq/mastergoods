-- B07 fix: add ON DELETE CASCADE to foreign keys for safe conversation deletion
-- and media asset -> binding cascade

-- agent_messages: drop and re-add with CASCADE
ALTER TABLE agent_messages DROP CONSTRAINT IF EXISTS fk_agent_messages_conversation;
ALTER TABLE agent_messages ADD CONSTRAINT fk_agent_messages_conversation
    FOREIGN KEY (conversation_id) REFERENCES agent_conversations(id) ON DELETE CASCADE;

-- agent_drafts: drop and re-add with CASCADE
ALTER TABLE agent_drafts DROP CONSTRAINT IF EXISTS fk_agent_drafts_conversation;
ALTER TABLE agent_drafts ADD CONSTRAINT fk_agent_drafts_conversation
    FOREIGN KEY (conversation_id) REFERENCES agent_conversations(id) ON DELETE CASCADE;

-- media_bindings: drop and re-add with CASCADE
ALTER TABLE media_bindings DROP CONSTRAINT IF EXISTS fk_media_bindings_asset;
ALTER TABLE media_bindings ADD CONSTRAINT fk_media_bindings_asset
    FOREIGN KEY (asset_id) REFERENCES media_assets(id) ON DELETE CASCADE;

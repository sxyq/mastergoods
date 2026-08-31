-- Preparation only. Execute on an approved PostgreSQL test target after service
-- and fixture access are available. This file does not contain credentials.
-- Do not treat its output as production EXPLAIN evidence without that target.

-- Confirm connection and version at execution time:
SELECT current_database() AS database_name, version() AS server_version;

-- Replace :owner_id and :store_id in the approved runner, never in this file.
-- Run EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) for owner-scoped Agent queries:
--   agent_conversations by owner/store and updated_at
--   agent_messages by conversation and created_at
--   agent_drafts by owner/store and status
--   agent_run_audits by owner/store and created_at
--   agent_context_checkpoints by owner/conversation/boundary/revision

-- Resource snapshots to pair with a controlled run:
SELECT count(*) AS active_sessions
FROM pg_stat_activity
WHERE state <> 'idle';

SELECT datname, numbackends, xact_commit, xact_rollback, blks_read, blks_hit
FROM pg_stat_database
WHERE datname = current_database();

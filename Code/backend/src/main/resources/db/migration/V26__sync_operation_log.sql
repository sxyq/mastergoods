-- V26: sync_operation_log — operationId idempotency for V2SyncService.upload
-- Ensures duplicate uploads with the same operationId are not re-applied.
CREATE TABLE IF NOT EXISTS sync_operation_log (
    owner_user_id BIGINT NOT NULL,
    operation_id  VARCHAR(128) NOT NULL,
    entity_type   VARCHAR(64)  NOT NULL,
    entity_id     VARCHAR(64)  NOT NULL,
    operation     VARCHAR(16)  NOT NULL,
    processed_at  BIGINT NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'applied',
    CONSTRAINT pk_sync_operation_log PRIMARY KEY (owner_user_id, operation_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_operation_log_owner_processed
    ON sync_operation_log (owner_user_id, processed_at);

-- V26 (cont.): sync_tombstones — tombstone records for deleted entities
-- Ensures pull can notify clients of deletions (eventual consistency).
CREATE TABLE IF NOT EXISTS sync_tombstones (
    owner_user_id BIGINT NOT NULL,
    entity_type   VARCHAR(64)  NOT NULL,
    entity_id     VARCHAR(64)  NOT NULL,
    deleted_at    BIGINT NOT NULL,
    CONSTRAINT pk_sync_tombstones PRIMARY KEY (owner_user_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_tombstones_owner_deleted
    ON sync_tombstones (owner_user_id, deleted_at);

package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V26SyncOperationLogSqlTest {
    @Test
    void migrationProvidesIdempotencyAndDeletionRecoveryStructures() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V26__sync_operation_log.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sync_operation_log"));
        assertTrue(sql.contains("PRIMARY KEY (owner_user_id, operation_id)"));
        assertTrue(sql.contains("idx_sync_operation_log_owner_processed"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sync_tombstones"));
        assertTrue(sql.contains("PRIMARY KEY (owner_user_id, entity_type, entity_id)"));
        assertTrue(sql.contains("idx_sync_tombstones_owner_deleted"));
    }
}

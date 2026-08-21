package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V27SyncChangeLogSqlTest {
    @Test
    void migrationAddsDurableStoreScopedChangeStream() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V27__sync_change_log_scope.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sync_change_log"));
        assertTrue(sql.contains("sequence_no BIGINT"));
        assertTrue(sql.contains("owner_user_id BIGINT NOT NULL"));
        assertTrue(sql.contains("store_id BIGINT NOT NULL"));
        assertTrue(sql.contains("idx_sync_change_log_owner_store_sequence"));
        assertTrue(sql.contains("idx_sync_change_log_owner_store_entity"));
        assertTrue(sql.contains("ALTER TABLE sync_operation_log ADD COLUMN IF NOT EXISTS store_id"));
        assertTrue(sql.contains("ALTER TABLE sync_tombstones ADD COLUMN IF NOT EXISTS store_id"));
    }
}

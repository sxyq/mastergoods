package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V12SyncAndImportOwnerUpgradeSqlTest {
    @Test
    void migrationContainsImportJobsStructure() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V12__sync_and_import_owner_upgrade.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS import_jobs"));
        assertTrue(sql.contains("owner_user_id BIGINT NOT NULL"));
        assertTrue(sql.contains("requested_by_user_id BIGINT NOT NULL"));
        assertTrue(sql.contains("client_id VARCHAR(128) NOT NULL"));
        assertTrue(sql.contains("idempotency_key VARCHAR(128)"));
        assertTrue(sql.contains("uk_import_jobs_owner_idempotency"));
        assertTrue(sql.contains("idx_import_jobs_owner_status"));
    }
}

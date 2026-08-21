package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V28SyncTombstoneScopeSqlTest {
    @Test
    void migrationBackfillsStoreAndMakesTombstoneKeyStoreScoped() throws Exception {
        String sql = Files.readString(Path.of(
            "src/main/resources/db/migration/V28__sync_tombstone_store_key.sql"
        ));

        assertTrue(sql.contains("ALTER COLUMN store_id SET NOT NULL"));
        assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS pk_sync_tombstones"));
        assertTrue(sql.contains(
            "PRIMARY KEY (owner_user_id, store_id, entity_type, entity_id)"
        ));
        assertTrue(sql.contains("HAVING COUNT(s.id) <> 1"));
        assertTrue(sql.contains("ambiguous or missing active store scope"));
        assertTrue(sql.contains("sync_tombstones rows without a resolvable store_id"));
    }
}

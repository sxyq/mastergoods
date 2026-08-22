package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V31PayOrderIdempotencyPayloadHashSqlTest {
    @Test
    void addsPayloadFingerprintColumnForConflictDetection() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V31__pay_order_idempotency_payload_hash.sql"));
        assertTrue(sql.contains("idempotency_payload_hash VARCHAR(64)"));
    }
}

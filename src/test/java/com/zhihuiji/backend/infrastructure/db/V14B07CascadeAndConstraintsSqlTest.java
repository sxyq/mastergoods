package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V14B07CascadeAndConstraintsSqlTest {
    @Test
    void migrationContainsExpectedCascadeConstraints() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V14__b07_cascade_and_constraints.sql"));

        assertTrue(sql.contains("fk_agent_messages_conversation"));
        assertTrue(sql.contains("fk_agent_drafts_conversation"));
        assertTrue(sql.contains("fk_media_bindings_asset"));
        assertTrue(sql.contains("ON DELETE CASCADE"));
    }
}

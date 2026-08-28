package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V34AgentDraftContentJsonTextSqlTest {
    @Test
    void migrationWidensAgentDraftContentJsonForPersistedImageResults() throws Exception {
        String sql = Files.readString(
            Path.of("src/main/resources/db/migration/V34__agent_draft_content_json_text.sql")
        );

        assertTrue(sql.contains("ALTER TABLE agent_drafts"));
        assertTrue(sql.contains("ALTER COLUMN content_json TYPE TEXT"));
    }
}

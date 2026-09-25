// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V13MediaAndAgentExpansionSqlTest {
    @Test
    void migrationContainsMediaAndAgentTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V13__media_and_agent_expansion.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS media_assets"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS media_bindings"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS agent_conversations"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS agent_messages"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS agent_drafts"));
        assertTrue(sql.contains("uk_media_assets_owner_object_key"));
        assertTrue(sql.contains("idx_agent_messages_owner_conversation_created"));
    }
}

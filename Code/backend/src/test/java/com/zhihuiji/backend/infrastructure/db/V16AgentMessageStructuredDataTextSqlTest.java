// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V16AgentMessageStructuredDataTextSqlTest {
    @Test
    void migrationWidensAgentMessageStructuredDataJson() throws Exception {
        String sql = Files.readString(
            Path.of("src/main/resources/db/migration/V16__agent_message_structured_data_text.sql")
        );

        assertTrue(sql.contains("ALTER TABLE agent_messages"));
        assertTrue(sql.contains("ALTER COLUMN structured_data_json TYPE TEXT"));
    }
}

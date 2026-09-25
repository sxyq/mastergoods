// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V43AgentDraftRunLinkSqlTest {
    @Test
    void migrationAddsOptionalRunLinkAndOwnerScopedIndex() throws Exception {
        String sql = Files.readString(Path.of(
            "src/main/resources/db/migration/V43__agent_draft_run_link.sql"
        )).toLowerCase();

        assertTrue(sql.contains("alter table agent_drafts"));
        assertTrue(sql.contains("add column if not exists run_id varchar(64)"));
        assertTrue(sql.contains("idx_agent_drafts_owner_run"));
    }
}

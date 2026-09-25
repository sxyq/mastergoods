// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V40AgentDraftConfirmationEvidenceSqlTest {
    @Test
    void migrationAddsConfirmationAndFailureEvidenceColumns() throws Exception {
        String sql = Files.readString(Path.of(
            "src/main/resources/db/migration/V40__agent_draft_confirmation_evidence.sql"
        )).toLowerCase();

        assertTrue(sql.contains("confirmed_by"));
        assertTrue(sql.contains("confirmed_at"));
        assertTrue(sql.contains("business_reference"));
        assertTrue(sql.contains("failure_reason"));
        assertTrue(sql.contains("fk_agent_drafts_confirmed_by"));
    }
}

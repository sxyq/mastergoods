package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Stage 1 field audit for the Agent run record used by the future admin read model.
 *
 * <p>This is a source-level baseline, not evidence that a production migration has
 * been applied. The negative assertions deliberately describe the current gap and
 * must be revised when a later stage adds reliable actor/store fields.</p>
 */
class AgentRunAuditSchemaStageOneBaselineTest {
    private static final Path MIGRATION =
        Path.of("src/main/resources/db/migration/V15__agent_run_audits.sql");
    private static final Path ENTITY =
        Path.of("src/main/java/com/zhihuiji/backend/domain/entity/AgentRunAuditEntity.java");
    private static final Path REPOSITORY =
        Path.of("src/main/java/com/zhihuiji/backend/infrastructure/repository/AgentRunAuditRepository.java");

    @Test
    void migrationHasOwnerAndConversationButCurrentlyLacksActorAndStore() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertTrue(sql.contains("owner_user_id"));
        assertTrue(sql.contains("conversation_id"));
        assertFalse(sql.contains("actor_user_id"));
        assertFalse(sql.contains("store_id"));
    }

    @Test
    void entityMatchesCurrentMigrationGap() throws Exception {
        String source = Files.readString(ENTITY);

        assertTrue(source.contains("@Column(name = \"owner_user_id\", nullable = false)"));
        assertTrue(source.contains("private Long conversationId;"));
        assertFalse(source.contains("actorUserId"));
        assertFalse(source.contains("storeId"));
    }

    @Test
    void scopedRunLookupExistsForAdminQueryImplementations() throws Exception {
        String source = Files.readString(REPOSITORY);

        assertTrue(source.contains("findByRunIdAndOwnerUserId"));
        assertTrue(source.contains("ownerUserId"));
    }
}

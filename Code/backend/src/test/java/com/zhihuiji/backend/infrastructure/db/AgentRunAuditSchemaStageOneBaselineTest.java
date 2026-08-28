package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Stage 1 field audit for the Agent run record used by the future admin read model.
 *
 * <p>This is a source-level baseline for the actor/store association migration.
 * Historical rows remain nullable because those associations are not recoverable.</p>
 */
class AgentRunAuditSchemaStageOneBaselineTest {
    private static final Path MIGRATION =
        Path.of("src/main/resources/db/migration/V36__agent_run_actor_store_scope.sql");
    private static final Path ENTITY =
        Path.of("src/main/java/com/zhihuiji/backend/domain/entity/AgentRunAuditEntity.java");
    private static final Path REPOSITORY =
        Path.of("src/main/java/com/zhihuiji/backend/infrastructure/repository/AgentRunAuditRepository.java");

    @Test
    void migrationAddsNullableActorAndStoreFields() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertTrue(sql.contains("actor_user_id"));
        assertTrue(sql.contains("store_id"));
        assertTrue(sql.contains("nullable"));
    }

    @Test
    void entityContainsNullableActorAndStoreFields() throws Exception {
        String source = Files.readString(ENTITY);

        assertTrue(source.contains("@Column(name = \"owner_user_id\", nullable = false)"));
        assertTrue(source.contains("private Long conversationId;"));
        assertTrue(source.contains("private Long actorUserId;"));
        assertTrue(source.contains("private Long storeId;"));
    }

    @Test
    void scopedRunLookupExistsForAdminQueryImplementations() throws Exception {
        String source = Files.readString(REPOSITORY);

        assertTrue(source.contains("findByRunIdAndOwnerUserId"));
        assertTrue(source.contains("ownerUserId"));
    }
}

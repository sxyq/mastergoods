package com.zhihuiji.backend.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * AgentContextCheckpointRepository 集成测试。
 *
 * <p>使用 H2（PostgreSQL 模式）验证真实 Spring Data 仓储行为：
 * owner 隔离、唯一约束冲突、失效后重建、失效更新、清理。
 * 生产 PostgreSQL 查询计划不在本测试范围（另行 Blocked/Deferred）。
 *
 * <p>注：实体未声明到 agent_conversations 的关联，外键 ON DELETE CASCADE 由
 * V32 迁移在数据库层定义，级联行为由 V32AgentContextCheckpointsSqlTest 验证。
 */
@DataJpaTest
class AgentContextCheckpointRepositoryTest {

    @Autowired private AgentContextCheckpointRepository checkpointRepository;
    @Autowired private AgentConversationRepository conversationRepository;
    @Autowired private EntityManager entityManager;

    private long conversationId;

    @BeforeEach
    void createConversation() {
        AgentConversationEntity conversation = new AgentConversationEntity();
        conversation.setOwnerUserId(1L);
        conversation.setTitle("测试会话");
        conversation.setStatus("active");
        conversation.setCreatedAt(1000L);
        conversation.setUpdatedAt(1000L);
        conversationId = conversationRepository.save(conversation).getId();
    }

    private AgentContextCheckpointEntity checkpoint(long boundary, int revision, String status) {
        AgentContextCheckpointEntity entity = new AgentContextCheckpointEntity();
        entity.setOwnerUserId(1L);
        entity.setConversationId(conversationId);
        entity.setSourceBoundaryMessageId(boundary);
        entity.setSourceMessageCount(6);
        entity.setSummaryBody("{\"summary_version\":1,\"confirmed_facts\":[]}");
        entity.setSummaryVersion(1);
        entity.setContextPolicyVersion(1);
        entity.setToolSchemaVersion(1);
        entity.setRevision(revision);
        entity.setQuality("deterministic");
        entity.setStatus(status);
        entity.setModelName("test-model");
        entity.setEstimatedInputTokens(10);
        entity.setEstimatedOutputTokens(0);
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setUpdatedAt(System.currentTimeMillis());
        return entity;
    }

    @Test
    void findActiveReturnsLatestActiveBoundaryOnlyForOwner() {
        checkpointRepository.save(checkpoint(50L, 1, "active"));
        checkpointRepository.save(checkpoint(100L, 1, "active"));

        Optional<AgentContextCheckpointEntity> found =
            checkpointRepository.findActiveByOwnerAndConversation(1L, conversationId);

        assertTrue(found.isPresent());
        assertEquals(100L, found.get().getSourceBoundaryMessageId());
    }

    @Test
    void ownerIsolationPreventsCrossOwnerRead() {
        checkpointRepository.save(checkpoint(50L, 1, "active"));

        // 其他 owner 读取不到当前检查点。
        Optional<AgentContextCheckpointEntity> otherOwner =
            checkpointRepository.findActiveByOwnerAndConversation(2L, conversationId);
        assertTrue(otherOwner.isEmpty());
    }

    @Test
    void duplicateActiveBoundaryViolatesUniqueConstraint() {
        checkpointRepository.save(checkpoint(100L, 1, "active"));
        assertThrows(DataIntegrityViolationException.class,
            () -> checkpointRepository.saveAndFlush(checkpoint(100L, 1, "active")));
        // 约束冲突后清理持久化上下文，避免 teardown flush 复用失败实体。
        entityManager.clear();
    }

    @Test
    void invalidatedBoundaryCanBeRegeneratedWithHigherRevision() {
        checkpointRepository.saveAndFlush(checkpoint(100L, 1, "active"));
        checkpointRepository.invalidateAfterBoundary(1L, conversationId, 100L, "message_edited", 2000L);

        // 失效后同一边界 revision=1 的重复插入仍冲突，但 revision=2 允许重建。
        assertThrows(DataIntegrityViolationException.class,
            () -> checkpointRepository.saveAndFlush(checkpoint(100L, 1, "active")));
        // 约束冲突后清理持久化上下文，避免后续 flush 复用失败实体。
        entityManager.clear();
        checkpointRepository.saveAndFlush(checkpoint(100L, 2, "active"));

        Optional<AgentContextCheckpointEntity> regenerated =
            checkpointRepository.findActiveByOwnerAndConversation(1L, conversationId);
        assertTrue(regenerated.isPresent());
        assertEquals(2, regenerated.get().getRevision());
    }

    @Test
    void invalidateAfterBoundaryOnlyTouchesLaterOrEqualBoundary() {
        checkpointRepository.saveAndFlush(checkpoint(50L, 1, "active"));
        checkpointRepository.saveAndFlush(checkpoint(100L, 1, "active"));

        int updated = checkpointRepository.invalidateAfterBoundary(1L, conversationId, 100L, "message_edited", 3000L);

        assertEquals(1, updated, "只应失效 boundary >= 100 的检查点");
        // 读取最新 active：boundary 50 仍有效。
        Optional<AgentContextCheckpointEntity> remaining =
            checkpointRepository.findActiveByOwnerAndConversation(1L, conversationId);
        assertTrue(remaining.isPresent());
        assertEquals(50L, remaining.get().getSourceBoundaryMessageId());
    }

    @Test
    void deleteByOwnerAndConversationCleansUpCheckpoints() {
        checkpointRepository.save(checkpoint(50L, 1, "active"));
        checkpointRepository.save(checkpoint(100L, 1, "active"));

        checkpointRepository.deleteAllByOwnerUserIdAndConversationId(1L, conversationId);

        assertTrue(checkpointRepository.findActiveByOwnerAndConversation(1L, conversationId).isEmpty());
    }

    @Test
    void invalidatedCheckpointIsNotReturnedAsActive() {
        AgentContextCheckpointEntity saved = checkpointRepository.saveAndFlush(checkpoint(50L, 1, "active"));
        checkpointRepository.invalidateAfterBoundary(1L, conversationId, saved.getSourceBoundaryMessageId(), "policy_changed", 4000L);

        assertTrue(checkpointRepository.findActiveByOwnerAndConversation(1L, conversationId).isEmpty());
    }
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Agent 上下文检查点仓储。
 *
 * <p>所有读写都按 owner_user_id + conversation_id 隔离；会话删除时通过外键
 * ON DELETE CASCADE 级联清理检查点，并提供 {@link #deleteByOwnerAndConversation}
 * 用于显式清理。并发的同一边界压缩通过
 * {@code uk_agent_context_checkpoints_boundary_revision} 唯一约束防止重复有效检查点；
 * 写入冲突时调用方应捕获 {@code DataIntegrityViolationException} 并读取已提交版本。
 */
public interface AgentContextCheckpointRepository
    extends JpaRepository<AgentContextCheckpointEntity, Long> {

    /**
     * 查找当前 owner + conversation 下距离最新消息最近的有效检查点。
     *
     * <p>有效定义：status = 'active'。读取后调用方再加载边界之后的原始消息。
     */
    Optional<AgentContextCheckpointEntity> findFirstByOwnerUserIdAndConversationIdAndStatusOrderBySourceBoundaryMessageIdDescIdDesc(
        Long ownerUserId,
        Long conversationId,
        String status
    );

    /**
     * 便捷方法：查找当前 owner + conversation 的最新 active 检查点。
     */
    default Optional<AgentContextCheckpointEntity> findActiveByOwnerAndConversation(
        Long ownerUserId, Long conversationId
    ) {
        return findFirstByOwnerUserIdAndConversationIdAndStatusOrderBySourceBoundaryMessageIdDescIdDesc(
            ownerUserId, conversationId, "active"
        );
    }

    /**
     * 使 {@code boundaryMessageId} 之后（不含边界本身）的有效检查点失效。
     *
     * <p>用于消息编辑、删除或重新生成时使受影响检查点变为 invalidated；不影响
     * 更早的有效检查点。返回受影响行数，便于审计。
     */
    @Modifying
    @Query("""
        update AgentContextCheckpointEntity c
           set c.status = 'invalidated',
               c.invalidatedAt = :now,
               c.invalidationReason = :reason,
               c.updatedAt = :now
         where c.ownerUserId = :ownerUserId
           and c.conversationId = :conversationId
           and c.status = 'active'
           and c.sourceBoundaryMessageId >= :boundaryMessageId
        """)
    int invalidateAfterBoundary(
        @Param("ownerUserId") Long ownerUserId,
        @Param("conversationId") Long conversationId,
        @Param("boundaryMessageId") Long boundaryMessageId,
        @Param("reason") String reason,
        @Param("now") Long now
    );

    /**
     * 级联清理：会话删除时一并删除所有检查点。
     *
     * <p>外键已配置 ON DELETE CASCADE，但显式删除可在不删除会话时使用。
     */
    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);
}

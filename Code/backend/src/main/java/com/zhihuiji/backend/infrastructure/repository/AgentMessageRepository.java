package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {
    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(Long ownerUserId, Long conversationId);

    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(
        Long ownerUserId,
        Long conversationId,
        Pageable pageable
    );

    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
        Long ownerUserId,
        Long conversationId,
        Pageable pageable
    );

    long countByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);

    @Query("""
        select m.conversationId, count(m.id)
          from AgentMessageEntity m
         where m.ownerUserId = :ownerUserId
           and m.conversationId in :conversationIds
         group by m.conversationId
        """)
    List<Object[]> countByOwnerUserIdAndConversationIdInGroupBy(
        @Param("ownerUserId") Long ownerUserId,
        @Param("conversationIds") List<Long> conversationIds
    );

    boolean existsByOwnerUserIdAndConversationIdAndRunIdAndRole(
        Long ownerUserId,
        Long conversationId,
        String runId,
        String role
    );

    Optional<AgentMessageEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);

    /**
     * 加载检查点边界之后的原始消息（时间正序），用于 ContextBuilder 拼装上下文。
     *
     * <p>边界按 {@code id > boundaryMessageId} 过滤，与 {@code AgentContextCheckpointEntity}
     * 的 {@code source_boundary_message_id} 含义一致：边界本身已经压缩进检查点，
     * 边界之后的消息仍以原始形式注入。
     */
    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdAndIdGreaterThanOrderByIdAsc(
        Long ownerUserId,
        Long conversationId,
        Long boundaryMessageId
    );

    /**
     * 查找会话内最近 N 条原始消息（时间倒序），用于压缩策略选择最早的完整已完成轮次。
     */
    default List<AgentMessageEntity> findRecentForCompaction(
        Long ownerUserId, Long conversationId, int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }
        return findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            ownerUserId, conversationId, Pageable.ofSize(limit)
        );
    }
}

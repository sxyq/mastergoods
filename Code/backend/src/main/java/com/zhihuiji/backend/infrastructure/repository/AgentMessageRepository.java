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
}

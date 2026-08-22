package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentConversationRepository extends JpaRepository<AgentConversationEntity, Long> {
    List<AgentConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId);

    List<AgentConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    @Query("SELECT c FROM AgentConversationEntity c WHERE c.ownerUserId = :ownerUserId ORDER BY c.updatedAt DESC, c.id DESC")
    List<AgentConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(
        Long ownerUserId, Pageable pageable);

    @Query("""
        SELECT c
        FROM AgentConversationEntity c
        WHERE c.ownerUserId = :ownerUserId
          AND EXISTS (
              SELECT m.id
              FROM AgentMessageEntity m
              WHERE m.ownerUserId = :ownerUserId
                AND m.conversationId = c.id
          )
        ORDER BY c.updatedAt DESC, c.id DESC
        """)
    List<AgentConversationEntity> findAllWithMessagesByOwnerUserIdOrderByUpdatedAtDescIdDesc(
        @Param("ownerUserId") Long ownerUserId,
        Pageable pageable
    );

    Optional<AgentConversationEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

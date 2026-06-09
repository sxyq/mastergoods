package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {
    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(Long ownerUserId, Long conversationId);

    List<AgentMessageEntity> findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(
        Long ownerUserId,
        Long conversationId,
        Pageable pageable
    );

    long countByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);

    Optional<AgentMessageEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);
}

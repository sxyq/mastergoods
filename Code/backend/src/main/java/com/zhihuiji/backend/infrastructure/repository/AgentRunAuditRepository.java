package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunAuditRepository extends JpaRepository<AgentRunAuditEntity, Long> {
    Optional<AgentRunAuditEntity> findByRunId(String runId);

    Optional<AgentRunAuditEntity> findByRunIdAndOwnerUserId(String runId, Long ownerUserId);

    List<AgentRunAuditEntity> findAllByOwnerUserIdAndConversationIdOrderByStartedAtAscIdAsc(
        Long ownerUserId,
        Long conversationId,
        Pageable pageable
    );
}

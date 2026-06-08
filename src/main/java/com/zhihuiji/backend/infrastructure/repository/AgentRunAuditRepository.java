package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunAuditRepository extends JpaRepository<AgentRunAuditEntity, Long> {
    Optional<AgentRunAuditEntity> findByRunId(String runId);

    Optional<AgentRunAuditEntity> findByRunIdAndOwnerUserId(String runId, Long ownerUserId);
}

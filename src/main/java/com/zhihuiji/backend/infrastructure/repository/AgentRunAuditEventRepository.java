package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunAuditEventRepository extends JpaRepository<AgentRunAuditEventEntity, Long> {
    List<AgentRunAuditEventEntity> findAllByRunIdOrderBySeqAsc(String runId);

    long countByRunId(String runId);
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentRunAuditEventRepository extends JpaRepository<AgentRunAuditEventEntity, Long> {
    @Query("""
        SELECT e
        FROM AgentRunAuditEventEntity e
        JOIN AgentRunAuditEntity audit ON audit.runId = e.runId
        WHERE e.runId = :runId AND audit.ownerUserId = :ownerUserId
        ORDER BY e.seq ASC
        """)
    List<AgentRunAuditEventEntity> findAllByRunIdAndOwnerUserIdOrderBySeqAsc(
        @Param("runId") String runId,
        @Param("ownerUserId") Long ownerUserId
    );

    @Query("""
        SELECT COUNT(e)
        FROM AgentRunAuditEventEntity e
        JOIN AgentRunAuditEntity audit ON audit.runId = e.runId
        WHERE e.runId = :runId AND audit.ownerUserId = :ownerUserId
        """)
    long countByRunIdAndOwnerUserId(@Param("runId") String runId, @Param("ownerUserId") Long ownerUserId);
}

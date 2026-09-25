// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentRunAuditEventRepository extends JpaRepository<AgentRunAuditEventEntity, Long> {
    Optional<AgentRunAuditEventEntity> findByEventId(String eventId);

    Optional<AgentRunAuditEventEntity> findTopByRunIdOrderBySeqDescIdDesc(String runId);

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

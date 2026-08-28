package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Owner-bound Agent run query source. Store filtering waits for persisted store_id. */
public interface AdminAgentQueryRepository extends Repository<AgentRunAuditEntity, Long> {
    @Query(value = """
        select a
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
         order by a.startedAt desc, a.id desc
        """,
        countQuery = """
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Page<AgentRunAuditEntity> findRuns(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("runId") String runId,
        @Param("conversationId") Long conversationId,
        @Param("status") String status,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt,
        Pageable pageable
    );

    @Query("""
        select a
          from AgentRunAuditEntity a
         where a.runId = :runId
           and (:allOwners = true or a.ownerUserId in :ownerUserIds)
        """)
    Optional<AgentRunAuditEntity> findRun(
        @Param("runId") String runId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds
    );
}

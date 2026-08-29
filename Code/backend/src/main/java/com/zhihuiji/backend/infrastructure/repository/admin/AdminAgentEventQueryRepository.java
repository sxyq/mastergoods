package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Range-bound persisted event source used for list and SSE replay. */
public interface AdminAgentEventQueryRepository extends Repository<AgentRunAuditEventEntity, Long> {
    @Query("""
        select e from AgentRunAuditEventEntity e
          join AgentRunAuditEntity a on a.runId = e.runId
         where e.runId = :runId
           and (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:afterSequence is null or e.seq > :afterSequence)
         order by e.seq asc, e.id asc
        """)
    List<AgentRunAuditEventEntity> findEvents(
        @Param("runId") String runId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("afterSequence") Integer afterSequence
    );

    @Query("""
        select e from AgentRunAuditEventEntity e
          join AgentRunAuditEntity a on a.runId = e.runId
         where e.runId = :runId
           and (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:afterSequence is null or e.seq > :afterSequence)
         order by e.seq asc, e.id asc
        """)
    List<AgentRunAuditEventEntity> findEventsScoped(
        @Param("runId") String runId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("afterSequence") Integer afterSequence
    );

    @Query("""
        select e from AgentRunAuditEventEntity e
          join AgentRunAuditEntity a on a.runId = e.runId
         where e.runId in :runIds
           and (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
         order by e.runId asc, e.seq asc, e.id asc
        """)
    List<AgentRunAuditEventEntity> findEventsForRuns(
        @Param("runIds") Collection<String> runIds,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds
    );
}

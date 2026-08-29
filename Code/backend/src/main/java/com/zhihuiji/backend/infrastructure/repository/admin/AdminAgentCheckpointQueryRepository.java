package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Range-bound context checkpoint source for administrator observability. */
public interface AdminAgentCheckpointQueryRepository extends Repository<AgentContextCheckpointEntity, Long> {
    @Query("""
        select c from AgentContextCheckpointEntity c
         where c.conversationId = :conversationId
           and (:allOwners = true or c.ownerUserId in :ownerUserIds)
         order by c.sourceBoundaryMessageId asc, c.revision asc
        """)
    List<AgentContextCheckpointEntity> findCheckpoints(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds
    );

    @Query("""
        select c from AgentContextCheckpointEntity c
          where c.conversationId = :conversationId
            and (:allOwners = true or c.ownerUserId in :ownerUserIds)
            and (:allStores = true or exists (
                select a.id from AgentRunAuditEntity a
                 where a.conversationId = c.conversationId
                   and (:allOwners = true or a.ownerUserId in :ownerUserIds)
                   and a.storeId in :storeIds
            ))
         order by c.sourceBoundaryMessageId asc, c.revision asc
        """)
    List<AgentContextCheckpointEntity> findCheckpointsScoped(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds
    );
}

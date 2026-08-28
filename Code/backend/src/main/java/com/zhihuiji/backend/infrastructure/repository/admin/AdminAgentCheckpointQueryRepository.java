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
}

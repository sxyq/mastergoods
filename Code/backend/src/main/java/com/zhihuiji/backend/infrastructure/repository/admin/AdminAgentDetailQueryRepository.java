package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Range-bound message projection source for administrator observability. */
public interface AdminAgentDetailQueryRepository extends Repository<AgentMessageEntity, Long> {
    @Query(value = """
        select m from AgentMessageEntity m
         where m.conversationId = :conversationId
           and (:allOwners = true or m.ownerUserId in :ownerUserIds)
         order by m.createdAt asc, m.id asc
        """, countQuery = """
        select count(m.id) from AgentMessageEntity m
         where m.conversationId = :conversationId
           and (:allOwners = true or m.ownerUserId in :ownerUserIds)
        """)
    Page<AgentMessageEntity> findMessages(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        Pageable pageable
    );

    @Query(value = """
        select m from AgentMessageEntity m
          join AgentRunAuditEntity a on a.runId = m.runId
         where m.conversationId = :conversationId
           and (:allOwners = true or m.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
         order by m.createdAt asc, m.id asc
        """, countQuery = """
        select count(m.id) from AgentMessageEntity m
          join AgentRunAuditEntity a on a.runId = m.runId
         where m.conversationId = :conversationId
           and (:allOwners = true or m.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
        """)
    Page<AgentMessageEntity> findMessagesScoped(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        Pageable pageable
    );
}

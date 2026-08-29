package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Range-bound draft source; drafts are linked to runs through their conversation. */
public interface AdminAgentDraftQueryRepository extends Repository<AgentDraftEntity, Long> {
    @Query("""
        select d from AgentDraftEntity d
         where d.conversationId = :conversationId
           and (:allOwners = true or d.ownerUserId in :ownerUserIds)
         order by d.updatedAt desc, d.id desc
        """)
    List<AgentDraftEntity> findDrafts(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds
    );

    @Query("""
        select d from AgentDraftEntity d
         where d.conversationId = :conversationId
           and (:allOwners = true or d.ownerUserId in :ownerUserIds)
           and (:allStores = true or exists (
               select a.id from AgentRunAuditEntity a
                where a.conversationId = d.conversationId
                  and (:allOwners = true or a.ownerUserId in :ownerUserIds)
                  and a.storeId in :storeIds
           ))
         order by d.updatedAt desc, d.id desc
        """)
    List<AgentDraftEntity> findDraftsScoped(
        @Param("conversationId") Long conversationId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds
    );
}

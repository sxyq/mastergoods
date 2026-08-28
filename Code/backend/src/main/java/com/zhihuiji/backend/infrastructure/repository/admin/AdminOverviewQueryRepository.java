package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.UserEntity;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Bounded count queries for the administrator overview. */
public interface AdminOverviewQueryRepository extends Repository<UserEntity, Long> {
    @Query("""
        select count(u.id)
          from UserEntity u
         where (
             :allOwners = true
             or u.id in :ownerUserIds
             or exists (
                 select m.id from StoreMembershipEntity m
                  where m.userId = u.id
                    and m.ownerUserId in :ownerUserIds
                    and (:allStores = true or m.storeId in :storeIds)
             )
         )
        """)
    long countUsers(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("allStores") boolean allStores
    );

    @Query("""
        select count(s.id)
          from StoreEntity s
         where :allOwners = true
            or (s.ownerUserId in :ownerUserIds and (:allStores = true or s.id in :storeIds))
        """)
    long countStores(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("allStores") boolean allStores
    );

    @Query("""
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRuns(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select coalesce(sum(a.toolCount), 0)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long sumAgentToolCount(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );
}

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
           and (:allStores = true or a.storeId in :storeIds)
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRuns(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    /** Compatibility overload for callers that already have an owner-wide scope. */
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
           and (:allStores = true or a.storeId in :storeIds)
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long sumAgentToolCount(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRunsByStatus(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("status") String status,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and lower(a.status) in :statuses
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRunsByStatuses(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("statuses") Collection<String> statuses,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select avg(a.completedAt - a.startedAt)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and lower(a.status) in ('completed', 'confirmation_pending')
           and a.completedAt is not null
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Double averageAgentRunDuration(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    /** Compatibility overload for callers that already have an owner-wide scope. */
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

    @Query("""
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRunsByStatus(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("status") String status,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and lower(a.status) in :statuses
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    long countAgentRunsByStatuses(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("statuses") Collection<String> statuses,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );

    @Query("""
        select avg(a.completedAt - a.startedAt)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and lower(a.status) in ('completed', 'confirmation_pending')
           and a.completedAt is not null
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Double averageAgentRunDuration(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt
    );
}

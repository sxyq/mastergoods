package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Owner/store-bound Agent run query source. */
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

    /** Filtered owner-wide query. Tool/model values are matched only in structured audit payload keys. */
    @Query(value = """
        select a
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:toolName is null or exists (
               select e.id from AgentRunAuditEventEntity e
                where e.runId = a.runId
                  and (lower(e.payloadJson) like lower(concat('%', '"tool_name":"', :toolName, '"%'))
                    or lower(e.payloadJson) like lower(concat('%', '"toolName":"', :toolName, '"%')))
           ))
           and (:modelId is null or exists (
               select e.id from AgentRunAuditEventEntity e
                where e.runId = a.runId
                  and (lower(e.payloadJson) like lower(concat('%', '"model_id":"', :modelId, '"%'))
                    or lower(e.payloadJson) like lower(concat('%', '"modelId":"', :modelId, '"%'))
                    or lower(e.payloadJson) like lower(concat('%', '"model":"', :modelId, '"%')))
           ))
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
         order by a.startedAt desc, a.id desc
        """, countQuery = """
        select count(a.id)
          from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:toolName is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"tool_name":"', :toolName, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"toolName":"', :toolName, '"%')))))
           and (:modelId is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"model_id":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"modelId":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"model":"', :modelId, '"%')))))
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Page<AgentRunAuditEntity> findRunsFiltered(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("runId") String runId,
        @Param("conversationId") Long conversationId,
        @Param("actorUserId") Long actorUserId,
        @Param("toolName") String toolName,
        @Param("modelId") String modelId,
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

    @Query(value = """
        select a from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
         order by a.startedAt desc, a.id desc
        """, countQuery = """
        select count(a.id) from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Page<AgentRunAuditEntity> findRunsScoped(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("runId") String runId,
        @Param("conversationId") Long conversationId,
        @Param("actorUserId") Long actorUserId,
        @Param("status") String status,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt,
        Pageable pageable
    );

    /** Filtered owner/store-scoped query. */
    @Query(value = """
        select a from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:toolName is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"tool_name":"', :toolName, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"toolName":"', :toolName, '"%')))))
           and (:modelId is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"model_id":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"modelId":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"model":"', :modelId, '"%')))))
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
         order by a.startedAt desc, a.id desc
        """, countQuery = """
        select count(a.id) from AgentRunAuditEntity a
         where (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
           and (:runId is null or a.runId = :runId)
           and (:conversationId is null or a.conversationId = :conversationId)
           and (:actorUserId is null or a.actorUserId = :actorUserId)
           and (:toolName is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"tool_name":"', :toolName, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"toolName":"', :toolName, '"%')))))
           and (:modelId is null or exists (select e.id from AgentRunAuditEventEntity e where e.runId = a.runId
             and (lower(e.payloadJson) like lower(concat('%', '"model_id":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"modelId":"', :modelId, '"%'))
               or lower(e.payloadJson) like lower(concat('%', '"model":"', :modelId, '"%')))))
           and (:status is null or lower(a.status) = lower(:status))
           and (:fromAt is null or a.startedAt >= :fromAt)
           and (:toAt is null or a.startedAt < :toAt)
        """)
    Page<AgentRunAuditEntity> findRunsScopedFiltered(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("runId") String runId,
        @Param("conversationId") Long conversationId,
        @Param("actorUserId") Long actorUserId,
        @Param("toolName") String toolName,
        @Param("modelId") String modelId,
        @Param("status") String status,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt,
        Pageable pageable
    );

    @Query("""
        select a from AgentRunAuditEntity a
         where a.runId = :runId
           and (:allOwners = true or a.ownerUserId in :ownerUserIds)
           and (:allStores = true or a.storeId in :storeIds)
        """)
    Optional<AgentRunAuditEntity> findRunScoped(
        @Param("runId") String runId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds
    );
}

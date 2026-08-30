package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminAuditEventEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEventEntity, Long> {
    long countByResult(String result);
    Optional<AdminAuditEventEntity> findByEventId(String eventId);

    Optional<AdminAuditEventEntity> findByAdminUserIdAndIdempotencyKey(Long adminUserId, String idempotencyKey);

    @Query(value = """
        select e from AdminAuditEventEntity e
         where (:allOwners = true
             or e.ownerUserId in :ownerUserIds
             or (e.ownerUserId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.storeId in :storeIds or (e.storeId is null and e.adminUserId = :requestingAdminUserId))
           and (:action is null or lower(e.action) = :action)
           and (:resourceType is null or lower(e.resourceType) = :resourceType)
           and (:result is null or lower(e.result) = :result)
           and (:fromAt is null or e.occurredAt >= :fromAt)
           and (:toAt is null or e.occurredAt < :toAt)
         order by e.occurredAt desc, e.id desc
        """, countQuery = """
        select count(e.id) from AdminAuditEventEntity e
         where (:allOwners = true
             or e.ownerUserId in :ownerUserIds
             or (e.ownerUserId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.storeId in :storeIds or (e.storeId is null and e.adminUserId = :requestingAdminUserId))
           and (:action is null or lower(e.action) = :action)
           and (:resourceType is null or lower(e.resourceType) = :resourceType)
           and (:result is null or lower(e.result) = :result)
           and (:fromAt is null or e.occurredAt >= :fromAt)
           and (:toAt is null or e.occurredAt < :toAt)
        """)
    Page<AdminAuditEventEntity> findVisible(
        @Param("requestingAdminUserId") Long requestingAdminUserId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("action") String action,
        @Param("resourceType") String resourceType,
        @Param("result") String result,
        @Param("fromAt") Long fromAt,
        @Param("toAt") Long toAt,
        Pageable pageable
    );

    @Query(value = """
        select e from AdminAuditEventEntity e
         where e.eventId = :eventId
           and (:allOwners = true
             or e.ownerUserId in :ownerUserIds
             or (e.ownerUserId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.storeId in :storeIds or (e.storeId is null and e.adminUserId = :requestingAdminUserId))
        """, countQuery = """
        select count(e.id) from AdminAuditEventEntity e
         where e.eventId = :eventId
           and (:allOwners = true
             or e.ownerUserId in :ownerUserIds
             or (e.ownerUserId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.storeId in :storeIds or (e.storeId is null and e.adminUserId = :requestingAdminUserId))
        """)
    Page<AdminAuditEventEntity> findVisibleByEventId(
        @Param("eventId") String eventId,
        @Param("requestingAdminUserId") Long requestingAdminUserId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        Pageable pageable
    );
}

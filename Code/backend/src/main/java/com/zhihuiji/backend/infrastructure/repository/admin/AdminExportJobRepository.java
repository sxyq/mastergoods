package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminExportJobRepository extends JpaRepository<AdminExportJobEntity, Long> {
    long countByStatus(String status);
    Optional<AdminExportJobEntity> findByExportId(String exportId);
    Optional<AdminExportJobEntity> findByAdminUserIdAndIdempotencyKey(Long adminUserId, String idempotencyKey);

    List<AdminExportJobEntity> findTop20ByStatusOrderByCreatedAtAsc(String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update AdminExportJobEntity e
           set e.status = 'RUNNING'
         where e.exportId = :exportId
           and e.status = 'PENDING'
        """)
    int claimPending(@Param("exportId") String exportId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update AdminExportJobEntity e
           set e.status = 'EXPIRED', e.contentCsv = null
         where e.expiresAt <= :now
           and e.status <> 'EXPIRED'
        """)
    int expireAndClearExpired(@Param("now") long now);

    @Query(value = """
        select e from AdminExportJobEntity e
         where (:allOwners = true
             or e.scopeOwnerUserId in :ownerUserIds
             or (e.scopeOwnerUserId is null and e.scopeStoreId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.scopeStoreId in :storeIds
                or (e.scopeStoreId is null and e.scopeOwnerUserId is null and e.adminUserId = :requestingAdminUserId))
         order by e.createdAt desc, e.id desc
        """, countQuery = """
        select count(e.id) from AdminExportJobEntity e
         where (:allOwners = true
             or e.scopeOwnerUserId in :ownerUserIds
             or (e.scopeOwnerUserId is null and e.scopeStoreId is null and e.adminUserId = :requestingAdminUserId))
           and (:allStores = true or e.scopeStoreId in :storeIds
                or (e.scopeStoreId is null and e.scopeOwnerUserId is null and e.adminUserId = :requestingAdminUserId))
        """)
    Page<AdminExportJobEntity> findVisible(
        @Param("requestingAdminUserId") Long requestingAdminUserId,
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("allStores") boolean allStores,
        @Param("storeIds") Collection<Long> storeIds,
        Pageable pageable
    );
}

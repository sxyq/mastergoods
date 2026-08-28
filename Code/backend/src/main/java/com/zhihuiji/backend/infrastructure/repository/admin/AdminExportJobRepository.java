package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminExportJobEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminExportJobRepository extends JpaRepository<AdminExportJobEntity, Long> {
    long countByStatus(String status);
    Optional<AdminExportJobEntity> findByExportId(String exportId);
    Optional<AdminExportJobEntity> findByAdminUserIdAndIdempotencyKey(Long adminUserId, String idempotencyKey);

    @Query(value = """
        select e from AdminExportJobEntity e
         where (:allOwners = true
             or e.adminUserId = :requestingAdminUserId
             or e.scopeOwnerUserId in :ownerUserIds)
           and (:allStores = true or e.scopeStoreId in :storeIds
                or (e.scopeStoreId is null and e.scopeOwnerUserId is null and e.adminUserId = :requestingAdminUserId))
         order by e.createdAt desc, e.id desc
        """, countQuery = """
        select count(e.id) from AdminExportJobEntity e
         where (:allOwners = true
             or e.adminUserId = :requestingAdminUserId
             or e.scopeOwnerUserId in :ownerUserIds)
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

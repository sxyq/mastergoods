package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.StoreEntity;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Owner/store-bound store projection source for administrator reads. */
public interface AdminStoreQueryRepository extends Repository<StoreEntity, Long> {
    @Query(value = """
        select s.id as storeId,
               s.ownerUserId as ownerUserId,
               s.storeName as name,
               s.status as status,
               s.adminVersion as adminVersion,
               (select count(m.id) from StoreMembershipEntity m
                 where m.storeId = s.id and m.ownerUserId = s.ownerUserId) as memberCount,
               s.createdAt as createdAt,
               s.updatedAt as updatedAt
          from StoreEntity s
         where (
             :allOwners = true
             or (
                 s.ownerUserId in :ownerUserIds
                 and (:allStores = true or s.id in :storeIds)
             )
         )
         order by s.updatedAt desc, s.id desc
        """,
        countQuery = """
        select count(s.id)
          from StoreEntity s
         where (
             :allOwners = true
             or (
                 s.ownerUserId in :ownerUserIds
                 and (:allStores = true or s.id in :storeIds)
             )
         )
        """)
    Page<StoreProjection> findStores(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("allStores") boolean allStores,
        Pageable pageable
    );

    interface StoreProjection {
        Long getStoreId();
        Long getOwnerUserId();
        String getName();
        Integer getStatus();
        Long getAdminVersion();
        Long getMemberCount();
        Long getCreatedAt();
        Long getUpdatedAt();
    }
}

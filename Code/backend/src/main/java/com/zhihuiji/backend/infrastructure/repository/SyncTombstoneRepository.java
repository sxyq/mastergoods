package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SyncTombstoneEntity;
import com.zhihuiji.backend.domain.entity.SyncTombstoneId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncTombstoneRepository
    extends JpaRepository<SyncTombstoneEntity, SyncTombstoneId> {

    @Query("""
        SELECT t FROM SyncTombstoneEntity t
        WHERE t.ownerUserId = :ownerUserId
          AND t.storeId = :storeId
          AND t.deletedAt > :sinceTimestamp
        ORDER BY t.deletedAt ASC, t.entityType ASC, t.entityId ASC
        """)
    List<SyncTombstoneEntity> findChangedByOwnerUserId(
        @Param("ownerUserId") Long ownerUserId,
        @Param("storeId") Long storeId,
        @Param("sinceTimestamp") long sinceTimestamp);
}

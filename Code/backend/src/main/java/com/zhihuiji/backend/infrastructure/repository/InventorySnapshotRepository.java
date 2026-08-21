package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshotEntity, Long> {
    List<InventorySnapshotEntity> findAllByOwnerUserIdOrderBySnapshotDateAscIdAsc(Long ownerUserId);

    @Query("SELECT e FROM InventorySnapshotEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp ORDER BY e.snapshotDate ASC, e.id ASC")
    List<InventorySnapshotEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    List<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(Long ownerUserId, Long snapshotDate);
    Optional<InventorySnapshotEntity> findByOwnerUserIdAndProductIdAndSnapshotDate(Long ownerUserId, Long productId, Long snapshotDate);
    List<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(Long ownerUserId, Long startDate, Long endDate);

    Page<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(Long ownerUserId, Long snapshotDate, Pageable pageable);
    Page<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(Long ownerUserId, Long startDate, Long endDate, Pageable pageable);
}

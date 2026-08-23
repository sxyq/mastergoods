package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustmentEntity, Long> {
    java.util.Optional<InventoryAdjustmentEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<InventoryAdjustmentEntity> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    List<InventoryAdjustmentEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM InventoryAdjustmentEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp ORDER BY e.createdAt ASC")
    List<InventoryAdjustmentEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    List<InventoryAdjustmentEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    List<InventoryAdjustmentEntity> findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long ownerUserId,
        Long startAt,
        Long endAt,
        Pageable pageable
    );
}

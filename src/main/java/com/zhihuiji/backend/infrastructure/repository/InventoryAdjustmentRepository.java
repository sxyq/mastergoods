package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustmentEntity, Long> {
    java.util.Optional<InventoryAdjustmentEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<InventoryAdjustmentEntity> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    List<InventoryAdjustmentEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    List<InventoryAdjustmentEntity> findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long ownerUserId,
        Long startAt,
        Long endAt,
        Pageable pageable
    );
}

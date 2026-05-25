package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustmentEntity, Long> {
    List<InventoryAdjustmentEntity> findByCreatedAtBetween(Long startAt, Long endAt);
}


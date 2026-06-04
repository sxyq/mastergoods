package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshotEntity, Long> {
    List<InventorySnapshotEntity> findAllByOwnerUserIdOrderBySnapshotDateAscIdAsc(Long ownerUserId);
    List<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(Long ownerUserId, Long snapshotDate);
    Optional<InventorySnapshotEntity> findByOwnerUserIdAndProductIdAndSnapshotDate(Long ownerUserId, Long productId, Long snapshotDate);
    List<InventorySnapshotEntity> findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(Long ownerUserId, Long startDate, Long endDate);
}

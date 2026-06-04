package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedgerEntity, Long> {
    List<InventoryLedgerEntity> findAllByOwnerUserIdOrderByCreatedAtAscIdAsc(Long ownerUserId);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndProductIdOrderByCreatedAtDesc(Long ownerUserId, Long productId);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long ownerUserId, Long startAt, Long endAt);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndSourceTypeAndSourceId(Long ownerUserId, String sourceType, Long sourceId);
}

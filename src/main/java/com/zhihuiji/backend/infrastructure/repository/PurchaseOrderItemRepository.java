package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItemEntity, Long> {
    List<PurchaseOrderItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    java.util.Optional<PurchaseOrderItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseOrderItemEntity> findByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    void deleteByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);
}

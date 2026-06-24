package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItemEntity, Long> {
    List<PurchaseOrderItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    @Query("SELECT e FROM PurchaseOrderItemEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp ORDER BY e.createdAt ASC")
    List<PurchaseOrderItemEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    java.util.Optional<PurchaseOrderItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseOrderItemEntity> findByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    List<PurchaseOrderItemEntity> findByOwnerUserIdAndOrderIdIn(Long ownerUserId, Collection<Long> orderIds);

    void deleteByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    void deleteAllByOrderId(Long orderId);
}

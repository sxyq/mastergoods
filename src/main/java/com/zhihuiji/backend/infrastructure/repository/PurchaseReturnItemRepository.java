package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReturnItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItemEntity, Long> {
    List<PurchaseReturnItemEntity> findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(Long ownerUserId, Long returnId);
}

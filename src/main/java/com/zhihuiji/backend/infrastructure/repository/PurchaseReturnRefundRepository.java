package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReturnRefundEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnRefundRepository extends JpaRepository<PurchaseReturnRefundEntity, Long> {
    List<PurchaseReturnRefundEntity> findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(Long ownerUserId, Long returnId);
}

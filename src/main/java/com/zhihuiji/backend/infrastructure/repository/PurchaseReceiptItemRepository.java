package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReceiptItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptItemRepository extends JpaRepository<PurchaseReceiptItemEntity, Long> {
    List<PurchaseReceiptItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    java.util.Optional<PurchaseReceiptItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseReceiptItemEntity> findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(Long ownerUserId, Long receiptId);

    List<PurchaseReceiptItemEntity> findByReceiptIdOrderByCreatedAtAsc(Long receiptId);

    void deleteByOwnerUserIdAndReceiptId(Long ownerUserId, Long receiptId);
}

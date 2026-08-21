package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReceiptItemEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReceiptItemRepository extends JpaRepository<PurchaseReceiptItemEntity, Long> {
    List<PurchaseReceiptItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    @Query("SELECT e FROM PurchaseReceiptItemEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp ORDER BY e.createdAt ASC")
    List<PurchaseReceiptItemEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    java.util.Optional<PurchaseReceiptItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseReceiptItemEntity> findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(Long ownerUserId, Long receiptId);

    List<PurchaseReceiptItemEntity> findByOwnerUserIdAndReceiptIdInOrderByReceiptIdAscCreatedAtAsc(
        Long ownerUserId,
        Collection<Long> receiptIds
    );

    List<PurchaseReceiptItemEntity> findByReceiptIdOrderByCreatedAtAsc(Long receiptId);

    void deleteByOwnerUserIdAndReceiptId(Long ownerUserId, Long receiptId);
}

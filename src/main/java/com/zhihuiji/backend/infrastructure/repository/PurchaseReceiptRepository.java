package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceiptEntity, Long> {

    List<PurchaseReceiptEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<PurchaseReceiptEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseReceiptEntity> findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(Long ownerUserId, Long purchaseOrderId);

    List<PurchaseReceiptEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, Integer status);

    @Query("SELECT pr FROM PurchaseReceiptEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (:keyword IS NULL OR LOWER(pr.receiptNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "AND (:status IS NULL OR pr.status = :status) " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReceiptEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );
}

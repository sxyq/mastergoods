package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturnEntity, Long> {

    List<PurchaseReturnEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<PurchaseReturnEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseReturnEntity> findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(Long ownerUserId, Long purchaseOrderId);

    List<PurchaseReturnEntity> findByOwnerUserIdAndPurchaseOrderIdAndStatusInOrderByCreatedAtDesc(
        Long ownerUserId,
        Long purchaseOrderId,
        List<Integer> statuses
    );

    List<PurchaseReturnEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, Integer status);

    @Query("SELECT pr FROM PurchaseReturnEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (:keyword IS NULL OR LOWER(pr.returnNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "AND (:status IS NULL OR pr.status = :status) " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReturnEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );
}

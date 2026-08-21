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
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.returnNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "AND pr.status = :status " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReturnEntity> searchByKeywordAndStatus(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );

    @Query("SELECT pr FROM PurchaseReturnEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.returnNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReturnEntity> searchByKeyword(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword
    );

    default List<PurchaseReturnEntity> search(Long ownerUserId, String keyword, Integer status) {
        return status == null
            ? searchByKeyword(ownerUserId, keyword)
            : searchByKeywordAndStatus(ownerUserId, keyword, status);
    }
}

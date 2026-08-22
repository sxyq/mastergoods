package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceiptEntity, Long> {

    List<PurchaseReceiptEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<PurchaseReceiptEntity> findByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM PurchaseReceiptEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.createdAt DESC")
    List<PurchaseReceiptEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    Optional<PurchaseReceiptEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PurchaseReceiptEntity> findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(Long ownerUserId, Long purchaseOrderId);

    List<PurchaseReceiptEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, Integer status);

    List<PurchaseReceiptEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDescIdDesc(
        Long ownerUserId,
        Integer status,
        Pageable pageable
    );

    @Query("SELECT pr FROM PurchaseReceiptEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.receiptNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "AND pr.status = :status " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReceiptEntity> searchByKeywordAndStatus(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );

    @Query("SELECT pr FROM PurchaseReceiptEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.receiptNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "AND pr.status = :status " +
        "ORDER BY pr.createdAt DESC, pr.id DESC")
    List<PurchaseReceiptEntity> searchByKeywordAndStatus(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        Pageable pageable
    );

    @Query("SELECT pr FROM PurchaseReceiptEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.receiptNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "ORDER BY pr.createdAt DESC")
    List<PurchaseReceiptEntity> searchByKeyword(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword
    );

    @Query("SELECT pr FROM PurchaseReceiptEntity pr WHERE pr.ownerUserId = :ownerUserId " +
        "AND (COALESCE(:keyword, '') = '' OR LOWER(pr.receiptNo) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) " +
        "OR LOWER(pr.supplierName) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%'))) " +
        "ORDER BY pr.createdAt DESC, pr.id DESC")
    List<PurchaseReceiptEntity> searchByKeyword(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    default List<PurchaseReceiptEntity> search(Long ownerUserId, String keyword, Integer status) {
        return status == null
            ? searchByKeyword(ownerUserId, keyword)
            : searchByKeywordAndStatus(ownerUserId, keyword, status);
    }
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleOrderRepository extends JpaRepository<SaleOrderEntity, Long> {
    List<SaleOrderEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    List<SaleOrderEntity> findByOwnerUserIdAndStatusAndUpdatedAtBetween(Long ownerUserId, Integer status, Long updatedAfter, Long updatedBefore);

    Optional<SaleOrderEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SaleOrderEntity e WHERE e.id = :id AND e.ownerUserId = :ownerUserId")
    Optional<SaleOrderEntity> findByIdAndOwnerUserIdForUpdate(
        @Param("id") Long id,
        @Param("ownerUserId") Long ownerUserId
    );

    boolean existsByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<SaleOrderEntity> findAllByOwnerUserId(Long ownerUserId);

    List<SaleOrderEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    @Query("SELECT e FROM SaleOrderEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp")
    List<SaleOrderEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    @Query("SELECT o FROM SaleOrderEntity o WHERE " +
        "o.ownerUserId = :ownerUserId AND " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:minTotal IS NULL OR o.totalAmount >= :minTotal - 0.000001) AND " +
        "(:maxTotal IS NULL OR o.totalAmount <= :maxTotal + 0.000001) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) AND " +
        "(:paymentStatus IS NULL OR " +
        "  (:paymentStatus = 0 AND o.paidAmount + 0.000001 < o.totalAmount) OR " +
        "  (:paymentStatus = 1 AND o.paidAmount + 0.000001 >= o.totalAmount)) AND " +
        "(:productKeyword IS NULL OR :productKeyword = '' OR EXISTS (" +
        "  SELECT 1 FROM SaleOrderItemEntity item " +
        "  WHERE item.orderId = o.id AND (" +
        "    LOWER(item.productCode) LIKE LOWER(CONCAT('%', :productKeyword, '%')) OR " +
        "    LOWER(item.productName) LIKE LOWER(CONCAT('%', :productKeyword, '%'))" +
        "  )" +
        ")) " +
        "ORDER BY o.createdAt DESC")
    List<SaleOrderEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("minTotal") Double minTotal,
        @Param("maxTotal") Double maxTotal,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore,
        @Param("productKeyword") String productKeyword,
        @Param("paymentStatus") Integer paymentStatus
    );

    @Query("SELECT o FROM SaleOrderEntity o WHERE " +
        "o.ownerUserId = :ownerUserId AND " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:minTotal IS NULL OR o.totalAmount >= :minTotal - 0.000001) AND " +
        "(:maxTotal IS NULL OR o.totalAmount <= :maxTotal + 0.000001) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) AND " +
        "(:paymentStatus IS NULL OR " +
        "  (:paymentStatus = 0 AND o.paidAmount + 0.000001 < o.totalAmount) OR " +
        "  (:paymentStatus = 1 AND o.paidAmount + 0.000001 >= o.totalAmount)) AND " +
        "(:productKeyword IS NULL OR :productKeyword = '' OR EXISTS (" +
        "  SELECT 1 FROM SaleOrderItemEntity item " +
        "  WHERE item.orderId = o.id AND (" +
        "    LOWER(item.productCode) LIKE LOWER(CONCAT('%', :productKeyword, '%')) OR " +
        "    LOWER(item.productName) LIKE LOWER(CONCAT('%', :productKeyword, '%'))" +
        "  )" +
        ")) " +
        "ORDER BY o.createdAt DESC")
    List<SaleOrderEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("minTotal") Double minTotal,
        @Param("maxTotal") Double maxTotal,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore,
        @Param("productKeyword") String productKeyword,
        @Param("paymentStatus") Integer paymentStatus,
        Pageable pageable
    );

    @Query("SELECT SUM(o.totalAmount) FROM SaleOrderEntity o WHERE o.ownerUserId = :ownerUserId AND o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Double sumTotalAmountBetween(@Param("ownerUserId") Long ownerUserId, @Param("startAt") Long startAt, @Param("endAt") Long endAt);

    @Query("SELECT SUM(o.paidAmount) FROM SaleOrderEntity o WHERE o.ownerUserId = :ownerUserId AND o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Double sumPaidAmountBetween(@Param("ownerUserId") Long ownerUserId, @Param("startAt") Long startAt, @Param("endAt") Long endAt);

    @Query("SELECT COUNT(o) FROM SaleOrderEntity o WHERE o.ownerUserId = :ownerUserId AND o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Long countNonCancelledBetween(@Param("ownerUserId") Long ownerUserId, @Param("startAt") Long startAt, @Param("endAt") Long endAt);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0),
               COALESCE(SUM(o.paidAmount), 0),
               COUNT(o)
        FROM SaleOrderEntity o
        WHERE o.ownerUserId = :ownerUserId
          AND o.createdAt BETWEEN :startAt AND :endAt
          AND o.status <> :cancelledStatus
    """)
    Object[] salesSummaryAggregate(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus
    );

    @Query(value = """
        WITH bucketed_orders AS (
            SELECT ((created_at - :startAt) / :bucketMillis) AS bucket_index,
                   total_amount,
                   paid_amount
            FROM sale_orders
            WHERE owner_user_id = :ownerUserId
              AND created_at BETWEEN :startAt AND :endAt
              AND status <> :cancelledStatus
        )
        SELECT bucket_index,
               COALESCE(SUM(total_amount), 0) AS total_sales_amount,
               COUNT(*) AS total_order_count,
               COALESCE(SUM(paid_amount), 0) AS total_paid_amount
        FROM bucketed_orders
        GROUP BY bucket_index
        ORDER BY bucket_index
    """, nativeQuery = true)
    List<Object[]> salesTrendBuckets(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("bucketMillis") Long bucketMillis,
        @Param("cancelledStatus") Integer cancelledStatus
    );

    @Query("""
        SELECT o.customerId, o.customerName, COUNT(o), COALESCE(SUM(o.totalAmount), 0)
        FROM SaleOrderEntity o
        WHERE o.ownerUserId = :ownerUserId
          AND o.createdAt BETWEEN :startAt AND :endAt
          AND o.status <> :cancelledStatus
        GROUP BY o.customerId, o.customerName
        ORDER BY SUM(o.totalAmount) DESC
    """)
    List<Object[]> customerSales(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleOrderRepository extends JpaRepository<SaleOrderEntity, Long> {
    List<SaleOrderEntity> findByCreatedAtBetween(Long startAt, Long endAt);

    @Query("SELECT o FROM SaleOrderEntity o WHERE " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:minTotal IS NULL OR o.totalAmount >= :minTotal - 0.000001) AND " +
        "(:maxTotal IS NULL OR o.totalAmount <= :maxTotal + 0.000001) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) " +
        "ORDER BY o.createdAt DESC")
    List<SaleOrderEntity> search(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("minTotal") Double minTotal,
        @Param("maxTotal") Double maxTotal,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore
    );

    @Query("SELECT SUM(o.totalAmount) FROM SaleOrderEntity o WHERE o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Double sumTotalAmountBetween(@Param("startAt") Long startAt, @Param("endAt") Long endAt);

    @Query("SELECT SUM(o.paidAmount) FROM SaleOrderEntity o WHERE o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Double sumPaidAmountBetween(@Param("startAt") Long startAt, @Param("endAt") Long endAt);

    @Query("SELECT COUNT(o) FROM SaleOrderEntity o WHERE o.createdAt BETWEEN :startAt AND :endAt AND o.status <> 2")
    Long countNonCancelledBetween(@Param("startAt") Long startAt, @Param("endAt") Long endAt);
}

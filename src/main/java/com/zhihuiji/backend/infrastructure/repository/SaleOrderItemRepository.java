package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItemEntity, Long> {
    List<SaleOrderItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    java.util.Optional<SaleOrderItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<SaleOrderItemEntity> findByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    List<SaleOrderItemEntity> findByOwnerUserIdAndOrderIdIn(Long ownerUserId, Collection<Long> orderIds);

    List<SaleOrderItemEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    void deleteByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    @Query("""
        SELECT item, orderEntity
        FROM SaleOrderItemEntity item
        JOIN SaleOrderEntity orderEntity
          ON item.orderId = orderEntity.id
         AND orderEntity.ownerUserId = :ownerUserId
        WHERE item.ownerUserId = :ownerUserId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
        ORDER BY item.createdAt DESC
    """)
    List<Object[]> recentStockOutRows(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );

    @Query("""
        SELECT item, orderEntity
        FROM SaleOrderItemEntity item
        JOIN SaleOrderEntity orderEntity
          ON item.orderId = orderEntity.id
         AND orderEntity.ownerUserId = :ownerUserId
        WHERE item.ownerUserId = :ownerUserId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
        ORDER BY orderEntity.createdAt DESC
    """)
    List<Object[]> recentSaleInventoryFlowRows(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );

    @Query("""
        SELECT item, orderEntity
        FROM SaleOrderItemEntity item
        JOIN SaleOrderEntity orderEntity
          ON item.orderId = orderEntity.id
         AND orderEntity.ownerUserId = :ownerUserId
        WHERE item.ownerUserId = :ownerUserId
          AND orderEntity.status = :cancelledStatus
          AND orderEntity.updatedAt BETWEEN :startAt AND :endAt
        ORDER BY orderEntity.updatedAt DESC
    """)
    List<Object[]> recentCancelledSaleInventoryFlowRows(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(item.amount), 0),
               COALESCE(SUM(item.quantity * COALESCE(p.purchasePrice, 0)), 0)
        FROM SaleOrderItemEntity item
        JOIN SaleOrderEntity orderEntity
          ON item.orderId = orderEntity.id
         AND orderEntity.ownerUserId = :ownerUserId
        LEFT JOIN ProductEntity p
          ON p.ownerUserId = :ownerUserId
         AND p.id = item.productId
        WHERE item.ownerUserId = :ownerUserId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
    """)
    Object[] profitSummary(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus
    );

    @Query("""
        SELECT item.productId, item.productCode, item.productName, COALESCE(SUM(item.quantity), 0), COALESCE(SUM(item.amount), 0)
        FROM SaleOrderItemEntity item, SaleOrderEntity orderEntity
        WHERE item.orderId = orderEntity.id
          AND item.ownerUserId = :ownerUserId
          AND orderEntity.ownerUserId = :ownerUserId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
        GROUP BY item.productId, item.productCode, item.productName
        ORDER BY SUM(item.amount) DESC
    """)
    List<Object[]> topProducts(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );

    @Query("""
        SELECT item.productId, item.productCode, item.productName,
               COALESCE(SUM(item.amount), 0),
               COALESCE(SUM(item.quantity * p.purchasePrice), 0)
        FROM SaleOrderItemEntity item, SaleOrderEntity orderEntity, ProductEntity p
        WHERE item.orderId = orderEntity.id
          AND item.ownerUserId = :ownerUserId
          AND orderEntity.ownerUserId = :ownerUserId
          AND p.ownerUserId = :ownerUserId
          AND p.id = item.productId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
        GROUP BY item.productId, item.productCode, item.productName
        ORDER BY (SUM(item.amount) - SUM(item.quantity * p.purchasePrice)) DESC
    """)
    List<Object[]> profitByProducts(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );

    @Query("""
        SELECT orderEntity.customerId, orderEntity.customerName,
               COALESCE(SUM(item.amount), 0),
               COALESCE(SUM(item.quantity * p.purchasePrice), 0)
        FROM SaleOrderItemEntity item, SaleOrderEntity orderEntity, ProductEntity p
        WHERE item.orderId = orderEntity.id
          AND item.ownerUserId = :ownerUserId
          AND orderEntity.ownerUserId = :ownerUserId
          AND p.ownerUserId = :ownerUserId
          AND p.id = item.productId
          AND orderEntity.createdAt BETWEEN :startAt AND :endAt
          AND (orderEntity.status IS NULL OR orderEntity.status <> :cancelledStatus)
        GROUP BY orderEntity.customerId, orderEntity.customerName
        ORDER BY (SUM(item.amount) - SUM(item.quantity * p.purchasePrice)) DESC
    """)
    List<Object[]> profitByCustomers(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("cancelledStatus") Integer cancelledStatus,
        Pageable pageable
    );
}

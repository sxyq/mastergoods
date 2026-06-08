package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayOrderRepository extends JpaRepository<PayOrderEntity, Long> {
    List<PayOrderEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    Optional<PayOrderEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PayOrderEntity> findAllByOwnerUserId(Long ownerUserId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PayOrderEntity o WHERE o.ownerUserId = :ownerUserId AND o.createdAt BETWEEN :startAt AND :endAt AND o.status = :status")
    Double sumAmountBetweenByStatus(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("status") Integer status
    );

    @Query("SELECT o FROM PayOrderEntity o WHERE " +
        "o.ownerUserId = :ownerUserId AND " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) " +
        "ORDER BY o.createdAt DESC")
    List<PayOrderEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore
    );

    @Query("SELECT o FROM PayOrderEntity o WHERE " +
        "o.ownerUserId = :ownerUserId AND " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) " +
        "ORDER BY o.createdAt DESC")
    List<PayOrderEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore,
        Pageable pageable
    );
}

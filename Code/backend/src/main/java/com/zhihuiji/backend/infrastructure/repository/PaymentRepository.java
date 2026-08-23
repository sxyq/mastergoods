package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    java.util.Optional<PaymentEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<PaymentEntity> findByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);

    List<PaymentEntity> findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(Long ownerUserId, Long orderId);

    List<PaymentEntity> findByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Long startAt, Long endAt);

    List<PaymentEntity> findByOwnerUserIdAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long ownerUserId,
        Integer type,
        Long startAt,
        Long endAt,
        Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(ABS(p.amount)), 0) FROM PaymentEntity p WHERE p.ownerUserId = :ownerUserId AND p.createdAt BETWEEN :startAt AND :endAt AND p.type = :type")
    Double sumAbsoluteAmountBetweenByType(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("type") Integer type
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.ownerUserId = :ownerUserId AND p.createdAt BETWEEN :startAt AND :endAt AND (p.type IS NULL OR p.type <> :refundType) AND p.amount > 0")
    Double sumReceivedAmountBetween(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("refundType") Integer refundType
    );

    List<PaymentEntity> findAllByOwnerUserId(Long ownerUserId);
    List<PaymentEntity> findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM PaymentEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp")
    List<PaymentEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    void deleteByOwnerUserIdAndOrderId(Long ownerUserId, Long orderId);
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayOrderRepository extends JpaRepository<PayOrderEntity, Long> {
    List<PayOrderEntity> findByCreatedAtBetween(Long startAt, Long endAt);

    @Query("SELECT o FROM PayOrderEntity o WHERE " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:createdAfter IS NULL OR o.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR o.createdAt <= :createdBefore) " +
        "ORDER BY o.createdAt DESC")
    List<PayOrderEntity> search(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore
    );
}

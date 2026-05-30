package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {
    List<PurchaseOrderEntity> findByCreatedAtBetween(Long startAt, Long endAt);

    @Query("SELECT o FROM PurchaseOrderEntity o WHERE " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "ORDER BY o.createdAt DESC")
    List<PurchaseOrderEntity> search(
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );
}

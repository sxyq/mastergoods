package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItemEntity, Long> {
    List<SaleOrderItemEntity> findByOrderId(Long orderId);

    List<SaleOrderItemEntity> findByCreatedAtBetween(Long startAt, Long endAt);
}


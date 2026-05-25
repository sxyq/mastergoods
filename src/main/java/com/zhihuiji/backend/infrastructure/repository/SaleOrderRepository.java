package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleOrderRepository extends JpaRepository<SaleOrderEntity, Long> {
    List<SaleOrderEntity> findByCreatedAtBetween(Long startAt, Long endAt);
}


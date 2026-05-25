package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayOrderRepository extends JpaRepository<PayOrderEntity, Long> {
    List<PayOrderEntity> findByCreatedAtBetween(Long startAt, Long endAt);
}

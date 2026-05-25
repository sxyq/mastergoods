package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByCreatedAtBetween(Long startAt, Long endAt);
}


package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceRecordRepository extends JpaRepository<FinanceRecordEntity, Long> {
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashChangeRecordRepository extends JpaRepository<CashChangeRecordEntity, Long> {
    List<CashChangeRecordEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    Optional<CashChangeRecordEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

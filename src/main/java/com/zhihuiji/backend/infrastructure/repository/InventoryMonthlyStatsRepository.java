package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMonthlyStatsRepository extends JpaRepository<InventoryMonthlyStatsEntity, Long> {
    List<InventoryMonthlyStatsEntity> findAllByOwnerUserIdOrderByYearAscMonthAscIdAsc(Long ownerUserId);
    List<InventoryMonthlyStatsEntity> findAllByOwnerUserIdAndYearAndMonthOrderByProductNameAsc(Long ownerUserId, Integer year, Integer month);
    Optional<InventoryMonthlyStatsEntity> findByOwnerUserIdAndProductIdAndYearAndMonth(Long ownerUserId, Long productId, Integer year, Integer month);
}

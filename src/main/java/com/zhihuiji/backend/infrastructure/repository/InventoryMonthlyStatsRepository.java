package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMonthlyStatsRepository extends JpaRepository<InventoryMonthlyStatsEntity, Long> {
    List<InventoryMonthlyStatsEntity> findAllByOwnerUserIdOrderByYearAscMonthAscIdAsc(Long ownerUserId);

    @Query("SELECT e FROM InventoryMonthlyStatsEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.year ASC, e.month ASC, e.id ASC")
    List<InventoryMonthlyStatsEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    List<InventoryMonthlyStatsEntity> findAllByOwnerUserIdAndYearAndMonthOrderByProductNameAsc(Long ownerUserId, Integer year, Integer month);
    Optional<InventoryMonthlyStatsEntity> findByOwnerUserIdAndProductIdAndYearAndMonth(Long ownerUserId, Long productId, Integer year, Integer month);

    Page<InventoryMonthlyStatsEntity> findAllByOwnerUserIdAndYearAndMonthOrderByProductNameAsc(Long ownerUserId, Integer year, Integer month, Pageable pageable);
}

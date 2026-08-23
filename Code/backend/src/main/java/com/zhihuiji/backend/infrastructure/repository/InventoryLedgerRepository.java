package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedgerEntity, Long> {
    List<InventoryLedgerEntity> findAllByOwnerUserIdOrderByCreatedAtAscIdAsc(Long ownerUserId);
    Page<InventoryLedgerEntity> findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM InventoryLedgerEntity e WHERE e.ownerUserId = :ownerUserId AND e.createdAt >= :sinceTimestamp ORDER BY e.createdAt ASC, e.id ASC")
    List<InventoryLedgerEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndProductIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Long productId);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(Long ownerUserId, Long startAt, Long endAt);
    List<InventoryLedgerEntity> findAllByOwnerUserIdAndSourceTypeAndSourceIdOrderByCreatedAtDescIdDesc(Long ownerUserId, String sourceType, Long sourceId);

    Page<InventoryLedgerEntity> findAllByOwnerUserIdAndProductIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Long productId, Pageable pageable);
    Page<InventoryLedgerEntity> findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(Long ownerUserId, Long startAt, Long endAt, Pageable pageable);

    @Query("""
        SELECT e FROM InventoryLedgerEntity e
        WHERE e.ownerUserId = :ownerUserId
          AND (:productId IS NULL OR e.productId = :productId)
          AND (:startAt IS NULL OR e.createdAt >= :startAt)
          AND (:endAt IS NULL OR e.createdAt <= :endAt)
          AND (:sourceType IS NULL OR e.sourceType = :sourceType)
        ORDER BY e.createdAt DESC, e.id DESC
        """)
    Page<InventoryLedgerEntity> findByOwnerUserIdAndFilters(
        @Param("ownerUserId") Long ownerUserId,
        @Param("productId") Long productId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("sourceType") String sourceType,
        Pageable pageable
    );
}

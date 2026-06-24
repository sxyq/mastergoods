package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinanceRecordRepository extends JpaRepository<FinanceRecordEntity, Long> {
    java.util.Optional<FinanceRecordEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<FinanceRecordEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);

    @Query("SELECT e FROM FinanceRecordEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<FinanceRecordEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    @Query("SELECT r FROM FinanceRecordEntity r WHERE " +
        "r.ownerUserId = :ownerUserId AND " +
        "(:type IS NULL OR r.type = :type) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(r.recordNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:createdAfter IS NULL OR r.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR r.createdAt <= :createdBefore) " +
        "ORDER BY r.createdAt DESC")
    List<FinanceRecordEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("type") Integer type,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore
    );

    @Query("SELECT r FROM FinanceRecordEntity r WHERE " +
        "r.ownerUserId = :ownerUserId AND " +
        "(:type IS NULL OR r.type = :type) AND " +
        "(:keyword IS NULL OR :keyword = '' OR LOWER(r.recordNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
        "(:createdAfter IS NULL OR r.createdAt >= :createdAfter) AND " +
        "(:createdBefore IS NULL OR r.createdAt <= :createdBefore) " +
        "ORDER BY r.createdAt DESC")
    List<FinanceRecordEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("type") Integer type,
        @Param("createdAfter") Long createdAfter,
        @Param("createdBefore") Long createdBefore,
        Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN r.type = :incomeType THEN r.amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.type = :expenseType THEN r.amount ELSE 0 END), 0),
               COUNT(r)
        FROM FinanceRecordEntity r
        WHERE r.ownerUserId = :ownerUserId
          AND r.createdAt BETWEEN :startAt AND :endAt
    """)
    Object[] cashflowSummary(
        @Param("ownerUserId") Long ownerUserId,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        @Param("incomeType") Integer incomeType,
        @Param("expenseType") Integer expenseType
    );
}

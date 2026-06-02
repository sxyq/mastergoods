package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinanceRecordRepository extends JpaRepository<FinanceRecordEntity, Long> {
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
}

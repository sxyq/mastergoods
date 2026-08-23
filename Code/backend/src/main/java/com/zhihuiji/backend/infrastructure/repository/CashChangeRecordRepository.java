package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashChangeRecordRepository extends JpaRepository<CashChangeRecordEntity, Long> {
    List<CashChangeRecordEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<CashChangeRecordEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId, Pageable pageable);

    @Query("""
        SELECT e FROM CashChangeRecordEntity e
        WHERE e.ownerUserId = :ownerUserId
          AND e.createdAt BETWEEN :startAt AND :endAt
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(e.orderType) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR EXISTS (SELECT a.id FROM AccountEntity a WHERE a.ownerUserId = :ownerUserId AND a.id = e.accountId AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :keyword, '%')))))
        ORDER BY e.createdAt DESC, e.id DESC
        """)
    List<CashChangeRecordEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        Pageable pageable
    );
    Optional<CashChangeRecordEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

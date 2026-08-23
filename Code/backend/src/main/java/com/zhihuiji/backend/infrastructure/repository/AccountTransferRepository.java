package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransferRepository extends JpaRepository<AccountTransferEntity, Long> {
    List<AccountTransferEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<AccountTransferEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId, Pageable pageable);

    @Query("""
        SELECT e FROM AccountTransferEntity e
        WHERE e.ownerUserId = :ownerUserId
          AND e.createdAt BETWEEN :startAt AND :endAt
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(e.transferNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR EXISTS (SELECT a.id FROM AccountEntity a WHERE a.ownerUserId = :ownerUserId AND a.id = e.fromAccountId AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :keyword, '%'))))
               OR EXISTS (SELECT a.id FROM AccountEntity a WHERE a.ownerUserId = :ownerUserId AND a.id = e.toAccountId AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :keyword, '%')))))
        ORDER BY e.createdAt DESC, e.id DESC
        """)
    List<AccountTransferEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("startAt") Long startAt,
        @Param("endAt") Long endAt,
        Pageable pageable
    );
    List<AccountTransferEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);

    @Query("SELECT e FROM AccountTransferEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<AccountTransferEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    Optional<AccountTransferEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    boolean existsByOwnerUserIdAndTransferNo(Long ownerUserId, String transferNo);
}

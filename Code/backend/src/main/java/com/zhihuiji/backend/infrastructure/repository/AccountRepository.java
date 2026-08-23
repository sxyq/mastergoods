package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);
    List<AccountEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId, Pageable pageable);

    @Query("""
        SELECT e FROM AccountEntity e
        WHERE e.ownerUserId = :ownerUserId
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY e.sortOrder ASC, e.name ASC, e.id ASC
        """)
    List<AccountEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("SELECT COUNT(e) FROM AccountEntity e WHERE e.ownerUserId = :ownerUserId AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countByKeyword(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword);

    @Query("SELECT COALESCE(SUM(e.balance), 0) FROM AccountEntity e WHERE e.ownerUserId = :ownerUserId AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Double sumBalanceByKeyword(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(e) FROM AccountEntity e WHERE e.ownerUserId = :ownerUserId AND e.status = :status AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countByKeywordAndStatus(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword, @Param("status") Integer status);

    @Query("SELECT COUNT(e) FROM AccountEntity e WHERE e.ownerUserId = :ownerUserId AND e.balance < :threshold AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countByKeywordAndBalanceLessThan(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword, @Param("threshold") Double threshold);
    List<AccountEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);
    List<AccountEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM AccountEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<AccountEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    List<AccountEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);
    Optional<AccountEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM AccountEntity e WHERE e.id = :id AND e.ownerUserId = :ownerUserId")
    Optional<AccountEntity> findByIdAndOwnerUserIdForUpdate(
        @Param("id") Long id,
        @Param("ownerUserId") Long ownerUserId
    );
    boolean existsByOwnerUserIdAndCode(Long ownerUserId, String code);
    boolean existsByOwnerUserIdAndCodeAndIdNot(Long ownerUserId, String code, Long id);
    Optional<AccountEntity> findByOwnerUserIdAndIsDefaultTrue(Long ownerUserId);
}

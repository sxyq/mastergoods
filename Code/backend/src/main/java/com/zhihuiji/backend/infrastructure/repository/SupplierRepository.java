package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SupplierEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    @Query("""
        SELECT s FROM SupplierEntity s
        WHERE s.ownerUserId = :ownerUserId
          AND (:status IS NULL OR s.status = :status)
          AND (:groupId IS NULL OR s.groupId = :groupId)
          AND (
              :keyword IS NULL
              OR :keyword = ''
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY s.updatedAt DESC, s.id DESC
        """)
    List<SupplierEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("groupId") Long groupId
    );

    @Query("SELECT s FROM SupplierEntity s WHERE s.ownerUserId = :ownerUserId AND (:status IS NULL OR s.status = :status) AND (:groupId IS NULL OR s.groupId = :groupId) AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY s.updatedAt DESC, s.id DESC")
    List<SupplierEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("groupId") Long groupId,
        Pageable pageable
    );

    List<SupplierEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<SupplierEntity> findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(Long ownerUserId, Double balance, Pageable pageable);

    List<SupplierEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId);

    List<SupplierEntity> findAllByOwnerUserId(Long ownerUserId);

    @Query("SELECT e FROM SupplierEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY COALESCE(e.updatedAt, e.createdAt) ASC, e.id ASC")
    List<SupplierEntity> findChangedByOwnerUserId(@org.springframework.data.repository.query.Param("ownerUserId") Long ownerUserId, @org.springframework.data.repository.query.Param("sinceTimestamp") Long sinceTimestamp);

    List<SupplierEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    long countByOwnerUserIdAndGroupId(Long ownerUserId, Long groupId);

    long countByOwnerUserId(Long ownerUserId);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.balance > 0 THEN s.balance ELSE 0 END), 0) FROM SupplierEntity s WHERE s.ownerUserId = :ownerUserId")
    Double sumPositiveBalance(@org.springframework.data.repository.query.Param("ownerUserId") Long ownerUserId);

    long countByOwnerUserIdAndBalanceGreaterThan(Long ownerUserId, Double balance);

    @Query("""
        SELECT s FROM SupplierEntity s
        WHERE s.ownerUserId = :ownerUserId
          AND s.balance > :minBalance
          AND (:status IS NULL OR s.status = :status)
          AND (:groupId IS NULL OR s.groupId = :groupId)
          AND (
              :keyword IS NULL
              OR :keyword = ''
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY s.balance DESC, s.id DESC
        """)
    List<SupplierEntity> findPayablesByOwnerUserIdAndFilters(
        @Param("ownerUserId") Long ownerUserId,
        @Param("minBalance") Double minBalance,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("groupId") Long groupId,
        Pageable pageable
    );

    boolean existsByOwnerUserIdAndPhone(Long ownerUserId, String phone);

    boolean existsByOwnerUserIdAndPhoneAndIdNot(Long ownerUserId, String phone, Long id);

    Optional<SupplierEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SupplierEntity e WHERE e.id = :id AND e.ownerUserId = :ownerUserId")
    Optional<SupplierEntity> findByIdAndOwnerUserIdForUpdate(
        @Param("id") Long id,
        @Param("ownerUserId") Long ownerUserId
    );
}

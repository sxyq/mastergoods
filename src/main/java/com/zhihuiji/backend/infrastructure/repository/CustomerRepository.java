package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByOwnerUserIdAndPhone(Long ownerUserId, String phone);

    @Query("""
        SELECT c FROM CustomerEntity c
        WHERE c.ownerUserId = :ownerUserId
          AND (:status IS NULL OR c.status = :status)
          AND (:groupId IS NULL OR c.groupId = :groupId)
          AND (
              :keyword IS NULL
              OR :keyword = ''
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY c.updatedAt DESC
        """)
    List<CustomerEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("groupId") Long groupId
    );

    List<CustomerEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<CustomerEntity> findAllByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);

    List<CustomerEntity> findAllByOwnerUserId(Long ownerUserId);

    @Query("SELECT e FROM CustomerEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp")
    List<CustomerEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    List<CustomerEntity> findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(Long ownerUserId, Double balance, Pageable pageable);

    long countByOwnerUserIdAndGroupId(Long ownerUserId, Long groupId);

    @Query("SELECT COALESCE(SUM(CASE WHEN c.balance > 0 THEN c.balance ELSE 0 END), 0) FROM CustomerEntity c WHERE c.ownerUserId = :ownerUserId")
    Double sumPositiveBalance(@Param("ownerUserId") Long ownerUserId);

    long countByOwnerUserIdAndBalanceGreaterThan(Long ownerUserId, Double balance);

    Optional<CustomerEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

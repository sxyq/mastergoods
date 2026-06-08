package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByOwnerUserIdAndPhone(Long ownerUserId, String phone);

    List<CustomerEntity> findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCase(
        Long ownerUserIdForName,
        String nameKeyword,
        Long ownerUserIdForPhone,
        String phoneKeyword
    );

    List<CustomerEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<CustomerEntity> findAllByOwnerUserId(Long ownerUserId);

    List<CustomerEntity> findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(Long ownerUserId, Double balance, Pageable pageable);

    long countByOwnerUserIdAndGroupId(Long ownerUserId, Long groupId);

    @Query("SELECT COALESCE(SUM(CASE WHEN c.balance > 0 THEN c.balance ELSE 0 END), 0) FROM CustomerEntity c WHERE c.ownerUserId = :ownerUserId")
    Double sumPositiveBalance(@org.springframework.data.repository.query.Param("ownerUserId") Long ownerUserId);

    long countByOwnerUserIdAndBalanceGreaterThan(Long ownerUserId, Double balance);

    Optional<CustomerEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

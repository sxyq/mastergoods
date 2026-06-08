package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SupplierEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    List<SupplierEntity> findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCase(
        Long ownerUserIdForName,
        String nameKeyword,
        Long ownerUserIdForPhone,
        String phoneKeyword
    );

    List<SupplierEntity> findByOwnerUserIdAndStatus(Long ownerUserId, Integer status);

    List<SupplierEntity> findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCaseAndStatus(
        Long ownerUserIdForName,
        String nameKeyword,
        Long ownerUserIdForPhone,
        String phoneKeyword,
        Integer status
    );

    List<SupplierEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<SupplierEntity> findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(Long ownerUserId, Double balance, Pageable pageable);

    List<SupplierEntity> findAllByOwnerUserId(Long ownerUserId);

    List<SupplierEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    long countByOwnerUserIdAndGroupId(Long ownerUserId, Long groupId);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.balance > 0 THEN s.balance ELSE 0 END), 0) FROM SupplierEntity s WHERE s.ownerUserId = :ownerUserId")
    Double sumPositiveBalance(@org.springframework.data.repository.query.Param("ownerUserId") Long ownerUserId);

    boolean existsByOwnerUserIdAndPhone(Long ownerUserId, String phone);

    boolean existsByOwnerUserIdAndPhoneAndIdNot(Long ownerUserId, String phone, Long id);

    Optional<SupplierEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

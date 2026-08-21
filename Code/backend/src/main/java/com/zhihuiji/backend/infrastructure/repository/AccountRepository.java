package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);
    List<AccountEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);

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

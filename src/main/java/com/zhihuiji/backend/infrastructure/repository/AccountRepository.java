package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);
    List<AccountEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);
    Optional<AccountEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    boolean existsByOwnerUserIdAndCode(Long ownerUserId, String code);
    boolean existsByOwnerUserIdAndCodeAndIdNot(Long ownerUserId, String code, Long id);
    Optional<AccountEntity> findByOwnerUserIdAndIsDefaultTrue(Long ownerUserId);
}

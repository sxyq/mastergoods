package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAgentConfigRepository extends JpaRepository<AdminAgentConfigEntity, Long> {
    Optional<AdminAgentConfigEntity> findFirstByOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminAgentConfigEntity> findByScopeOwnerUserIdAndScopeStoreId(Long ownerUserId, Long storeId);
    Optional<AdminAgentConfigEntity> findByIdempotencyKey(String idempotencyKey);
}

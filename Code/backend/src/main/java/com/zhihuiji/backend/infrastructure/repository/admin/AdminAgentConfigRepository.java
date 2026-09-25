// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminAgentConfigEntity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAgentConfigRepository extends JpaRepository<AdminAgentConfigEntity, Long> {
    Optional<AdminAgentConfigEntity> findFirstByOrderByIdAsc();

    Optional<AdminAgentConfigEntity> findFirstByScopeOwnerUserIdAndScopeStoreId(Long ownerUserId, Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminAgentConfigEntity> findByScopeOwnerUserIdAndScopeStoreId(Long ownerUserId, Long storeId);
    Optional<AdminAgentConfigEntity> findByIdempotencyKey(String idempotencyKey);
}

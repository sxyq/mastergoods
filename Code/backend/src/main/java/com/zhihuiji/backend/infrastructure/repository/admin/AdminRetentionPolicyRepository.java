package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminRetentionPolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRetentionPolicyRepository extends JpaRepository<AdminRetentionPolicyEntity, Long> {
    Optional<AdminRetentionPolicyEntity> findByIdempotencyKey(String idempotencyKey);
}

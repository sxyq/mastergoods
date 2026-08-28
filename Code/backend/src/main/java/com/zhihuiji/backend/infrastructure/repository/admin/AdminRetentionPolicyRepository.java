package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminRetentionPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRetentionPolicyRepository extends JpaRepository<AdminRetentionPolicyEntity, Long> {}

package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.AdminScopeGrantEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminScopeGrantRepository extends JpaRepository<AdminScopeGrantEntity, Long> {
    List<AdminScopeGrantEntity> findAllByAdminAccountIdAndStatusOrderByIdAsc(Long adminAccountId, Integer status);
}

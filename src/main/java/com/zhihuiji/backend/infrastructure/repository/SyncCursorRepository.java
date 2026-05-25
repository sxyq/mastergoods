package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCursorRepository extends JpaRepository<SyncCursorEntity, String> {
}


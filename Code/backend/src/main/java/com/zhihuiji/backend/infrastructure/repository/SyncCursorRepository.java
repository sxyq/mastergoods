package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.domain.entity.SyncCursorId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCursorRepository extends JpaRepository<SyncCursorEntity, SyncCursorId> {
    Optional<SyncCursorEntity> findByOwnerUserIdAndClientId(Long ownerUserId, String clientId);
}

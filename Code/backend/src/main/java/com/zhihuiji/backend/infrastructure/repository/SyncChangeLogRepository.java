package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SyncChangeLogEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncChangeLogRepository extends JpaRepository<SyncChangeLogEntity, Long> {
    List<SyncChangeLogEntity> findByOwnerUserIdAndStoreIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        Long ownerUserId, Long storeId, Long sequenceNumber, Pageable pageable);

    List<SyncChangeLogEntity> findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
        Long ownerUserId, Long storeId, Long changedAt, Pageable pageable);
}

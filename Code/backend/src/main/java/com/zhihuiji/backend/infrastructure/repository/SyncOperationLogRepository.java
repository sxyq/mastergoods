package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SyncOperationLogEntity;
import com.zhihuiji.backend.domain.entity.SyncOperationLogId;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncOperationLogRepository
    extends JpaRepository<SyncOperationLogEntity, SyncOperationLogId> {

    Optional<SyncOperationLogEntity> findByOwnerUserIdAndOperationId(
        Long ownerUserId, String operationId);

    /**
     * Reserves an operation ID before its business write. The composite primary key makes
     * concurrent retries idempotent without relying on a read-then-write race.
     */
    @Modifying
    @Query(value = """
        INSERT INTO sync_operation_log (
            owner_user_id, store_id, operation_id, entity_type, entity_id, operation, processed_at, status
        ) VALUES (
            :ownerUserId, :storeId, :operationId, :entityType, :entityId, :operation, :processedAt, 'applied'
        ) ON CONFLICT (owner_user_id, operation_id) DO NOTHING
        """, nativeQuery = true)
    int reserveOperation(
        @Param("ownerUserId") Long ownerUserId,
        @Param("storeId") Long storeId,
        @Param("operationId") String operationId,
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("operation") String operation,
        @Param("processedAt") Long processedAt
    );
}

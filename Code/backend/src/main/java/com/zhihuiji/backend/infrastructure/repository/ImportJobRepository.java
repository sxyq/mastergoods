package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {
    List<ImportJobEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<ImportJobEntity> findAllByOwnerUserIdAndStatusOrderByUpdatedAtDesc(Long ownerUserId, String status);

    Optional<ImportJobEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Optional<ImportJobEntity> findByOwnerUserIdAndIdempotencyKey(Long ownerUserId, String idempotencyKey);

    // System-level worker queries: the background import-job executor polls and claims pending/running
    // jobs across all owners. These intentionally omit ownerUserId because job dispatch is system-wide;
    // per-owner isolation is enforced when the executor reads job details and applies imports.
    List<ImportJobEntity> findTop5ByStatusOrderByUpdatedAtAscCreatedAtAscIdAsc(String status);

    @Query("SELECT j FROM ImportJobEntity j WHERE j.status = :status AND j.lastHeartbeatAt < :threshold ORDER BY j.updatedAt ASC")
    List<ImportJobEntity> findByStatusAndLastHeartbeatAtBefore(@Param("status") String status, @Param("threshold") Long threshold);
}

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

    List<ImportJobEntity> findTop5ByStatusOrderByUpdatedAtAscCreatedAtAscIdAsc(String status);

    @Query("SELECT j FROM ImportJobEntity j WHERE j.status = :status AND j.lastHeartbeatAt < :threshold ORDER BY j.updatedAt ASC")
    List<ImportJobEntity> findByStatusAndLastHeartbeatAtBefore(@Param("status") String status, @Param("threshold") Long threshold);
}

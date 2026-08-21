package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, Long> {
    List<MediaAssetEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<MediaAssetEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndObjectKey(Long ownerUserId, String objectKey);
}

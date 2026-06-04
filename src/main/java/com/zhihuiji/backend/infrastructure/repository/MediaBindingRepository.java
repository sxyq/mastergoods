package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.MediaBindingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaBindingRepository extends JpaRepository<MediaBindingEntity, Long> {
    List<MediaBindingEntity> findAllByOwnerUserIdAndTargetTypeAndTargetIdOrderBySortOrderAscIdAsc(Long ownerUserId, String targetType, Long targetId);

    List<MediaBindingEntity> findAllByOwnerUserIdAndAssetIdOrderByCreatedAtAsc(Long ownerUserId, Long assetId);

    Optional<MediaBindingEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndAssetIdAndTargetTypeAndTargetId(Long ownerUserId, Long assetId, String targetType, Long targetId);

    void deleteAllByOwnerUserIdAndAssetId(Long ownerUserId, Long assetId);
}

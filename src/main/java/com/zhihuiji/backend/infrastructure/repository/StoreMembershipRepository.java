package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreMembershipRepository extends JpaRepository<StoreMembershipEntity, Long> {
    Optional<StoreMembershipEntity> findByUserId(Long userId);

    Optional<StoreMembershipEntity> findByOwnerUserIdAndUserId(Long ownerUserId, Long userId);

    List<StoreMembershipEntity> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    List<StoreMembershipEntity> findByOwnerUserIdAndUserIdIn(Long ownerUserId, Collection<Long> userIds);
}

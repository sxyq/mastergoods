package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreMembershipRepository extends JpaRepository<StoreMembershipEntity, Long> {
    Optional<StoreMembershipEntity> findByUserId(Long userId);

    Optional<StoreMembershipEntity> findByOwnerUserIdAndUserId(Long ownerUserId, Long userId);

    Optional<StoreMembershipEntity> findByOwnerUserIdAndStoreIdAndUserId(
        Long ownerUserId, Long storeId, Long userId);

    long countByOwnerUserId(Long ownerUserId);

    List<StoreMembershipEntity> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    List<StoreMembershipEntity> findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(
        Long ownerUserId, Long storeId);

    Page<StoreMembershipEntity> findByOwnerUserIdAndStoreIdOrderByCreatedAtAsc(
        Long ownerUserId, Long storeId, Pageable pageable);

    long countByOwnerUserIdAndStoreId(Long ownerUserId, Long storeId);

    List<StoreMembershipEntity> findByOwnerUserIdAndUserIdIn(Long ownerUserId, Collection<Long> userIds);
}

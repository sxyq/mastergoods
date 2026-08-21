package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.StoreEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
    Optional<StoreEntity> findByOwnerUserId(Long ownerUserId);
}

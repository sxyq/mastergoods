package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SalesReturnItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItemEntity, Long> {
    List<SalesReturnItemEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);

    java.util.Optional<SalesReturnItemEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<SalesReturnItemEntity> findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(Long ownerUserId, Long returnId);

    List<SalesReturnItemEntity> findByReturnIdOrderByCreatedAtAsc(Long returnId);

    void deleteByOwnerUserIdAndReturnId(Long ownerUserId, Long returnId);
}

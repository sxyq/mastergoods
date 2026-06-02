package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ProductUnitEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUnitRepository extends JpaRepository<ProductUnitEntity, Long> {
    List<ProductUnitEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);

    List<ProductUnitEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    Optional<ProductUnitEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}

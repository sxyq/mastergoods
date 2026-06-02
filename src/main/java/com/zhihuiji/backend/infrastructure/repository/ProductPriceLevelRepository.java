package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceLevelRepository extends JpaRepository<ProductPriceLevelEntity, Long> {
    List<ProductPriceLevelEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);

    List<ProductPriceLevelEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    Optional<ProductPriceLevelEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndCode(Long ownerUserId, String code);

    boolean existsByOwnerUserIdAndCodeAndIdNot(Long ownerUserId, String code, Long id);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}

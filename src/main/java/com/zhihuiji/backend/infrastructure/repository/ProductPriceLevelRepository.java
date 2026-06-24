package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPriceLevelRepository extends JpaRepository<ProductPriceLevelEntity, Long> {
    List<ProductPriceLevelEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);

    @Query("SELECT e FROM ProductPriceLevelEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.sortOrder ASC, e.name ASC")
    List<ProductPriceLevelEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    List<ProductPriceLevelEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    Optional<ProductPriceLevelEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndCode(Long ownerUserId, String code);

    boolean existsByOwnerUserIdAndCodeAndIdNot(Long ownerUserId, String code, Long id);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}

package com.zhihuiji.backend.infrastructure.repository.product;

import com.zhihuiji.backend.domain.entity.product.ProductCategoryEntity;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Long> {
    List<ProductCategoryEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId);
    List<ProductCategoryEntity> findAllByOwnerUserIdOrderBySortOrderAscNameAsc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM ProductCategoryEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.sortOrder ASC, e.name ASC")
    List<ProductCategoryEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    List<ProductCategoryEntity> findAllByOwnerUserIdAndIdIn(Long ownerUserId, Collection<Long> ids);

    Optional<ProductCategoryEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}

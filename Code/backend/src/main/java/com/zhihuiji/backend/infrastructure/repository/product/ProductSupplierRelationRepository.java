package com.zhihuiji.backend.infrastructure.repository.product;

import com.zhihuiji.backend.domain.entity.product.ProductSupplierRelationEntity;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSupplierRelationRepository extends JpaRepository<ProductSupplierRelationEntity, Long> {
    List<ProductSupplierRelationEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);
    List<ProductSupplierRelationEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId, Pageable pageable);

    @Query("SELECT e FROM ProductSupplierRelationEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<ProductSupplierRelationEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

    List<ProductSupplierRelationEntity> findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(
        Long ownerUserId,
        Long productId
    );

    List<ProductSupplierRelationEntity> findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(
        Long ownerUserId,
        Long productId,
        Pageable pageable
    );

    List<ProductSupplierRelationEntity> findAllByOwnerUserIdAndProductIdInOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(
        Long ownerUserId,
        Collection<Long> productIds
    );

    Optional<ProductSupplierRelationEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Optional<ProductSupplierRelationEntity> findByOwnerUserIdAndProductIdAndIsDefaultTrue(Long ownerUserId, Long productId);

    boolean existsByOwnerUserIdAndProductIdAndSupplierId(Long ownerUserId, Long productId, Long supplierId);

    boolean existsByOwnerUserIdAndProductIdAndSupplierIdAndIdNot(Long ownerUserId, Long productId, Long supplierId, Long id);

    void deleteAllByOwnerUserIdAndProductId(Long ownerUserId, Long productId);

    void deleteByIdAndOwnerUserId(Long id, Long ownerUserId);
}

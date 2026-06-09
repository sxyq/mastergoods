package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ProductEntity;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByOwnerUserIdAndCode(Long ownerUserId, String code);

    List<ProductEntity> findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndCodeContainingIgnoreCase(
        Long ownerUserIdForName,
        String nameKeyword,
        Long ownerUserIdForCode,
        String codeKeyword
    );

    List<ProductEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<ProductEntity> findAllByOwnerUserId(Long ownerUserId);

    long countByOwnerUserId(Long ownerUserId);

    long countByOwnerUserIdAndCategoryId(Long ownerUserId, Long categoryId);

    long countByOwnerUserIdAndUnitId(Long ownerUserId, Long unitId);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM ProductEntity p WHERE p.ownerUserId = :ownerUserId")
    Double sumStockByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.ownerUserId = :ownerUserId AND p.stock <= p.safeStock")
    long countLowStockByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Query("SELECT p FROM ProductEntity p WHERE p.ownerUserId = :ownerUserId AND p.stock <= p.safeStock ORDER BY p.stock ASC, p.updatedAt DESC")
    List<ProductEntity> findLowStockProducts(@Param("ownerUserId") Long ownerUserId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.ownerUserId = :ownerUserId and p.id = :id")
    Optional<ProductEntity> findByIdForUpdate(@Param("ownerUserId") Long ownerUserId, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.ownerUserId = :ownerUserId and p.code = :code")
    Optional<ProductEntity> findByCodeForUpdate(@Param("ownerUserId") Long ownerUserId, @Param("code") String code);

    Optional<ProductEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

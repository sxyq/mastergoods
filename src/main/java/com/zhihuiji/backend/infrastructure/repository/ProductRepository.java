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

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.ownerUserId = :ownerUserId
          AND (:status IS NULL OR p.status = :status)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:unitId IS NULL OR p.unitId = :unitId)
        ORDER BY p.updatedAt DESC
        """)
    List<ProductEntity> findAllByOwnerUserIdAndFiltersOrderByUpdatedAtDesc(
        @Param("ownerUserId") Long ownerUserId,
        @Param("status") Integer status,
        @Param("categoryId") Long categoryId,
        @Param("unitId") Long unitId
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.ownerUserId = :ownerUserId
          AND (:status IS NULL OR p.status = :status)
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:unitId IS NULL OR p.unitId = :unitId)
          AND (
              LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY p.updatedAt DESC
        """)
    List<ProductEntity> findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("categoryId") Long categoryId,
        @Param("unitId") Long unitId
    );

    List<ProductEntity> findAllByOwnerUserIdOrderByNameAsc(Long ownerUserId, Pageable pageable);

    List<ProductEntity> findAllByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);

    List<ProductEntity> findAllByOwnerUserId(Long ownerUserId);

    @Query("SELECT e FROM ProductEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp")
    List<ProductEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);

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

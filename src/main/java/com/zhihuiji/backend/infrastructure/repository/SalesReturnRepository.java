package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesReturnRepository extends JpaRepository<SalesReturnEntity, Long> {

    List<SalesReturnEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<SalesReturnEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<SalesReturnEntity> findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc(Long ownerUserId, Long originalOrderId);

    List<SalesReturnEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, Integer status);

    @Query("SELECT sr FROM SalesReturnEntity sr WHERE sr.ownerUserId = :ownerUserId " +
        "AND (:keyword IS NULL OR LOWER(sr.returnNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(sr.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "AND (:status IS NULL OR sr.status = :status) " +
        "ORDER BY sr.createdAt DESC")
    List<SalesReturnEntity> search(
        @Param("ownerUserId") Long ownerUserId,
        @Param("keyword") String keyword,
        @Param("status") Integer status
    );
}

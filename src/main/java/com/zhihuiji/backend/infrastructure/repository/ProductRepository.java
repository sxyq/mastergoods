package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.ProductEntity;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByCode(String code);

    List<ProductEntity> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String nameKeyword, String codeKeyword);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :id")
    Optional<ProductEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.code = :code")
    Optional<ProductEntity> findByCodeForUpdate(@Param("code") String code);
}

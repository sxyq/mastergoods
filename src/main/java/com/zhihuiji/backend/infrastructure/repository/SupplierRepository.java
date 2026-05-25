package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SupplierEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    List<SupplierEntity> findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(String nameKeyword, String phoneKeyword);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByPhone(String phone);

    List<CustomerEntity> findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(String nameKeyword, String phoneKeyword);
}


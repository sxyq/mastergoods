package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.UserCreditEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreditRepository extends JpaRepository<UserCreditEntity, Long> {

    Optional<UserCreditEntity> findByOwnerUserId(Long ownerUserId);
}

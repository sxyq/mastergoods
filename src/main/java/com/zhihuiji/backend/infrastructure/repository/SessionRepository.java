package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    Optional<SessionEntity> findByTokenAndIsActiveTrue(String token);

    Optional<SessionEntity> findByRefreshTokenAndIsActiveTrue(String refreshToken);

    long countByUserIdAndIsActiveTrue(Long userId);
}

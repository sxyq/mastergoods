package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.SessionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    Optional<SessionEntity> findByTokenAndIsActiveTrue(String token);

    Optional<SessionEntity> findByRefreshTokenAndIsActiveTrue(String refreshToken);

    List<SessionEntity> findByUserIdAndIsActiveTrue(Long userId);

    long deleteByUserId(Long userId);

    @Query("SELECT s.userId AS userId, COUNT(s.id) AS activeCount FROM SessionEntity s " +
        "WHERE s.userId IN :userIds AND s.isActive = true GROUP BY s.userId")
    List<ActiveSessionCount> countActiveSessionsByUserIds(@Param("userIds") Collection<Long> userIds);

    long countByUserIdAndIsActiveTrue(Long userId);

    interface ActiveSessionCount {
        Long getUserId();

        Long getActiveCount();
    }
}

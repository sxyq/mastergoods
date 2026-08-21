package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentNotificationRepository extends JpaRepository<AgentNotificationEntity, Long> {
    List<AgentNotificationEntity> findTop30ByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<AgentNotificationEntity> findTop30ByOwnerUserIdAndIsReadFalseOrderByCreatedAtDesc(Long ownerUserId);

    List<AgentNotificationEntity> findTop30ByOwnerUserIdAndIsDeliveredFalseOrderByCreatedAtDesc(Long ownerUserId);

    List<AgentNotificationEntity> findTop30ByOwnerUserIdAndIsReadFalseAndIsDeliveredFalseOrderByCreatedAtDesc(Long ownerUserId);

    long countByOwnerUserIdAndIsReadFalse(Long ownerUserId);

    long countByIsReadFalse();

    void deleteAllByOwnerUserIdAndIsReadTrue(Long ownerUserId);

    void deleteAllByOwnerUserIdIn(Collection<Long> ownerUserIds);

    Optional<AgentNotificationEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentNotificationRepository extends JpaRepository<AgentNotificationEntity, Long> {
    List<AgentNotificationEntity> findTop30ByOrderByCreatedAtDesc();

    List<AgentNotificationEntity> findTop30ByIsReadFalseOrderByCreatedAtDesc();

    List<AgentNotificationEntity> findTop30ByIsDeliveredFalseOrderByCreatedAtDesc();

    List<AgentNotificationEntity> findTop30ByIsReadFalseAndIsDeliveredFalseOrderByCreatedAtDesc();
}

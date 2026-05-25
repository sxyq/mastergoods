package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTaskRepository extends JpaRepository<AgentTaskEntity, Long> {
    List<AgentTaskEntity> findTop20ByOrderByCreatedAtDesc();

    Optional<AgentTaskEntity> findFirstByTaskTypeAndStatusInOrderByCreatedAtDesc(String taskType, Collection<String> statuses);
}

// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTaskRepository extends JpaRepository<AgentTaskEntity, Long> {
    List<AgentTaskEntity> findTop20ByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<AgentTaskEntity> findFirstByOwnerUserIdAndTaskTypeAndStatusInOrderByCreatedAtDesc(Long ownerUserId, String taskType, Collection<String> statuses);

    Optional<AgentTaskEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    void deleteAllByOwnerUserIdIn(Collection<Long> ownerUserIds);
}

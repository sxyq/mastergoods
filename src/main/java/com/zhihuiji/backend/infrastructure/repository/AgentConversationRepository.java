package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentConversationRepository extends JpaRepository<AgentConversationEntity, Long> {
    List<AgentConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId);

    List<AgentConversationEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    Optional<AgentConversationEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

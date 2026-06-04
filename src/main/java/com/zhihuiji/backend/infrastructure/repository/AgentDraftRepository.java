package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentDraftRepository extends JpaRepository<AgentDraftEntity, Long> {
    List<AgentDraftEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId);

    List<AgentDraftEntity> findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(Long ownerUserId, Long conversationId);

    Optional<AgentDraftEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);
}

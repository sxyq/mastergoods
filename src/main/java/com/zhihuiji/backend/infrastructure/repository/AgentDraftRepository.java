package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentDraftRepository extends JpaRepository<AgentDraftEntity, Long> {
    List<AgentDraftEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId);

    List<AgentDraftEntity> findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    List<AgentDraftEntity> findAllByOwnerUserIdAndStatusIgnoreCaseOrderByUpdatedAtDescIdDesc(
        Long ownerUserId,
        String status,
        Pageable pageable
    );

    List<AgentDraftEntity> findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(Long ownerUserId, Long conversationId);

    List<AgentDraftEntity> findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(
        Long ownerUserId,
        Long conversationId,
        Pageable pageable
    );

    long countByOwnerUserIdAndStatusIgnoreCase(Long ownerUserId, String status);

    Optional<AgentDraftEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);
}

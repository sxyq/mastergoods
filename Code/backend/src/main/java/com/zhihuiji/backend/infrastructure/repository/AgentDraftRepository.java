package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("""
        update AgentDraftEntity d
           set d.status = :newStatus, d.updatedAt = :updatedAt
         where d.id = :draftId
           and d.ownerUserId = :ownerUserId
           and lower(d.status) = lower(:expectedStatus)
        """)
    int updateStatusIfCurrent(
        @Param("draftId") Long draftId,
        @Param("ownerUserId") Long ownerUserId,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("updatedAt") Long updatedAt
    );

    void deleteAllByOwnerUserIdAndConversationId(Long ownerUserId, Long conversationId);
}

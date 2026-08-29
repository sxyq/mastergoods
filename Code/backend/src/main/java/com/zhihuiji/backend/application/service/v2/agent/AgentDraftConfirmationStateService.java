package com.zhihuiji.backend.application.service.v2.agent;

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists confirmation failure state independently from the business write transaction. */
@Service
public class AgentDraftConfirmationStateService {
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CONFIRMING = "confirming";

    private final AgentDraftRepository repository;

    public AgentDraftConfirmationStateService(AgentDraftRepository repository) {
        this.repository = repository;
    }

    /**
     * Restores a claimed draft to active after a business service rejects the
     * confirmation. A separate transaction keeps the failure visible even when
     * the business transaction has been marked rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long draftId, Long ownerUserId, String failureReason) {
        Optional<AgentDraftEntity> optional = repository.findByIdAndOwnerUserId(draftId, ownerUserId);
        if (optional.isEmpty()) {
            return;
        }
        AgentDraftEntity entity = optional.get();
        if (!STATUS_CONFIRMING.equalsIgnoreCase(entity.getStatus())) {
            return;
        }
        entity.setStatus(STATUS_ACTIVE);
        entity.setFailureReason(failureReason);
        entity.setUpdatedAt(System.currentTimeMillis());
        repository.saveAndFlush(entity);
    }
}

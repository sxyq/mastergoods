package com.zhihuiji.backend.application.service.admin;

import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTerminalStatus;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only Agent run summaries for API-ADM-05. */
@Service
public class AdminAgentObservabilityService {
    private static final int MAX_RUN_ID_LENGTH = 64;

    private final AdminAuthorizationService authorizationService;
    private final AdminAgentQueryRepository agentQueryRepository;

    public AdminAgentObservabilityService(
        AdminAuthorizationService authorizationService,
        AdminAgentQueryRepository agentQueryRepository
    ) {
        this.authorizationService = authorizationService;
        this.agentQueryRepository = agentQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminAgentDtos.RunSummary> listRuns(
        AdminPrincipal principal,
        String runId,
        Long conversationId,
        String status,
        Instant from,
        Instant to,
        Long requestedOwnerUserId,
        Long requestedStoreId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.AGENT_RUN_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        requirePersistedStoreScope(scope);
        validateTimeRange(from, to);
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Page<AgentRunAuditEntity> result = agentQueryRepository.findRuns(
            queryScope.allOwners(),
            queryScope.ownerUserIds(),
            normalizeRunId(runId),
            conversationId,
            normalizeStatus(status),
            from == null ? null : from.toEpochMilli(),
            to == null ? null : to.toEpochMilli(),
            PaginationUtils.pageable(page, size)
        );
        List<AdminAgentDtos.RunSummary> items = result.getContent().stream()
            .map(this::toSummary)
            .toList();
        return new AdminPageDtos.PageResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.hasNext(),
            Instant.now(),
            AdminScopeDtos.Scope.from(scope),
            "PARTIAL"
        );
    }

    @Transactional(readOnly = true)
    public AdminAgentDtos.RunSummary getRun(
        AdminPrincipal principal,
        String runId,
        Long requestedOwnerUserId,
        Long requestedStoreId
    ) {
        AdminDataScope scope = authorizationService.authorize(
            principal,
            AdminPermission.AGENT_RUN_READ,
            requestedOwnerUserId,
            requestedStoreId
        );
        requirePersistedStoreScope(scope);
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        AgentRunAuditEntity run = agentQueryRepository.findRun(
            normalizeRequiredRunId(runId),
            queryScope.allOwners(),
            queryScope.ownerUserIds()
        ).orElseThrow(() -> new AccessDeniedException("administrator resource not visible"));
        return toSummary(run);
    }

    private void requirePersistedStoreScope(AdminDataScope scope) {
        if (!scope.allOwners() && !scope.storeIds().isEmpty()) {
            throw new IllegalStateException("Agent store scope is unavailable in persisted run audits");
        }
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private String normalizeRunId(String runId) {
        if (runId == null) {
            return null;
        }
        String normalized = runId.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_RUN_ID_LENGTH) {
            throw new IllegalArgumentException("runId is too long");
        }
        return normalized;
    }

    private String normalizeRequiredRunId(String runId) {
        String normalized = normalizeRunId(runId);
        if (normalized == null) {
            throw new IllegalArgumentException("runId is required");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? null : status.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private AdminAgentDtos.RunSummary toSummary(AgentRunAuditEntity run) {
        Instant startedAt = instant(run.getStartedAt());
        Instant completedAt = instant(run.getCompletedAt());
        Long durationMs = startedAt == null || completedAt == null
            ? null
            : Math.max(0L, Duration.between(startedAt, completedAt).toMillis());
        return new AdminAgentDtos.RunSummary(
            run.getRunId(),
            id(run.getConversationId()),
            null,
            id(run.getOwnerUserId()),
            null,
            terminalStatus(run.getStatus()),
            null,
            startedAt,
            completedAt,
            durationMs,
            null,
            null,
            run.getToolCount(),
            null,
            null,
            null,
            AdminAgentDtos.TokenSource.UNAVAILABLE,
            true,
            "PARTIAL"
        );
    }

    private String terminalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("running".equalsIgnoreCase(status)) {
            return "RUNNING";
        }
        try {
            return AgentTerminalStatus.fromAuditStatus(status).name();
        } catch (RuntimeException ignored) {
            return status.trim().toUpperCase(Locale.ROOT);
        }
    }

    private String id(Long value) {
        return value == null ? null : value.toString();
    }

    private Instant instant(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}

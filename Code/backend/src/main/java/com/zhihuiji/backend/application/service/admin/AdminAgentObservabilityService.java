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
    private static final int MAX_FILTER_LENGTH = 128;

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
        Long actorUserId,
        String toolName,
        String modelId,
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
        validateTimeRange(from, to);
        String normalizedToolName = normalizeFilter(toolName, "toolName");
        String normalizedModelId = normalizeFilter(modelId, "modelId");
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        Page<AgentRunAuditEntity> result = queryScope.storeIds().equals(java.util.Set.of(Long.MIN_VALUE))
            ? (hasRunFilters(actorUserId, normalizedToolName, normalizedModelId)
                ? agentQueryRepository.findRunsFiltered(
                    queryScope.allOwners(), queryScope.ownerUserIds(), normalizeRunId(runId), conversationId,
                    actorUserId, normalizedToolName, normalizedModelId, normalizeStatus(status),
                    from == null ? null : from.toEpochMilli(), to == null ? null : to.toEpochMilli(),
                    PaginationUtils.pageable(page, size))
                : agentQueryRepository.findRuns(
                queryScope.allOwners(), queryScope.ownerUserIds(), normalizeRunId(runId), conversationId,
                normalizeStatus(status), from == null ? null : from.toEpochMilli(),
                to == null ? null : to.toEpochMilli(), PaginationUtils.pageable(page, size)))
            : (hasRunFilters(actorUserId, normalizedToolName, normalizedModelId)
                ? agentQueryRepository.findRunsScopedFiltered(
                    queryScope.allOwners(), queryScope.ownerUserIds(), queryScope.allStores(), queryScope.storeIds(),
                    normalizeRunId(runId), conversationId, actorUserId, normalizedToolName, normalizedModelId,
                    normalizeStatus(status), from == null ? null : from.toEpochMilli(),
                    to == null ? null : to.toEpochMilli(), PaginationUtils.pageable(page, size))
                : agentQueryRepository.findRunsScoped(
                queryScope.allOwners(), queryScope.ownerUserIds(), queryScope.allStores(), queryScope.storeIds(),
                normalizeRunId(runId), conversationId, null, normalizeStatus(status),
                from == null ? null : from.toEpochMilli(), to == null ? null : to.toEpochMilli(),
                PaginationUtils.pageable(page, size)));
        List<AdminAgentDtos.RunSummary> items = result.getContent().stream()
            .map(run -> toSummary(run, scope))
            .toList();
        return new AdminPageDtos.PageResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.hasNext(),
            Instant.now(),
            AdminScopeDtos.Scope.from(scope),
            scopeCompleteness(scope)
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
        AdminScopeQuery queryScope = AdminScopeQuery.from(scope);
        String normalizedRunId = normalizeRequiredRunId(runId);
        java.util.Optional<AgentRunAuditEntity> run = queryScope.storeIds().equals(java.util.Set.of(Long.MIN_VALUE))
            ? agentQueryRepository.findRun(normalizedRunId, queryScope.allOwners(), queryScope.ownerUserIds())
            : agentQueryRepository.findRunScoped(normalizedRunId, queryScope.allOwners(), queryScope.ownerUserIds(),
                queryScope.allStores(), queryScope.storeIds());
        return toSummary(run.orElseThrow(() -> new AccessDeniedException("administrator resource not visible")), scope);
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
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if ("RUNNING".equalsIgnoreCase(normalized)) {
            return "running";
        }
        try {
            return AgentTerminalStatus.valueOf(normalized.toUpperCase(Locale.ROOT)).auditStatus();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("terminalStatus is invalid");
        }
    }

    private boolean hasRunFilters(Long actorUserId, String toolName, String modelId) {
        return actorUserId != null || toolName != null || modelId != null;
    }

    private String normalizeFilter(String value, String field) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_FILTER_LENGTH || normalized.indexOf('%') >= 0
            || normalized.indexOf('_') >= 0 || normalized.indexOf('"') >= 0
            || normalized.indexOf('\\') >= 0 || normalized.indexOf('\'') >= 0
            || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    private AdminAgentDtos.RunSummary toSummary(AgentRunAuditEntity run, AdminDataScope scope) {
        Instant startedAt = instant(run.getStartedAt());
        Instant completedAt = instant(run.getCompletedAt());
        Long durationMs = startedAt == null || completedAt == null
            ? null
            : Math.max(0L, Duration.between(startedAt, completedAt).toMillis());
        return new AdminAgentDtos.RunSummary(
            run.getRunId(),
            id(run.getConversationId()),
            id(run.getActorUserId()),
            id(run.getOwnerUserId()),
            id(run.getStoreId()),
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
            scopeCompleteness(scope)
        );
    }

    private String scopeCompleteness(AdminDataScope scope) {
        return scope.allOwners() ? "COMPLETE" : "PARTIAL";
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

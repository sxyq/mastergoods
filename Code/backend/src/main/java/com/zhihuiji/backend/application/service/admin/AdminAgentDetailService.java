package com.zhihuiji.backend.application.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentCheckpointQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentDetailQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentDraftQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentEventQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminScopeQuery;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Detail-only Agent projections. Raw message/event payloads never leave this service. */
@Service
public class AdminAgentDetailService {
    private static final int MAX_CONTENT_LENGTH = 600;

    private final AdminAuthorizationService authorizationService;
    private final AdminAgentQueryRepository runRepository;
    private final AdminAgentDetailQueryRepository messageRepository;
    private final AdminAgentEventQueryRepository eventRepository;
    private final AdminAgentCheckpointQueryRepository checkpointRepository;
    private final AdminAgentDraftQueryRepository draftRepository;
    private final ObjectMapper objectMapper;

    public AdminAgentDetailService(
        AdminAuthorizationService authorizationService,
        AdminAgentQueryRepository runRepository,
        AdminAgentDetailQueryRepository messageRepository,
        AdminAgentEventQueryRepository eventRepository,
        AdminAgentCheckpointQueryRepository checkpointRepository,
        AdminAgentDraftQueryRepository draftRepository,
        ObjectMapper objectMapper
    ) {
        this.authorizationService = authorizationService;
        this.runRepository = runRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
        this.checkpointRepository = checkpointRepository;
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminAgentDtos.Message> messages(
        AdminPrincipal principal, String conversationId, boolean includeContent,
        Integer page, Integer size, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, includeContent);
        long id = parseId(conversationId, "conversationId");
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Page<AgentMessageEntity> result = messageRepository.findMessages(
            id, query.allOwners(), query.ownerUserIds(), PaginationUtils.pageable(page, size)
        );
        List<AdminAgentDtos.Message> items = result.getContent().stream()
            .map(message -> toMessage(message, includeContent && scope.contentMode() == AdminDataScope.ContentMode.AUTHORIZED))
            .toList();
        return new AdminPageDtos.PageResponse<>(items, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.hasNext(), Instant.now(), AdminScopeDtos.Scope.from(scope),
            scopeCompleteness(scope));
    }

    @Transactional(readOnly = true)
    public AdminAgentDtos.EventPage events(
        AdminPrincipal principal, String runId, Integer afterSequence, boolean includeContent,
        Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, includeContent);
        AgentRunAuditEntity run = visibleRun(runId, scope);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        List<AgentRunAuditEventEntity> events = eventRepository.findEvents(
            run.getRunId(), query.allOwners(), query.ownerUserIds(), afterSequence
        );
        boolean authorized = includeContent && scope.contentMode() == AdminDataScope.ContentMode.AUTHORIZED;
        List<AdminAgentDtos.Event> items = events.stream().map(event -> toEvent(event, authorized)).toList();
        return new AdminAgentDtos.EventPage(items, items.size(), isContiguous(items));
    }

    @Transactional(readOnly = true)
    public AdminAgentDtos.ContextResponse context(
        AdminPrincipal principal, String runId, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        AgentRunAuditEntity run = visibleRun(runId, scope);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        List<AgentContextCheckpointEntity> checkpoints = checkpointRepository.findCheckpoints(
            run.getConversationId(), query.allOwners(), query.ownerUserIds()
        );
        List<AdminAgentDtos.ContextCheckpoint> items = checkpoints.stream().map(this::toCheckpoint).toList();
        Integer input = items.stream().map(AdminAgentDtos.ContextCheckpoint::estimatedInputTokens)
            .filter(value -> value != null).max(Integer::compareTo).orElse(null);
        Integer output = items.stream().map(AdminAgentDtos.ContextCheckpoint::estimatedOutputTokens)
            .filter(value -> value != null).max(Integer::compareTo).orElse(null);
        return new AdminAgentDtos.ContextResponse(run.getRunId(), id(run.getConversationId()), null,
            input, output, items, true, scopeCompleteness(scope));
    }

    @Transactional(readOnly = true)
    public List<AdminAgentDtos.Draft> drafts(
        AdminPrincipal principal, String runId, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        AgentRunAuditEntity run = visibleRun(runId, scope);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        return draftRepository.findDrafts(run.getConversationId(), query.allOwners(), query.ownerUserIds())
            .stream().map(this::toDraft).toList();
    }

    @Transactional(readOnly = true)
    public AdminAgentDtos.UsagePage usage(
        AdminPrincipal principal, Instant from, Instant to, Long ownerUserId, Long storeId,
        Integer page, Integer size
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Page<AgentRunAuditEntity> runs = runRepository.findRuns(query.allOwners(), query.ownerUserIds(),
            null, null, null, from == null ? null : from.toEpochMilli(),
            to == null ? null : to.toEpochMilli(), PaginationUtils.pageable(page, size));
        List<AdminAgentDtos.Usage> items = runs.getContent().stream().map(run -> usageFor(run, query, scope)).toList();
        return new AdminAgentDtos.UsagePage(items, runs.getTotalElements(), Instant.now());
    }

    private AdminDataScope authorize(AdminPrincipal principal, Long ownerUserId, Long storeId, boolean includeContent) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.AGENT_RUN_READ, ownerUserId, storeId);
        if (!scope.allOwners() && !scope.storeIds().isEmpty()) {
            throw new IllegalStateException("Agent store scope is unavailable in persisted run audits");
        }
        if (includeContent) {
            authorizationService.requirePermission(principal, AdminPermission.AGENT_CONTENT_READ);
        }
        return scope;
    }

    private AgentRunAuditEntity visibleRun(String runId, AdminDataScope scope) {
        String normalized = normalizeRunId(runId);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        return runRepository.findRun(normalized, query.allOwners(), query.ownerUserIds())
            .orElseThrow(() -> new IllegalArgumentException("agent run not found"));
    }

    private AdminAgentDtos.Message toMessage(AgentMessageEntity entity, boolean authorized) {
        String redaction = authorized ? "FULL_ALLOWED" : "REDACTED";
        String content = authorized ? redact(entity.getContent()) : "REDACTED";
        return new AdminAgentDtos.Message(id(entity.getId()), id(entity.getConversationId()), entity.getRunId(),
            entity.getRole(), entity.getMessageType(), content, redaction, instant(entity.getCreatedAt()));
    }

    private AdminAgentDtos.Event toEvent(AgentRunAuditEventEntity entity, boolean authorized) {
        JsonNode payload = parse(entity.getPayloadJson());
        String argument = authorized ? safe(payload, "input_summary") : null;
        String result = authorized ? safe(payload, "result_summary") : null;
        String redaction = authorized ? "PARTIAL" : "REDACTED";
        Long duration = number(payload, "duration_ms");
        return new AdminAgentDtos.Event(entity.getEventId(), entity.getRunId(), entity.getSeq(),
            entity.getEventType(), safe(payload, "tool_name"), first(payload, "call_id", "tool_call_id"),
            instant(entity.getCreatedAt()), eventStatus(entity.getEventType()), duration,
            argument, result, AdminAgentDtos.RedactionState.valueOf(redaction));
    }

    private AdminAgentDtos.ContextCheckpoint toCheckpoint(AgentContextCheckpointEntity entity) {
        return new AdminAgentDtos.ContextCheckpoint(id(entity.getId()), id(entity.getConversationId()),
            id(entity.getSourceBoundaryMessageId()), entity.getSourceMessageCount(), entity.getSummaryVersion(),
            entity.getContextPolicyVersion(), entity.getToolSchemaVersion(), entity.getRevision(), entity.getQuality(),
            entity.getStatus(), entity.getModelName(), entity.getEstimatedInputTokens(), entity.getEstimatedOutputTokens(),
            instant(entity.getCreatedAt()), instant(entity.getUpdatedAt()), true);
    }

    private AdminAgentDtos.Draft toDraft(AgentDraftEntity entity) {
        return new AdminAgentDtos.Draft(id(entity.getId()), id(entity.getConversationId()), entity.getDraftType(),
            entity.getTitle(), entity.getStatus(), instant(entity.getCreatedAt()), instant(entity.getUpdatedAt()), true);
    }

    private AdminAgentDtos.Usage usageFor(AgentRunAuditEntity run, AdminScopeQuery query, AdminDataScope scope) {
        List<AgentRunAuditEventEntity> events = eventRepository.findEvents(run.getRunId(), query.allOwners(), query.ownerUserIds(), null);
        long input = events.stream().map(event -> number(parse(event.getPayloadJson()), "input_token_estimate"))
            .filter(value -> value != null).mapToLong(Long::longValue).max().orElse(0L);
        long output = events.stream().map(event -> number(parse(event.getPayloadJson()), "output_token_estimate"))
            .filter(value -> value != null).mapToLong(Long::longValue).max().orElse(0L);
        boolean estimated = input > 0 || output > 0;
        Long duration = duration(run.getStartedAt(), run.getCompletedAt());
        return new AdminAgentDtos.Usage(run.getRunId(), null, estimated ? input : null, estimated ? output : null,
            estimated ? input + output : null, duration, null,
            estimated ? AdminAgentDtos.TokenSource.ESTIMATED : AdminAgentDtos.TokenSource.UNAVAILABLE,
            estimated, scopeCompleteness(scope));
    }

    private boolean isContiguous(List<AdminAgentDtos.Event> events) {
        if (events.size() < 2) return true;
        for (int i = 1; i < events.size(); i++) {
            if (events.get(i).sequence() != events.get(i - 1).sequence() + 1) return false;
        }
        return true;
    }

    private JsonNode parse(String value) {
        try { return value == null ? objectMapper.createObjectNode() : objectMapper.readTree(value); }
        catch (Exception ignored) { return objectMapper.createObjectNode(); }
    }

    private String safe(JsonNode node, String field) { return redact(truncate(node.path(field).asText(null))); }

    private String redact(String value) {
        if (value == null || value.isBlank()) return value;
        return value.replaceAll("(?i)(api[_-]?key|token|secret|password|authorization|bearer)(\\s*[:=]\\s*)[^\\s,;]+", "$1$2***");
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_CONTENT_LENGTH ? value : value.substring(0, MAX_CONTENT_LENGTH) + "...";
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) { String value = node.path(field).asText(null); if (value != null && !value.isBlank()) return truncate(value); }
        return null;
    }

    private Long number(JsonNode node, String field) { return node.hasNonNull(field) && node.get(field).canConvertToLong() ? node.get(field).longValue() : null; }

    private String eventStatus(String type) {
        if (type == null) return null;
        String normalized = type.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("started") || normalized.equals("run_started")) return "STARTED";
        if (normalized.endsWith("progress")) return "PROGRESS";
        if (normalized.endsWith("completed") || normalized.equals("run_completed")) return "COMPLETED";
        if (normalized.endsWith("failed")) return "FAILED";
        if (normalized.endsWith("cancelled")) return "CANCELLED";
        return "PROGRESS";
    }

    private String normalizeRunId(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 64) throw new IllegalArgumentException("runId is invalid");
        return value.trim();
    }
    private long parseId(String value, String field) { try { long id = Long.parseLong(value); if (id <= 0) throw new NumberFormatException(); return id; } catch (Exception ex) { throw new IllegalArgumentException(field + " is invalid"); } }
    private String id(Long value) { return value == null ? null : value.toString(); }
    private Instant instant(Long value) { return value == null ? null : Instant.ofEpochMilli(value); }
    private Long duration(Long from, Long to) { return from == null || to == null ? null : Math.max(0L, Duration.ofMillis(to - from).toMillis()); }
    private String scopeCompleteness(AdminDataScope scope) { return scope.allOwners() || scope.storeIds().isEmpty() ? "COMPLETE" : "PARTIAL"; }
}

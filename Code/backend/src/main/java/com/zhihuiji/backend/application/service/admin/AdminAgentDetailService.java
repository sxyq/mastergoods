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
import com.zhihuiji.backend.application.service.v2.agent.context.ContextWindowResolver;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Detail-only Agent projections. Raw message/event payloads never leave this service. */
@Service
public class AdminAgentDetailService {
    private static final int MAX_CONTENT_LENGTH = 600;
    private static final int USAGE_BATCH_SIZE = 200;
    private static final int MAX_MODEL_ID_LENGTH = 128;
    private static final Duration DEFAULT_USAGE_WINDOW = Duration.ofDays(30);
    private static final Duration MAX_USAGE_WINDOW = Duration.ofDays(90);

    private final AdminAuthorizationService authorizationService;
    private final AdminAgentQueryRepository runRepository;
    private final AdminAgentDetailQueryRepository messageRepository;
    private final AdminAgentEventQueryRepository eventRepository;
    private final AdminAgentCheckpointQueryRepository checkpointRepository;
    private final AdminAgentDraftQueryRepository draftRepository;
    private final ObjectMapper objectMapper;
    private final ContextWindowResolver contextWindowResolver;

    @Autowired
    public AdminAgentDetailService(
        AdminAuthorizationService authorizationService,
        AdminAgentQueryRepository runRepository,
        AdminAgentDetailQueryRepository messageRepository,
        AdminAgentEventQueryRepository eventRepository,
        AdminAgentCheckpointQueryRepository checkpointRepository,
        AdminAgentDraftQueryRepository draftRepository,
        ObjectMapper objectMapper,
        ContextWindowResolver contextWindowResolver
    ) {
        this.authorizationService = authorizationService;
        this.runRepository = runRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
        this.checkpointRepository = checkpointRepository;
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
        this.contextWindowResolver = contextWindowResolver;
    }

    /** Compatibility constructor for isolated service tests and legacy callers. */
    public AdminAgentDetailService(
        AdminAuthorizationService authorizationService,
        AdminAgentQueryRepository runRepository,
        AdminAgentDetailQueryRepository messageRepository,
        AdminAgentEventQueryRepository eventRepository,
        AdminAgentCheckpointQueryRepository checkpointRepository,
        AdminAgentDraftQueryRepository draftRepository,
        ObjectMapper objectMapper
    ) {
        this(authorizationService, runRepository, messageRepository, eventRepository, checkpointRepository,
            draftRepository, objectMapper, new ContextWindowResolver(new AgentLlmProperties()));
    }

    @Transactional(readOnly = true)
    public AdminPageDtos.PageResponse<AdminAgentDtos.Message> messages(
        AdminPrincipal principal, String conversationId, boolean includeContent,
        Integer page, Integer size, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, includeContent);
        long id = parseId(conversationId, "conversationId");
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Page<AgentMessageEntity> result = query.allStores()
            ? messageRepository.findMessages(id, query.allOwners(), query.ownerUserIds(), PaginationUtils.pageable(page, size))
            : messageRepository.findMessagesScoped(id, query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(), PaginationUtils.pageable(page, size));
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
        List<AgentRunAuditEventEntity> events = query.allStores()
            ? eventRepository.findEvents(run.getRunId(), query.allOwners(), query.ownerUserIds(), afterSequence)
            : eventRepository.findEventsScoped(run.getRunId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(), afterSequence);
        boolean authorized = includeContent && scope.contentMode() == AdminDataScope.ContentMode.AUTHORIZED;
        List<AdminAgentDtos.Event> items = events.stream().map(event -> toEvent(event, authorized)).toList();
        return new AdminAgentDtos.EventPage(items, items.size(), isContiguous(items, afterSequence));
    }

    @Transactional(readOnly = true)
    public AdminAgentDtos.ContextResponse context(
        AdminPrincipal principal, String runId, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        AgentRunAuditEntity run = visibleRun(runId, scope);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        List<AgentContextCheckpointEntity> checkpoints = query.allStores()
            ? checkpointRepository.findCheckpoints(run.getConversationId(), query.allOwners(), query.ownerUserIds())
            : checkpointRepository.findCheckpointsScoped(run.getConversationId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds());
        List<AdminAgentDtos.ContextCheckpoint> items = checkpoints.stream().map(this::toCheckpoint).toList();
        Integer input = items.stream().map(AdminAgentDtos.ContextCheckpoint::estimatedInputTokens)
            .filter(value -> value != null).max(Integer::compareTo).orElse(null);
        Integer output = items.stream().map(AdminAgentDtos.ContextCheckpoint::estimatedOutputTokens)
            .filter(value -> value != null).max(Integer::compareTo).orElse(null);
        String modelName = items.stream()
            .map(AdminAgentDtos.ContextCheckpoint::modelName)
            .filter(value -> value != null && !value.isBlank())
            .reduce((first, second) -> second)
            .orElse(null);
        ContextWindowResolver.Resolution window = modelName == null
            ? contextWindowResolver.resolveForCurrentWithSource()
            : contextWindowResolver.resolveWithSource(null, modelName, null);
        AdminAgentDtos.ContextWindowSource source = AdminAgentDtos.ContextWindowSource.valueOf(window.source().name());
        return new AdminAgentDtos.ContextResponse(run.getRunId(), id(run.getConversationId()), window.tokens(),
            input, output, items, true, scopeCompleteness(scope), source);
    }

    @Transactional(readOnly = true)
    public List<AdminAgentDtos.Draft> drafts(
        AdminPrincipal principal, String runId, Long ownerUserId, Long storeId
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        AgentRunAuditEntity run = visibleRun(runId, scope);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        List<AgentDraftEntity> drafts = query.allStores()
            ? draftRepository.findDrafts(run.getConversationId(), query.allOwners(), query.ownerUserIds())
            : draftRepository.findDraftsScoped(run.getConversationId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds());
        return drafts.stream().map(this::toDraft).toList();
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
        Page<AgentRunAuditEntity> runs = query.allStores()
            ? runRepository.findRuns(query.allOwners(), query.ownerUserIds(), null, null, null,
                from == null ? null : from.toEpochMilli(), to == null ? null : to.toEpochMilli(), PaginationUtils.pageable(page, size))
            : runRepository.findRunsScoped(query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(),
                null, null, null, null, from == null ? null : from.toEpochMilli(),
                to == null ? null : to.toEpochMilli(), PaginationUtils.pageable(page, size));
        List<AdminAgentDtos.Usage> items = runs.getContent().stream().map(run -> usageFor(run, query, scope)).toList();
        return new AdminAgentDtos.UsagePage(items, runs.getTotalElements(), Instant.now());
    }

    /** API-ADM-08 aggregate usage by model and time bucket. */
    @Transactional(readOnly = true)
    public AdminAgentDtos.UsagePage usage(
        AdminPrincipal principal,
        Instant from,
        Instant to,
        String modelId,
        String granularity,
        Long ownerUserId,
        Long storeId,
        Integer page,
        Integer size
    ) {
        AdminDataScope scope = authorize(principal, ownerUserId, storeId, false);
        TimeRange range = usageTimeRange(from, to);
        String normalizedModelId = normalizeModelId(modelId);
        UsageGranularity usageGranularity = UsageGranularity.parse(granularity);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        Map<UsageBucket, UsageAccumulator> aggregates = new HashMap<>();

        int batchPage = 0;
        Page<AgentRunAuditEntity> runs;
        do {
            var pageable = PaginationUtils.pageable(batchPage, USAGE_BATCH_SIZE);
            runs = query.allStores()
                ? runRepository.findRuns(query.allOwners(), query.ownerUserIds(), null, null, null,
                    range.from().toEpochMilli(), range.to().toEpochMilli(), pageable)
                : runRepository.findRunsScoped(query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(),
                    null, null, null, null, range.from().toEpochMilli(), range.to().toEpochMilli(), pageable);

            List<AgentRunAuditEntity> batch = runs.getContent();
            List<String> runIds = batch.stream()
                .map(AgentRunAuditEntity::getRunId)
                .filter(Objects::nonNull)
                .toList();
            Map<String, List<AgentRunAuditEventEntity>> eventsByRun = runIds.isEmpty()
                ? Map.of()
                : eventRepository.findEventsForRuns(
                    runIds, query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds()
                ).stream().collect(Collectors.groupingBy(AgentRunAuditEventEntity::getRunId));

            for (AgentRunAuditEntity run : batch) {
                if (run.getStartedAt() == null) {
                    continue;
                }
                RunUsage sample = usageSample(run, eventsByRun.getOrDefault(run.getRunId(), List.of()));
                if (normalizedModelId != null && !normalizedModelId.equals(sample.modelId())) {
                    continue;
                }
                Instant startedAt = Instant.ofEpochMilli(run.getStartedAt());
                Instant bucketStart = usageGranularity.bucketStart(startedAt);
                UsageBucket bucket = new UsageBucket(sample.modelId(), bucketStart);
                aggregates.computeIfAbsent(bucket, ignored -> new UsageAccumulator()).add(sample);
            }
            batchPage++;
        } while (runs.hasNext());

        List<AdminAgentDtos.Usage> allItems = aggregates.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator
                .comparing(UsageBucket::bucketStart)
                .thenComparing(UsageBucket::modelId, Comparator.nullsFirst(String::compareTo))))
            .map(entry -> entry.getValue().toDto(
                entry.getKey(), usageGranularity, scopeCompleteness(scope)
            ))
            .toList();
        var aggregatePage = PaginationUtils.pageable(page, size);
        List<AdminAgentDtos.Usage> items = PaginationUtils.slice(
            allItems, aggregatePage.getPageNumber(), aggregatePage.getPageSize()
        );
        return new AdminAgentDtos.UsagePage(
            items,
            allItems.size(),
            Instant.now(),
            range.from(),
            range.to(),
            usageGranularity.name(),
            AdminScopeDtos.Scope.from(scope),
            scopeCompleteness(scope)
        );
    }

    /** Parameter-order compatibility for callers that keep owner/store beside the other scope filters. */
    @Transactional(readOnly = true)
    public AdminAgentDtos.UsagePage usage(
        AdminPrincipal principal,
        Instant from,
        Instant to,
        Long ownerUserId,
        Long storeId,
        String modelId,
        String granularity,
        Integer page,
        Integer size
    ) {
        return usage(principal, from, to, modelId, granularity, ownerUserId, storeId, page, size);
    }

    private TimeRange usageTimeRange(Instant from, Instant to) {
        Instant normalizedTo = to == null ? Instant.now() : to;
        Instant normalizedFrom = from == null ? normalizedTo.minus(DEFAULT_USAGE_WINDOW) : from;
        if (!normalizedFrom.isBefore(normalizedTo)) {
            throw new IllegalArgumentException("from must be before to");
        }
        if (Duration.between(normalizedFrom, normalizedTo).compareTo(MAX_USAGE_WINDOW) > 0) {
            throw new IllegalArgumentException("time range is too wide");
        }
        return new TimeRange(normalizedFrom, normalizedTo);
    }

    private String normalizeModelId(String modelId) {
        if (modelId == null) {
            return null;
        }
        String normalized = modelId.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_MODEL_ID_LENGTH) {
            throw new IllegalArgumentException("modelId is too long");
        }
        return normalized;
    }

    private AdminDataScope authorize(AdminPrincipal principal, Long ownerUserId, Long storeId, boolean includeContent) {
        AdminDataScope scope = authorizationService.authorize(principal, AdminPermission.AGENT_RUN_READ, ownerUserId, storeId);
        if (includeContent) {
            authorizationService.requirePermission(principal, AdminPermission.AGENT_CONTENT_READ);
        }
        return scope;
    }

    private AgentRunAuditEntity visibleRun(String runId, AdminDataScope scope) {
        String normalized = normalizeRunId(runId);
        AdminScopeQuery query = AdminScopeQuery.from(scope);
        return (query.allStores()
            ? runRepository.findRun(normalized, query.allOwners(), query.ownerUserIds())
            : runRepository.findRunScoped(normalized, query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds()))
            .orElseThrow(() -> new AccessDeniedException("administrator resource not visible"));
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
            entity.getTitle(), entity.getStatus(), instant(entity.getCreatedAt()), instant(entity.getUpdatedAt()), true,
            id(entity.getConfirmedBy()), instant(entity.getConfirmedAt()), entity.getBusinessReference(),
            safeFailure(entity.getFailureReason()));
    }

    private AdminAgentDtos.Usage usageFor(AgentRunAuditEntity run, AdminScopeQuery query, AdminDataScope scope) {
        List<AgentRunAuditEventEntity> events = query.allStores()
            ? eventRepository.findEvents(run.getRunId(), query.allOwners(), query.ownerUserIds(), null)
            : eventRepository.findEventsScoped(run.getRunId(), query.allOwners(), query.ownerUserIds(), query.allStores(), query.storeIds(), null);
        RunUsage sample = usageSample(run, events);
        return new AdminAgentDtos.Usage(run.getRunId(), sample.modelId(), sample.inputTokens(), sample.outputTokens(),
            sample.totalTokens(), sample.durationMs(), sample.timeToFirstTokenMs(), sample.tokenSource(),
            sample.tokenSource() == AdminAgentDtos.TokenSource.ESTIMATED, scopeCompleteness(scope));
    }

    private RunUsage usageSample(AgentRunAuditEntity run, List<AgentRunAuditEventEntity> events) {
        List<AgentRunAuditEventEntity> orderedEvents = events == null ? List.of() : events.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(AgentRunAuditEventEntity::getSeq, Comparator.nullsLast(Integer::compareTo)))
            .toList();
        TokenValues tokens = tokenValues(orderedEvents);
        return new RunUsage(
            modelId(orderedEvents),
            tokens.inputTokens(),
            tokens.outputTokens(),
            tokens.totalTokens(),
            duration(run.getStartedAt(), run.getCompletedAt()),
            timeToFirstToken(run, orderedEvents),
            tokens.source()
        );
    }

    private TokenValues tokenValues(List<AgentRunAuditEventEntity> events) {
        Long exactInput = null;
        Long exactOutput = null;
        Long exactTotal = null;
        Long estimatedInput = null;
        Long estimatedOutput = null;
        boolean exactObserved = false;
        boolean estimatedObserved = false;
        for (AgentRunAuditEventEntity event : events) {
            JsonNode root = parse(event.getPayloadJson());
            JsonNode usage = object(root, "usage", "token_usage", "usage_metadata");
            Long input = nonNegative(firstNumber(usage, root, "input_tokens", "prompt_tokens", "inputTokenCount", "promptTokenCount"));
            Long output = nonNegative(firstNumber(usage, root, "output_tokens", "completion_tokens", "outputTokenCount", "completionTokenCount"));
            Long total = nonNegative(firstNumber(usage, root, "total_tokens", "totalTokenCount"));
            if (input != null || output != null || total != null) {
                exactObserved = true;
                exactInput = add(exactInput, input);
                exactOutput = add(exactOutput, output);
                exactTotal = add(exactTotal, total);
            }

            Long inputEstimate = nonNegative(firstNumber(root, null,
                "input_token_estimate", "estimated_input_tokens", "input_tokens_estimate", "inputTokenEstimate"));
            Long outputEstimate = nonNegative(firstNumber(root, null,
                "output_token_estimate", "estimated_output_tokens", "output_tokens_estimate", "outputTokenEstimate"));
            if (inputEstimate != null || outputEstimate != null) {
                estimatedObserved = true;
                estimatedInput = max(estimatedInput, inputEstimate);
                estimatedOutput = max(estimatedOutput, outputEstimate);
            }
        }

        Long input = exactInput != null ? exactInput : estimatedInput;
        Long output = exactOutput != null ? exactOutput : estimatedOutput;
        Long total = exactTotal;
        if (total == null && input != null && output != null) {
            total = add(input, output);
        }
        boolean estimateUsed = estimatedObserved && (exactInput == null || exactOutput == null || exactTotal == null);
        AdminAgentDtos.TokenSource source = exactObserved && !estimateUsed
            ? AdminAgentDtos.TokenSource.EXACT
            : (exactObserved || estimatedObserved ? AdminAgentDtos.TokenSource.ESTIMATED : AdminAgentDtos.TokenSource.UNAVAILABLE);
        return new TokenValues(input, output, total, source);
    }

    private String modelId(List<AgentRunAuditEventEntity> events) {
        String result = null;
        for (AgentRunAuditEventEntity event : events) {
            JsonNode root = parse(event.getPayloadJson());
            String candidate = firstText(root, "model_id", "modelId", "model_name", "modelName", "model");
            if (candidate != null) {
                result = candidate;
            }
        }
        return result;
    }

    private Long timeToFirstToken(AgentRunAuditEntity run, List<AgentRunAuditEventEntity> events) {
        for (AgentRunAuditEventEntity event : events) {
            JsonNode root = parse(event.getPayloadJson());
            Long direct = firstNumber(root, null,
                "time_to_first_token_ms", "timeToFirstTokenMs", "ttft_ms", "first_token_latency_ms", "firstTokenLatencyMs");
            if (direct != null && direct >= 0) {
                return direct;
            }
            Long firstTokenAt = firstNumber(root, null, "first_token_at", "firstTokenAt", "first_token_timestamp");
            if (firstTokenAt != null && run.getStartedAt() != null) {
                return Math.max(0L, firstTokenAt - run.getStartedAt());
            }
            if (isAnswerDelta(event.getEventType()) && event.getCreatedAt() != null && run.getStartedAt() != null) {
                return Math.max(0L, event.getCreatedAt() - run.getStartedAt());
            }
        }
        return null;
    }

    private boolean isAnswerDelta(String eventType) {
        return eventType != null && (eventType.equalsIgnoreCase("answer_delta")
            || eventType.equalsIgnoreCase("answer.delta"));
    }

    private JsonNode object(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode child = root.get(field);
            if (child != null && child.isObject()) {
                return child;
            }
        }
        return root;
    }

    private Long firstNumber(JsonNode primary, JsonNode secondary, String... fields) {
        for (String field : fields) {
            Long value = number(primary, field);
            if (value != null) {
                return value;
            }
            if (secondary != null) {
                value = number(secondary, field);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String firstText(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isValueNode()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private Long max(Long current, Long candidate) {
        if (candidate == null || candidate < 0) {
            return current;
        }
        return current == null ? candidate : Math.max(current, candidate);
    }

    private Long nonNegative(Long value) {
        return value == null || value < 0 ? null : value;
    }

    private Long add(Long current, Long candidate) {
        if (candidate == null || candidate < 0) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return candidate > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + candidate;
    }

    private record TimeRange(Instant from, Instant to) {}

    private enum UsageGranularity {
        HOUR,
        DAY,
        WEEK;

        private static UsageGranularity parse(String value) {
            if (value == null || value.isBlank()) {
                return DAY;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("granularity must be HOUR, DAY or WEEK");
            }
        }

        private Instant bucketStart(Instant value) {
            return switch (this) {
                case HOUR -> value.truncatedTo(ChronoUnit.HOURS);
                case DAY -> value.truncatedTo(ChronoUnit.DAYS);
                case WEEK -> {
                    LocalDate date = value.atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1L);
                    yield monday.atStartOfDay(ZoneOffset.UTC).toInstant();
                }
            };
        }

        private Instant bucketEnd(Instant start) {
            return switch (this) {
                case HOUR -> start.plus(1, ChronoUnit.HOURS);
                case DAY -> start.plus(1, ChronoUnit.DAYS);
                case WEEK -> start.plus(7, ChronoUnit.DAYS);
            };
        }
    }

    private record UsageBucket(String modelId, Instant bucketStart) {}

    private record TokenValues(
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        AdminAgentDtos.TokenSource source
    ) {}

    private record RunUsage(
        String modelId,
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        Long durationMs,
        Long timeToFirstTokenMs,
        AdminAgentDtos.TokenSource tokenSource
    ) {}

    private final class UsageAccumulator {
        private long requestCount;
        private long inputTokens;
        private long outputTokens;
        private long totalTokens;
        private boolean hasInputTokens;
        private boolean hasOutputTokens;
        private boolean hasTotalTokens;
        private boolean exactTokens;
        private boolean estimatedTokens;
        private final List<Long> durations = new ArrayList<>();
        private final List<Long> timeToFirstTokens = new ArrayList<>();

        private void add(RunUsage sample) {
            requestCount++;
            if (sample.inputTokens() != null) {
                inputTokens = saturatingAdd(inputTokens, sample.inputTokens());
                hasInputTokens = true;
            }
            if (sample.outputTokens() != null) {
                outputTokens = saturatingAdd(outputTokens, sample.outputTokens());
                hasOutputTokens = true;
            }
            if (sample.totalTokens() != null) {
                totalTokens = saturatingAdd(totalTokens, sample.totalTokens());
                hasTotalTokens = true;
            }
            if (sample.durationMs() != null && sample.durationMs() >= 0) {
                durations.add(sample.durationMs());
            }
            if (sample.timeToFirstTokenMs() != null && sample.timeToFirstTokenMs() >= 0) {
                timeToFirstTokens.add(sample.timeToFirstTokenMs());
            }
            if (sample.tokenSource() == AdminAgentDtos.TokenSource.EXACT) {
                exactTokens = true;
            } else if (sample.tokenSource() == AdminAgentDtos.TokenSource.ESTIMATED) {
                estimatedTokens = true;
            }
        }

        private AdminAgentDtos.Usage toDto(
            UsageBucket bucket,
            UsageGranularity granularity,
            String scopeCompleteness
        ) {
            Long averageDuration = average(durations);
            Long p95Duration = percentile95(durations);
            Long averageTimeToFirstToken = average(timeToFirstTokens);
            Long p95TimeToFirstToken = percentile95(timeToFirstTokens);
            Long input = hasInputTokens ? inputTokens : null;
            Long output = hasOutputTokens ? outputTokens : null;
            Long total = hasTotalTokens ? totalTokens
                : (input != null && output != null ? Long.valueOf(saturatingAdd(input, output)) : null);
            AdminAgentDtos.TokenSource source = exactTokens && !estimatedTokens
                ? AdminAgentDtos.TokenSource.EXACT
                : (exactTokens || estimatedTokens ? AdminAgentDtos.TokenSource.ESTIMATED : AdminAgentDtos.TokenSource.UNAVAILABLE);
            return new AdminAgentDtos.Usage(
                null,
                bucket.modelId(),
                bucket.bucketStart(),
                granularity.bucketEnd(bucket.bucketStart()),
                requestCount,
                input,
                output,
                total,
                averageDuration,
                averageTimeToFirstToken,
                averageDuration,
                p95Duration,
                averageTimeToFirstToken,
                p95TimeToFirstToken,
                source,
                source == AdminAgentDtos.TokenSource.ESTIMATED,
                scopeCompleteness
            );
        }

        private Long average(List<Long> values) {
            if (values.isEmpty()) {
                return null;
            }
            double sum = values.stream().mapToDouble(Long::doubleValue).sum();
            return Math.round(sum / values.size());
        }

        private Long percentile95(List<Long> values) {
            if (values.isEmpty()) {
                return null;
            }
            List<Long> sorted = values.stream().sorted().toList();
            int rank = Math.max(1, (int) Math.ceil(sorted.size() * 0.95d));
            return sorted.get(rank - 1);
        }

        private long saturatingAdd(long left, long right) {
            return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
        }
    }

    private boolean isContiguous(List<AdminAgentDtos.Event> events, Integer afterSequence) {
        if (events.isEmpty()) return true;
        long expected = afterSequence == null ? 1L : Math.max(0L, afterSequence.longValue()) + 1L;
        java.util.Set<String> eventIds = new java.util.HashSet<>();
        java.util.Set<Long> sequences = new java.util.HashSet<>();
        for (AdminAgentDtos.Event event : events) {
            if (event == null || event.sequence() != expected
                || !sequences.add(event.sequence())
                || (event.eventId() != null && !eventIds.add(event.eventId()))) {
                return false;
            }
            expected++;
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

    private String safeFailure(String value) {
        if (value == null || value.isBlank()) return null;
        return truncate(redact(value.replaceAll("[\\r\\n\\t]+", " ").trim()));
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_CONTENT_LENGTH ? value : value.substring(0, MAX_CONTENT_LENGTH) + "...";
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) { String value = node.path(field).asText(null); if (value != null && !value.isBlank()) return truncate(value); }
        return null;
    }

    private Long number(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value.canConvertToLong()) {
            return value.longValue();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

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
    private Long duration(Long from, Long to) {
        if (from == null || to == null) {
            return null;
        }
        if (to <= from) {
            return 0L;
        }
        try {
            return Math.subtractExact(to, from);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
    private String scopeCompleteness(AdminDataScope scope) { return scope.allOwners() ? "COMPLETE" : "PARTIAL"; }
}

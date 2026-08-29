package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

/** Size-bounded projections for administrator Agent observability. */
public final class AdminAgentDtos {
    private AdminAgentDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RunSummary(
        String runId,
        String conversationId,
        String actorUserId,
        String ownerUserId,
        String storeId,
        String terminalStatus,
        String modelId,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        Long timeToFirstTokenMs,
        Integer iterationCount,
        Integer toolCallCount,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        TokenSource tokenSource,
        boolean contentRedacted,
        String scopeCompleteness
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Event(
        String eventId,
        String runId,
        long sequence,
        String eventType,
        String toolName,
        String callId,
        Instant occurredAt,
        String status,
        Long durationMs,
        String argumentSummary,
        String resultSummary,
        RedactionState redactionState
    ) {}

    public enum TokenSource { EXACT, ESTIMATED, UNAVAILABLE }

    public enum RedactionState { FULL_ALLOWED, PARTIAL, REDACTED }

    public enum ContextWindowSource { CONFIGURED_OVERRIDE, KNOWN_MODEL, CONSERVATIVE_FALLBACK }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventPage(List<Event> items, long total, boolean eventIntegrity) {
        public EventPage {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Message(
        String messageId,
        String conversationId,
        String runId,
        String role,
        String messageType,
        String content,
        String redactionState,
        Instant occurredAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ContextCheckpoint(
        String checkpointId,
        String conversationId,
        String sourceBoundaryMessageId,
        Integer sourceMessageCount,
        Integer summaryVersion,
        Integer contextPolicyVersion,
        Integer toolSchemaVersion,
        Integer revision,
        String quality,
        String status,
        String modelName,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        Instant createdAt,
        Instant updatedAt,
        boolean contentRedacted
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ContextResponse(
        String runId,
        String conversationId,
        Integer contextWindowTokens,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        List<ContextCheckpoint> checkpoints,
        boolean contentRedacted,
        String scopeCompleteness,
        ContextWindowSource contextWindowSource
    ) {
        public ContextResponse(
            String runId,
            String conversationId,
            Integer contextWindowTokens,
            Integer estimatedInputTokens,
            Integer estimatedOutputTokens,
            List<ContextCheckpoint> checkpoints,
            boolean contentRedacted,
            String scopeCompleteness
        ) {
            this(runId, conversationId, contextWindowTokens, estimatedInputTokens, estimatedOutputTokens,
                checkpoints, contentRedacted, scopeCompleteness, null);
        }

        public ContextResponse {
            checkpoints = List.copyOf(checkpoints == null ? List.of() : checkpoints);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Draft(
        String draftId,
        String conversationId,
        String draftType,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt,
        boolean contentRedacted,
        String confirmedBy,
        Instant confirmedAt,
        String businessReference,
        String failureReason
    ) {
        /** Compatibility constructor for callers that only have the persisted draft fields. */
        public Draft(
            String draftId,
            String conversationId,
            String draftType,
            String title,
            String status,
            Instant createdAt,
            Instant updatedAt,
            boolean contentRedacted
        ) {
            this(draftId, conversationId, draftType, title, status, createdAt, updatedAt, contentRedacted,
                null, null, null, null);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Usage(
        String runId,
        String modelId,
        Instant bucketStart,
        Instant bucketEnd,
        Long requestCount,
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        Long durationMs,
        Long timeToFirstTokenMs,
        Long averageDurationMs,
        Long p95DurationMs,
        Long averageTimeToFirstTokenMs,
        Long p95TimeToFirstTokenMs,
        TokenSource tokenSource,
        boolean estimated,
        String scopeCompleteness
    ) {
        /** Compatibility constructor for the per-run usage shape used by existing callers. */
        public Usage(
            String runId,
            String modelId,
            Long inputTokens,
            Long outputTokens,
            Long totalTokens,
            Long durationMs,
            Long timeToFirstTokenMs,
            TokenSource tokenSource,
            boolean estimated,
            String scopeCompleteness
        ) {
            this(
                runId,
                modelId,
                null,
                null,
                runId == null ? null : 1L,
                inputTokens,
                outputTokens,
                totalTokens,
                durationMs,
                timeToFirstTokenMs,
                durationMs,
                durationMs,
                timeToFirstTokenMs,
                timeToFirstTokenMs,
                tokenSource,
                estimated,
                scopeCompleteness
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UsagePage(
        List<Usage> items,
        long total,
        Instant generatedAt,
        Instant from,
        Instant to,
        String granularity,
        AdminScopeDtos.Scope scope,
        String scopeCompleteness
    ) {
        public UsagePage(List<Usage> items, long total, Instant generatedAt) {
            this(items, total, generatedAt, null, null, null, null, null);
        }

        public UsagePage {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }
}

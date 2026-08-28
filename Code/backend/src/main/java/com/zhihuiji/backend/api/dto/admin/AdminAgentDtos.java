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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventPage(List<Event> items, long total, boolean eventIntegrity) {
        public EventPage {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }
}

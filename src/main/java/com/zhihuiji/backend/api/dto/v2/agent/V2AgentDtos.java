package com.zhihuiji.backend.api.dto.v2.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class V2AgentDtos {
    private V2AgentDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationResponse(
        Long id,
        String title,
        String status,
        String latestSummary,
        Long createdAt,
        Long updatedAt,
        Long lastMessageAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationCreateRequest(
        @NotBlank String title,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentConversationUpdateRequest(
        String title,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentMessageResponse(
        Long id,
        Long conversationId,
        String role,
        String messageType,
        String content,
        String structuredDataJson,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentMessageCreateRequest(
        @NotBlank String role,
        @NotBlank String messageType,
        @NotBlank String content,
        String structuredDataJson
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftResponse(
        Long id,
        Long conversationId,
        String draftType,
        String title,
        String contentJson,
        String status,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftCreateRequest(
        Long conversationId,
        @NotBlank String draftType,
        @NotBlank String title,
        @NotBlank String contentJson,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentDraftUpdateRequest(
        Long conversationId,
        @NotBlank String draftType,
        @NotBlank String title,
        @NotBlank String contentJson,
        String status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentWorkbenchResponse(
        String greeting,
        List<KpiCardItem> kpiCards,
        List<String> quickQuestions,
        List<RecentConversationItem> recentConversations,
        List<PendingDraftItem> pendingDrafts,
        List<RiskAlertItem> riskAlerts,
        String todaySummary
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KpiCardItem(
        String label,
        String value,
        String trendDirection,
        String trendValue,
        String route
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecentConversationItem(
        Long id,
        String title,
        Long lastMessageAt,
        Integer messageCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PendingDraftItem(
        Long id,
        String draftType,
        String title,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RiskAlertItem(
        String level,
        String title,
        String description
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentChatRequest(
        Long conversationId,
        @NotBlank String message,
        Boolean stream
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentRunCancelResponse(
        String runId,
        String status,
        Boolean cancelled
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentRunAuditResponse(
        String runId,
        Long ownerUserId,
        Long conversationId,
        String status,
        String mode,
        String llmStatus,
        String planSource,
        Integer toolCount,
        Integer eventCount,
        String auditId,
        String traceId,
        String errorCode,
        String errorMessage,
        Long startedAt,
        Long completedAt,
        Long updatedAt,
        List<AgentRunAuditEventResponse> events
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentRunAuditEventResponse(
        String eventId,
        Integer seq,
        String eventType,
        JsonNode payload,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ResultBlockDto(
        String blockType,
        String title,
        JsonNode data
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentChatResponse(
        String runId,
        Long conversationId,
        String answer,
        List<ResultBlockDto> blocks,
        Long draftId,
        Boolean safetyPassed,
        String safetyReason,
        String mode,
        String llmStatus,
        String planSource,
        String planSummary,
        List<AgentToolCallDto> toolCalls,
        List<AgentEvidenceRefDto> evidenceRefs,
        List<ResultBlockDto> resultBlocks,
        AgentPerformanceSummaryDto performanceSummary,
        String auditId,
        String traceId,
        AgentObservabilityDto observability
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentToolCallDto(
        String toolCallId,
        String toolName,
        String status,
        String inputSummary,
        JsonNode queryWindow,
        Integer returnedCount,
        Integer totalCount,
        Integer limit,
        Boolean isTruncated,
        Long durationMs,
        String resultSummary,
        String errorCode,
        String errorMessage
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentEvidenceRefDto(
        String evidenceId,
        String toolCallId,
        String toolName,
        String label,
        String value,
        JsonNode queryWindow,
        Boolean isTruncated
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentPerformanceSummaryDto(
        Long startedAt,
        Long completedAt,
        Long durationMs,
        Long toolDurationMs,
        Long modelDurationMs
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentObservabilityDto(
        String requestId,
        String correlationId,
        String traceId,
        String auditId,
        String logRef
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentTaskResponse(
        Long id,
        String taskType,
        String title,
        String triggerSource,
        String status,
        String statusLabel,
        Integer progress,
        String inputText,
        String resultJson,
        Long createdAt,
        Long updatedAt,
        Long completedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentNotificationResponse(
        Long id,
        Long taskId,
        String title,
        String body,
        String level,
        Boolean isRead,
        Boolean isDelivered,
        Long createdAt
    ) {}
}

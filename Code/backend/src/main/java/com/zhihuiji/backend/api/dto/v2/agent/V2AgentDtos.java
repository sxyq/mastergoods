package com.zhihuiji.backend.api.dto.v2.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        String runId,
        String role,
        String messageType,
        String content,
        String structuredDataJson,
        Long createdAt
    ) {
        /**
         * Backward-compatible constructor for callers that create manually
         * authored messages outside an Agent run.
         */
        public AgentMessageResponse(
            Long id,
            Long conversationId,
            String role,
            String messageType,
            String content,
            String structuredDataJson,
            Long createdAt
        ) {
            this(id, conversationId, null, role, messageType, content, structuredDataJson, createdAt);
        }
    }

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
        Long updatedAt,
        AgentImageGenerateResponse imageResult
    ) {
        public AgentDraftResponse(
            Long id,
            Long conversationId,
            String draftType,
            String title,
            String contentJson,
            String status,
            Long createdAt,
            Long updatedAt
        ) {
            this(id, conversationId, draftType, title, contentJson, status, createdAt, updatedAt, null);
        }
    }

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
        String todaySummary,
        String status,
        String dataPolicy,
        List<WorkbenchCapabilityItem> capabilities,
        List<String> warnings
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WorkbenchCapabilityItem(
        String id,
        String title,
        String description
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
        Integer messageCount,
        String latestSummary
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
        Boolean stream,
        @Size(max = 9) List<Long> imageAssetIds
    ) {
        public AgentChatRequest(Long conversationId, String message, Boolean stream) {
            this(conversationId, message, stream, List.of());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentImageGenerateRequest(
        @NotBlank String prompt,
        List<Long> referenceAssetIds
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentImageGenerateResponse(
        String imageUrl,
        String revisedPrompt
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
        Integer auditWriteDroppedCount,
        Integer auditWriteFailedCount,
        Boolean auditLossy,
        Integer emittedEventCount,
        List<String> warnings,
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
    public record AgentRunTraceResponse(
        String runId,
        Long conversationId,
        String status,
        String mode,
        String llmStatus,
        String planSource,
        Integer toolCount,
        Integer eventCount,
        Integer auditWriteDroppedCount,
        Integer auditWriteFailedCount,
        Boolean auditLossy,
        Integer emittedEventCount,
        List<String> warnings,
        String auditId,
        String traceId,
        String errorCode,
        String errorMessage,
        Long startedAt,
        Long completedAt,
        Long updatedAt,
        List<AgentTraceEventResponse> events
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentTraceEventResponse(
        String eventId,
        Integer seq,
        Integer toolSequence,
        String eventType,
        String toolCallId,
        String toolName,
        String toolLabel,
        String content,
        String deltaSource,
        String inputSummary,
        JsonNode queryWindow,
        String resultSummary,
        Integer returnedCount,
        Integer totalCount,
        Integer limit,
        Boolean isTruncated,
        JsonNode evidence,
        String safeMessage,
        Long draftId,
        String draftType,
        String title,
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
        AgentObservabilityDto observability,
        String terminalStatus,
        String errorCode,
        String safeMessage,
        List<String> completedTools,
        List<String> missingTargetTools
    ) {
        /**
         * 兼容旧调用方的构造器：终态缺省按 COMPLETED 处理仅限旧路径；
         * 新代码必须显式传入 terminalStatus。
         */
        public AgentChatResponse(
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
        ) {
            this(runId, conversationId, answer, blocks, draftId, safetyPassed, safetyReason,
                mode, llmStatus, planSource, planSummary, toolCalls, evidenceRefs, resultBlocks,
                performanceSummary, auditId, traceId, observability,
                "COMPLETED", null, null, List.of(), List.of());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AgentToolCallDto(
        Integer sequence,
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

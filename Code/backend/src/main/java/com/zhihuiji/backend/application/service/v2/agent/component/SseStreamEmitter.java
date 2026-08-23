package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent SSE 事件发送组件。
 *
 * <p>承担原 {@code V2AgentAiService} 中所有 {@code emit*} 方法与 {@code sendEvent}：
 * 构造事件 payload、序列化为 JSON、通过 {@link SseEmitter} 推送，并把事件交给
 * {@link RunAuditService} 做活跃检测、序号 enrich 与审计持久化。
 *
 * <p>单向依赖 {@link RunAuditService}（运行生命周期与审计），避免循环依赖。
 * {@code emitPlan} 因耦合主类的 {@code AgentToolPlan} 私有记录，保留在主类中，
 * 通过调用本组件的 {@link #sendEvent} 完成发送。
 */
@Component
public class SseStreamEmitter {
    private static final int ANSWER_DELTA_BATCH_CHAR_THRESHOLD = 24;

    private final ObjectMapper objectMapper;
    private final RunAuditService runAuditService;

    public SseStreamEmitter(ObjectMapper objectMapper, RunAuditService runAuditService) {
        this.objectMapper = objectMapper;
        this.runAuditService = runAuditService;
    }

    public void emitBlocks(SseEmitter emitter, String runId, List<V2AgentDtos.ResultBlockDto> blocks) {
        if (emitter == null || runId == null) {
            return;
        }
        for (V2AgentDtos.ResultBlockDto block : blocks) {
            try {
                sendEvent(emitter, eventMap("result_block", mapOf(
                    "run_id", runId,
                    "block", block,
                    "timestamp", System.currentTimeMillis()
                )));
            } catch (IOException ex) {
                throw new IllegalStateException("发送 result_block 失败", ex);
            }
        }
    }

    public void emitToolStarted(SseEmitter emitter, String runId, String toolName, Map<String, Object> toolInput) {
        emitToolStarted(emitter, runId, toolName, toolInput, 0);
    }

    public void emitToolStarted(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput,
        int sequence
    ) {
        emitToolStarted(emitter, runId, toolName, toolInput, sequence, null);
    }

    public void emitToolStarted(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput,
        int sequence,
        String modelToolCallId
    ) {
        if (runId == null) {
            return;
        }
        try {
            sendEvent(emitter, eventMap("tool_started", mapOf(
                "run_id", runId,
                "tool_call_id", StringUtils.hasText(modelToolCallId)
                    ? modelToolCallId
                    : callId(runId, toolName, sequence),
                "tool_sequence", positiveSequence(sequence),
                "tool_name", toolName,
                "selection_origin", StringUtils.hasText(modelToolCallId) ? "model_tool_call" : "unknown",
                "input_summary", toolInputSummary(toolName, toolInput),
                "query_window", queryWindowFor(toolInput),
                "tool_input", toolInput,
                "started_at", System.currentTimeMillis(),
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_started 失败", ex);
        }
    }

    public ToolAudit startToolAudit(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput
    ) {
        return startToolAudit(emitter, runId, toolName, toolInput, 0);
    }

    public ToolAudit startToolAudit(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput,
        int sequence
    ) {
        return startToolAudit(emitter, runId, toolName, toolInput, sequence, null);
    }

    public ToolAudit startToolAudit(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput,
        int sequence,
        String modelToolCallId
    ) {
        ToolAudit audit = new ToolAudit(toolName, toolInput, System.currentTimeMillis(), sequence, modelToolCallId);
        emitToolStarted(emitter, runId, toolName, toolInput, sequence, modelToolCallId);
        return audit;
    }

    public void emitToolCompleted(SseEmitter emitter, String runId, String toolName, String resultSummary) {
        emitToolCompleted(emitter, runId, toolName, resultSummary, null);
    }

    public void emitToolCompleted(
        SseEmitter emitter,
        String runId,
        String toolName,
        String resultSummary,
        ToolAudit audit
    ) {
        if (runId == null) {
            return;
        }
        try {
            int sequence = audit == null ? 0 : audit.sequence();
            Map<String, Object> payload = mapOf(
                "run_id", runId,
                "tool_call_id", callId(runId, toolName, sequence),
                "tool_sequence", positiveSequence(sequence),
                "tool_name", toolName,
                "result_summary", resultSummary,
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            );
            if (audit != null) {
                payload.putAll(audit.eventFields());
            }
            sendEvent(emitter, eventMap("tool_completed", payload));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_completed 失败", ex);
        }
    }

    public void emitToolFailed(SseEmitter emitter, String runId, String toolName, String safeMessage, long durationMs) {
        emitToolFailed(emitter, runId, toolName, safeMessage, durationMs, System.currentTimeMillis(), Map.of());
    }

    public void emitToolFailed(
        SseEmitter emitter,
        String runId,
        String toolName,
        String safeMessage,
        long durationMs,
        long startedAt,
        Map<String, Object> toolInput
    ) {
        emitToolFailed(emitter, runId, toolName, safeMessage, durationMs, startedAt, toolInput, 0);
    }

    public void emitToolFailed(
        SseEmitter emitter,
        String runId,
        String toolName,
        String safeMessage,
        long durationMs,
        long startedAt,
        Map<String, Object> toolInput,
        int sequence
    ) {
        emitToolFailed(emitter, runId, toolName, safeMessage, durationMs, startedAt, toolInput, sequence, null);
    }

    public void emitToolFailed(
        SseEmitter emitter,
        String runId,
        String toolName,
        String safeMessage,
        long durationMs,
        long startedAt,
        Map<String, Object> toolInput,
        int sequence,
        String modelToolCallId
    ) {
        if (runId == null) {
            return;
        }
        long completedAt = System.currentTimeMillis();
        try {
            sendEvent(emitter, eventMap("tool_failed", mapOf(
                "run_id", runId,
                "tool_call_id", StringUtils.hasText(modelToolCallId)
                    ? modelToolCallId
                    : callId(runId, toolName, sequence),
                "tool_sequence", positiveSequence(sequence),
                "tool_name", toolName,
                "selection_origin", StringUtils.hasText(modelToolCallId) ? "model_tool_call" : "unknown",
                "input_summary", toolInputSummary(toolName, toolInput),
                "query_window", queryWindowFor(toolInput),
                "error_code", "TOOL_QUERY_FAILED",
                "safe_message", safeMessage,
                "error_summary", safeMessage,
                "duration_ms", Math.max(0L, durationMs),
                "started_at", startedAt,
                "completed_at", completedAt,
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "timestamp", completedAt
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_failed 失败", ex);
        }
    }

    public void emitToolSkipped(
        SseEmitter emitter,
        String runId,
        String toolName,
        String reason,
        Map<String, Object> toolInput,
        int sequence,
        String modelToolCallId
    ) {
        if (runId == null) {
            return;
        }
        try {
            sendEvent(emitter, eventMap("tool_skipped", mapOf(
                "run_id", runId,
                "tool_call_id", StringUtils.hasText(modelToolCallId)
                    ? modelToolCallId
                    : callId(runId, toolName, sequence),
                "tool_sequence", positiveSequence(sequence),
                "tool_name", toolName,
                "selection_origin", StringUtils.hasText(modelToolCallId) ? "model_tool_call" : "unknown",
                "skip_reason", reason,
                "input_summary", toolInputSummary(toolName, toolInput),
                "tool_input", toolInput == null ? Map.of() : toolInput,
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_skipped 失败", ex);
        }
    }

    public void emitAnswerCompleted(
        SseEmitter emitter,
        String runId,
        String answer,
        String mode,
        String llmStatus,
        String planSource
    ) throws IOException {
        if (!StringUtils.hasText(answer)) {
            return;
        }
        String auditId = RunAuditService.auditIdFor(runId);
        String traceId = RunAuditService.traceIdFor(runId);
        sendEvent(emitter, eventMap("answer_completed", mapOf(
            "run_id", runId,
            "answer", answer,
            "mode", mode,
            "llm_status", llmStatus,
            "plan_source", planSource,
            "audit_id", auditId,
            "trace_id", traceId,
            "observability", observabilityFor(runId, auditId, traceId),
            "timestamp", System.currentTimeMillis()
        )));
    }

    public void emitAnswerDeltaUnchecked(
        SseEmitter emitter,
        String runId,
        String delta,
        String deltaSource
    ) {
        if (!StringUtils.hasText(delta)) {
            return;
        }
        try {
            String auditId = RunAuditService.auditIdFor(runId);
            String traceId = RunAuditService.traceIdFor(runId);
            sendEvent(emitter, eventMap("answer_delta", mapOf(
                "run_id", runId,
                "delta", delta,
                "delta_source", deltaSource,
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 answer_delta 失败", ex);
        }
    }

    public void emitRunCancelled(SseEmitter emitter, String runId, String reason) {
        try {
            String auditId = RunAuditService.auditIdFor(runId);
            String traceId = RunAuditService.traceIdFor(runId);
            sendEvent(emitter, eventMap("run_cancelled", mapOf(
                "run_id", runId,
                "reason", reason,
                "terminal_status", AgentTerminalStatus.CANCELLED.name(),
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (Exception ignored) {
            // 客户端可能已经断开；取消状态仍以 API 返回为准。
        }
    }

    /**
     * 发送统一终态事件：每个 run 只出现一次终态事件。
     *
     * <p>事件名由 {@link AgentTerminalStatus#terminalEventName()} 决定：
     * run_completed（COMPLETED / CONFIRMATION_PENDING）、run_failed、run_blocked、
     * run_exhausted、run_cancelled。所有终态事件都携带大写 terminal_status 字段；
     * 缺少 terminal_status 的旧事件不能作为新回归用例的成功依据。
     *
     * @param emitter              SSE emitter（可为 null，仅审计）
     * @param runId                运行 ID
     * @param status               终态
     * @param finalAnswer          正式回答（可为 null）
     * @param mode                 模式
     * @param llmStatus            llm 状态
     * @param planSource           计划来源
     * @param errorCode            稳定错误码（FAILED/EXHAUSTED 等非成功终态必填）
     * @param safeMessage          安全消息（非成功终态使用）
     * @param completedTools       已完成工具（EXHAUSTED 使用）
     * @param missingTargetTools   未完成目标工具（EXHAUSTED 使用）
     */
    public void emitTerminalEvent(
        SseEmitter emitter,
        String runId,
        AgentTerminalStatus status,
        String finalAnswer,
        String mode,
        String llmStatus,
        String planSource,
        String errorCode,
        String safeMessage,
        List<String> completedTools,
        List<String> missingTargetTools
    ) {
        String auditId = RunAuditService.auditIdFor(runId);
        String traceId = RunAuditService.traceIdFor(runId);
        Map<String, Object> payload = mapOf(
            "run_id", runId,
            "terminal_status", status.name(),
            "audit_id", auditId,
            "trace_id", traceId,
            "observability", observabilityFor(runId, auditId, traceId),
            "timestamp", System.currentTimeMillis()
        );
        if (finalAnswer != null) {
            payload.put("final_answer", finalAnswer);
        }
        if (mode != null) {
            payload.put("mode", mode);
        }
        if (llmStatus != null) {
            payload.put("llm_status", llmStatus);
        }
        if (planSource != null) {
            payload.put("plan_source", planSource);
        }
        if (errorCode != null) {
            payload.put("code", errorCode);
        }
        if (safeMessage != null) {
            payload.put("safe_message", safeMessage);
        }
        if (completedTools != null && !completedTools.isEmpty()) {
            payload.put("completed_tools", completedTools);
        }
        if (missingTargetTools != null && !missingTargetTools.isEmpty()) {
            payload.put("missing_target_tools", missingTargetTools);
        }
        try {
            sendEvent(emitter, eventMap(status.terminalEventName(), payload));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 " + status.terminalEventName() + " 失败", ex);
        }
    }

    /**
     * 发送上下文压缩事件（context_compacted）。
     *
     * <p>事件只携带边界、原因、压缩条数与摘要预览，不携带完整检查点原文。
     */
    public void emitContextCompacted(
        SseEmitter emitter,
        String runId,
        Long checkpointId,
        Long sourceBoundaryMessageId,
        int compactedCount,
        String summaryPreview,
        long inputTokenEstimate,
        long outputTokenEstimate,
        String reason,
        boolean reused
    ) {
        if (runId == null) {
            return;
        }
        try {
            Map<String, Object> payload = mapOf(
                "run_id", runId,
                "compacted_count", compactedCount,
                "input_token_estimate", inputTokenEstimate,
                "output_token_estimate", outputTokenEstimate,
                "reason", reason == null ? "context_budget_threshold" : reason,
                "reused", reused,
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            );
            if (checkpointId != null) {
                payload.put("checkpoint_id", checkpointId);
            }
            if (sourceBoundaryMessageId != null) {
                payload.put("source_boundary_message_id", sourceBoundaryMessageId);
            }
            if (summaryPreview != null) {
                payload.put("summary_preview", summaryPreview);
            }
            sendEvent(emitter, eventMap("context_compacted", payload));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 context_compacted 失败", ex);
        }
    }


    public AnswerDeltaBatcher newAnswerDeltaBatcher(SseEmitter emitter, String runId) {
        return new AnswerDeltaBatcher(emitter, runId);
    }

    public void sendEvent(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        Object runId = payload.get("run_id");
        boolean cancellationEvent = "run_cancelled".equals(payload.get("event_type"));
        if (runId instanceof String runIdText) {
            runAuditService.prepareSend(runIdText, payload, cancellationEvent);
        }
        payload.putIfAbsent("timestamp", System.currentTimeMillis());
        String payloadJson = objectMapper.writeValueAsString(payload);
        if (emitter != null) {
            emitter.send(SseEmitter.event().data(payloadJson));
        }
        if (runId instanceof String runIdText) {
            runAuditService.queueRunAuditEvent(runIdText, payload, payloadJson);
        }
    }

    public static Map<String, Object> eventMap(String eventType, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event_type", eventType);
        result.putAll(payload);
        return result;
    }

    public static String toolInputSummary(String toolName, Map<String, Object> toolInput) {
        String label = switch (toolName) {
            case "inventory_low_stock_lookup" -> "查询当前账号低库存商品";
            case "product_catalog_lookup" -> "查询当前账号商品目录";
            case "customer_receivable_lookup" -> "查询当前账号客户应收余额";
            case "supplier_payable_lookup" -> "查询当前账号供应商应付余额";
            case "sales_overview_lookup" -> "汇总当前账号近 7 天经营信号";
            case "sale_order_lookup" -> "查询当前账号最近销售单";
            case "purchase_order_lookup" -> "查询当前账号最近采购单";
            case "pay_order_lookup" -> "查询当前账号最近付款单";
            case "finance_record_lookup" -> "查询当前账号最近资金流水";
            default -> "执行当前账号只读查询";
        };
        Map<String, Object> safeInput = toolInput == null ? Map.of() : toolInput;
        if (safeInput.isEmpty()) {
            return label;
        }
        return label + "，参数 " + safeInput;
    }

    public static Map<String, Object> queryWindowFor(Map<String, Object> toolInput) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("owner_scope", "current_owner");
        if (toolInput != null) {
            window.putAll(toolInput);
        }
        return window;
    }

    private static String callId(String runId, String toolName, int sequence) {
        return sequence > 0
            ? RunAuditService.toolCallId(runId, sequence, toolName)
            : RunAuditService.toolCallId(runId, toolName);
    }

    private static Integer positiveSequence(int sequence) {
        return sequence > 0 ? sequence : null;
    }

    public static boolean isStreamEmissionFailure(Throwable ex) {
        if (ex == null) {
            return false;
        }
        String message = ex.getMessage();
        if (message != null && message.startsWith("发送 ")) {
            return true;
        }
        return isStreamEmissionFailure(ex.getCause());
    }

    public static V2AgentDtos.AgentObservabilityDto observabilityFor(String runId, String auditId, String traceId) {
        String safeRunId = safeText(runId, "run");
        return new V2AgentDtos.AgentObservabilityDto(
            safeRunId,
            safeRunId,
            traceId,
            auditId,
            "agent-run:" + safeRunId
        );
    }

    private static String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    /** 答案增量批处理器：合并短 delta、按阈值或来源切换 flush。 */
    public final class AnswerDeltaBatcher {
        private final SseEmitter emitter;
        private final String runId;
        private final StringBuilder buffer = new StringBuilder();
        private String deltaSource;
        private boolean emittedAnyDelta;

        private AnswerDeltaBatcher(SseEmitter emitter, String runId) {
            this.emitter = emitter;
            this.runId = runId;
        }

        public boolean accept(String delta, String nextDeltaSource) {
            if (!StringUtils.hasText(delta)) {
                return false;
            }
            if (!StringUtils.hasText(deltaSource)) {
                deltaSource = nextDeltaSource;
            }
            if (!deltaSource.equals(nextDeltaSource)) {
                flush();
                deltaSource = nextDeltaSource;
            }
            buffer.append(delta);
            if (!emittedAnyDelta || buffer.length() >= ANSWER_DELTA_BATCH_CHAR_THRESHOLD) {
                flush();
                return true;
            }
            return false;
        }

        public void flush() {
            if (!StringUtils.hasText(buffer.toString())) {
                buffer.setLength(0);
                return;
            }
            emitAnswerDeltaUnchecked(emitter, runId, buffer.toString(), deltaSource);
            buffer.setLength(0);
            emittedAnyDelta = true;
        }
    }

    /** 单次工具执行的审计字段累积器（input_summary、计数、游标等）。 */
    public static final class ToolAudit {
        private final String toolName;
        private final Map<String, Object> toolInput;
        private final long startedAt;
        private final int sequence;
        private final String modelToolCallId;
        private Integer returnedCount;
        private Integer totalCount;
        private Integer limit;
        private boolean truncated;

        private ToolAudit(String toolName, Map<String, Object> toolInput, long startedAt, int sequence, String modelToolCallId) {
            this.toolName = toolName;
            this.toolInput = toolInput == null ? Map.of() : toolInput;
            this.startedAt = startedAt;
            this.sequence = sequence;
            this.modelToolCallId = modelToolCallId;
        }

        public int sequence() {
            return sequence;
        }

        public void markReturned(int returnedCount) {
            this.returnedCount = Math.max(0, returnedCount);
        }

        public void markLimitedResult(int returnedCount, int limit) {
            markLimitedResult(returnedCount, limit, returnedCount >= limit);
        }

        public void markLimitedResult(int returnedCount, int limit, boolean maybeTruncated) {
            this.returnedCount = Math.max(0, returnedCount);
            this.limit = Math.max(0, limit);
            this.truncated = maybeTruncated;
        }

        public void markListResult(List<?> sourceRows, int returnedCount, int limit) {
            this.returnedCount = Math.max(0, returnedCount);
            this.totalCount = sourceRows == null ? 0 : sourceRows.size();
            this.limit = Math.max(0, limit);
            this.truncated = this.totalCount > this.returnedCount;
        }

        private Map<String, Object> eventFields() {
            Map<String, Object> fields = new LinkedHashMap<>();
            long completedAt = System.currentTimeMillis();
            fields.put("input_summary", toolInputSummary(toolName, toolInput));
            fields.put("query_window", queryWindowFor(toolInput));
            fields.put("tool_sequence", positiveSequence(sequence));
            if (StringUtils.hasText(modelToolCallId)) {
                fields.put("tool_call_id", modelToolCallId);
            }
            fields.put("selection_origin", StringUtils.hasText(modelToolCallId) ? "model_tool_call" : "unknown");
            fields.put("started_at", startedAt);
            fields.put("completed_at", completedAt);
            fields.put("duration_ms", Math.max(0L, completedAt - startedAt));
            if (returnedCount != null) {
                fields.put("returned_count", returnedCount);
            }
            if (totalCount != null) {
                fields.put("total_count", totalCount);
            }
            if (limit != null) {
                fields.put("limit", limit);
            }
            fields.put("is_truncated", truncated);
            fields.put("next_cursor", nextCursor());
            fields.put("evidence", evidenceSummary());
            return fields;
        }

        private Map<String, Object> facts() {
            Map<String, Object> fields = eventFields();
            fields.put("tool_name", toolName);
            fields.put("tool_input", toolInput);
            return fields;
        }

        private String nextCursor() {
            if (!truncated || limit == null || returnedCount == null) {
                return null;
            }
            return "offset:" + returnedCount + ":limit:" + limit;
        }

        private Map<String, Object> evidenceSummary() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "tool:" + toolName);
            evidence.put("scope", "current_owner");
            if (returnedCount != null) {
                evidence.put("returned_count", returnedCount);
            }
            if (totalCount != null) {
                evidence.put("total_count", totalCount);
            }
            evidence.put("is_truncated", truncated);
            return evidence;
        }
    }
}

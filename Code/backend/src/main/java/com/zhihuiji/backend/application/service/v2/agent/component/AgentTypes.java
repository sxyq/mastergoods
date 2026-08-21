package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 运行时跨组件共享的数据类型，从 V2AgentAiService 内部 record 抽取。
 * 包括工具规划、工具执行结果、响应负载、最终答案等。
 */
public final class AgentTypes {

    private AgentTypes() {
    }

    /** 工具规划结果，由 ToolPlanner 产出。 */
    public record AgentToolPlan(
        List<String> tools,
        String rationale,
        String source,
        Map<String, JsonNode> toolParams,
        String nativeResponseId,
        List<NativeToolCallBlock> nativeToolCallBlocks,
        String terminalAnswer
    ) {
        public AgentToolPlan(List<String> tools, String rationale) {
            this(tools, rationale, "llm", Map.of(), null, List.of(), null);
        }

        public AgentToolPlan(List<String> tools, String rationale, Map<String, JsonNode> toolParams) {
            this(tools, rationale, "llm", toolParams, null, List.of(), null);
        }

        public AgentToolPlan(List<String> tools, String rationale, String source, Map<String, JsonNode> toolParams) {
            this(tools, rationale, source, toolParams, null, List.of(), null);
        }

        public AgentToolPlan(List<String> tools, String rationale, String source, Map<String, JsonNode> toolParams, String nativeResponseId) {
            this(tools, rationale, source, toolParams, nativeResponseId, List.of(), null);
        }

        public AgentToolPlan(
            List<String> tools,
            String rationale,
            String source,
            Map<String, JsonNode> toolParams,
            String nativeResponseId,
            List<NativeToolCallBlock> nativeToolCallBlocks
        ) {
            this(tools, rationale, source, toolParams, nativeResponseId, nativeToolCallBlocks, null);
        }

        /**
         * 供 SSE 和审计使用的调用来源。只有模型原生 tool call 和模型 JSON 兼容计划
         * 才能标记为模型选择；服务端校验或兜底不得复用这两个来源。
         */
        public String selectionOrigin() {
            if (source == null) {
                return "unknown";
            }
            if (source.startsWith("native_tool_use")) {
                return "model_tool_call";
            }
            if (source.startsWith("model_json_plan")) {
                return "model_json_plan";
            }
            return source;
        }

        public String toolChoiceMode() {
            return source != null && source.startsWith("native_tool_use")
                ? "auto"
                : "json_compatibility";
        }
    }

    /** 单个工具执行结果。 */
    public record ToolExecutionResult(
        String toolName,
        String summary,
        JsonNode facts,
        boolean insufficient,
        String toolCallId,
        Integer sequence
    ) {
        public ToolExecutionResult(
            String toolName,
            String summary,
            JsonNode facts,
            boolean insufficient
        ) {
            this(toolName, summary, facts, insufficient, null, null);
        }

        public ToolExecutionResult withCallIdentity(String callId, Integer callSequence) {
            return new ToolExecutionResult(toolName, summary, facts, insufficient, callId, callSequence);
        }

        public Map<String, Object> queryAudit() {
            Map<String, Object> audit = new LinkedHashMap<>();
            JsonNode auditNode = facts == null ? null : facts.path("query_audit");
            if (auditNode == null || auditNode.isMissingNode() || !auditNode.isObject()) {
                return audit;
            }
            auditNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isNumber()) {
                    audit.put(entry.getKey(), value.numberValue());
                } else if (value.isBoolean()) {
                    audit.put(entry.getKey(), value.booleanValue());
                } else if (value.isTextual()) {
                    audit.put(entry.getKey(), value.asText());
                } else {
                    audit.put(entry.getKey(), value);
                }
            });
            return audit;
        }

        public long durationMs() {
            Object value = queryAudit().get("duration_ms");
            return value instanceof Number number ? number.longValue() : 0L;
        }
    }

    /** 工具执行失败信息。 */
    public record ToolFailureResult(
        String toolName,
        String safeMessage,
        String toolCallId,
        Integer sequence
    ) {
        public ToolFailureResult(String toolName, String safeMessage) {
            this(toolName, safeMessage, null, null);
        }

        public ToolFailureResult withCallIdentity(String callId, Integer callSequence) {
            return new ToolFailureResult(toolName, safeMessage, callId, callSequence);
        }
    }

    /**
     * 原生 Function Calling 的工具调用块（从 provider 响应中提取）。
     *
     * <p>用于 function_call_output 续轮：carry 模型返回的 call_id、工具名和参数，
     * 服务端执行后以 {@link #toolCallId()} 作为 call_id 回传 function_call_output。
     *
     * @param toolCallId provider 返回的 function_call ID（call_id）
     * @param toolName   工具名
     * @param arguments  模型生成的参数 JSON 字符串
     */
    public record NativeToolCallBlock(String toolCallId, String toolName, String arguments) {}

    /** 一次 Agent 响应的完整负载。 */
    public record ResponsePayload(
        List<V2AgentDtos.ResultBlockDto> blocks,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        AgentToolPlan plan
    ) {
        public ResponsePayload(
            List<V2AgentDtos.ResultBlockDto> blocks,
            List<ToolExecutionResult> toolResults
        ) {
            this(blocks, toolResults, List.of(),
                new AgentToolPlan(List.of(), "工具直接返回", "tool", Map.of()));
        }

        public String planSource() {
            return plan == null ? "tool" : plan.source();
        }

        public String planSummary() {
            if (plan == null) {
                return "";
            }
            if (plan.tools() == null || plan.tools().isEmpty()) {
                return plan.rationale() == null ? "" : plan.rationale();
            }
            return plan.rationale() + "：" + String.join("、", plan.tools());
        }

        /**
         * 返回原生 Function Calling 首轮响应的 provider response ID。
         *
         * <p>仅当 plan.source 为 native_tool_use* 时有值，用于 function_call_output 续轮。
         * provider 不支持续轮时为 null，由 AnswerSynthesizer 降级到应用层 facts 注入。
         */
        public String nativeResponseId() {
            return plan == null ? null : plan.nativeResponseId();
        }

        /**
         * 返回原生 Function Calling 的工具调用块列表（含 call_id）。
         *
         * <p>用于 function_call_output 续轮：将服务端执行结果以 call_id 关联回传给模型。
         */
        public List<NativeToolCallBlock> nativeToolCallBlocks() {
            return plan == null || plan.nativeToolCallBlocks() == null
                ? List.of() : plan.nativeToolCallBlocks();
        }

        public long toolDurationMs() {
            long total = 0L;
            if (toolResults != null) {
                for (ToolExecutionResult result : toolResults) {
                    total += Math.max(0L, result.durationMs());
                }
            }
            return total;
        }
    }

    /** 最终答案，由 AnswerSynthesizer 产出。 */
    public record FinalAnswer(String answer, String mode, String llmStatus, boolean modelAttempted) {}
}

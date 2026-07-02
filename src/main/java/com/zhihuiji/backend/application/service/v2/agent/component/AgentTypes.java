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
        Map<String, JsonNode> toolParams
    ) {
        public AgentToolPlan(List<String> tools, String rationale) {
            this(tools, rationale, "llm", Map.of());
        }

        public AgentToolPlan(List<String> tools, String rationale, Map<String, JsonNode> toolParams) {
            this(tools, rationale, "llm", toolParams);
        }
    }

    /** 单个工具执行结果。 */
    public record ToolExecutionResult(
        String toolName,
        String summary,
        JsonNode facts,
        boolean insufficient
    ) {
        public String toolCallId() {
            return "tool:" + toolName;
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
    public record ToolFailureResult(String toolName, String safeMessage) {}

    /** 一次 Agent 响应的完整负载。 */
    public record ResponsePayload(
        String answer,
        List<V2AgentDtos.ResultBlockDto> blocks,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        AgentToolPlan plan
    ) {
        public ResponsePayload(
            String answer,
            List<V2AgentDtos.ResultBlockDto> blocks,
            List<ToolExecutionResult> toolResults
        ) {
            this(answer, blocks, toolResults, List.of(),
                new AgentToolPlan(List.of(), "工具直接返回", "tool", Map.of()));
        }

        public String planSource() {
            return plan == null ? "tool" : plan.source();
        }

        public String planSummary() {
            if (plan == null) {
                return "工具直接返回";
            }
            if (plan.tools() == null || plan.tools().isEmpty()) {
                return plan.rationale() + "：未匹配到已接入的真实查询工具";
            }
            return plan.rationale() + "：" + String.join("、", plan.tools());
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

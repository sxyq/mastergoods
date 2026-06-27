package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 工具共享基类，提供格式化、SSE 推送与审计辅助方法。
 *
 * <p>所有只读/写入工具可继承此类，复用 {@link #money}、{@link #formatNumber}、
 * {@link #safeDouble}、{@link #safeLong}、{@link #mapOf}、{@link #toJsonNode}、
 * {@link #limit} 等工具方法，以及 {@link #startAudit}、{@link #emitToolCompleted}、
 * {@link #emitToolFailed} 等 SSE 推送方法。
 *
 * <p>设计原则：
 * <ul>
 *   <li>格式化方法与 V2AgentAiService 保持一致，确保前端展示统一</li>
 *   <li>SSE 推送方法通过 {@link ToolContext} 获取 emitter 与 runId，工具无需直接依赖 V2AgentAiService</li>
 *   <li>审计信息封装在 {@link ToolAudit} 中，工具执行结束时通过 emitToolCompleted 推送</li>
 * </ul>
 */
public abstract class ToolSupport implements AgentTool {

    /** 默认工具返回条数上限。 */
    protected static final int DEFAULT_TOOL_LIMIT = 10;

    /** 默认工具输入参数（limit=10）。 */
    protected static final Map<String, Object> DEFAULT_LIMIT_TOOL_INPUT = Map.of("limit", DEFAULT_TOOL_LIMIT);

    /**
     * 货币格式化（与 V2AgentAiService.money 保持一致）。
     *
     * @param value 金额
     * @return 格式化后的字符串，如 ¥123.45
     */
    protected String money(double value) {
        return String.format(Locale.US, "¥%.2f", value);
    }

    /**
     * 数字格式化（整数显示无小数，浮点数保留两位）。
     *
     * @param value 数值
     * @return 格式化后的字符串
     */
    protected String formatNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001
            ? String.valueOf((long) Math.rint(value))
            : String.format(Locale.US, "%.2f", value);
    }

    /**
     * null 安全的 Double 转换。
     *
     * @param value 可能为 null 的 Double
     * @return 非 null 的 double 值
     */
    protected double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    /**
     * null 安全的 Long 转换。
     *
     * @param value 可能为 null 的 Long
     * @return 非 null 的 long 值
     */
    protected long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 任意对象的安全 double 转换。
     *
     * @param value Number 或其他对象
     * @return double 值（非 Number 返回 0）
     */
    protected double safeDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    /**
     * 任意对象的安全 long 转换。
     *
     * @param value Number 或其他对象
     * @return long 值（非 Number 返回 0）
     */
    protected long safeLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * 快速构建 Map（键值交替传入）。
     *
     * @param values 键值对序列，如 {"name", "张三", "age", 18}
     * @return LinkedHashMap（保持插入顺序）
     */
    protected Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    /**
     * 对象转 JsonNode（使用上下文的 ObjectMapper）。
     *
     * @param ctx       工具上下文
     * @param value 待转换对象
     * @return JsonNode
     */
    protected JsonNode toJsonNode(ToolContext ctx, Object value) {
        return ctx.objectMapper().valueToTree(value);
    }

    /**
     * 列表截断（不修改原列表）。
     *
     * @param items 原列表
     * @param size  最大长度
     * @return 截断后的列表
     */
    protected <T> List<T> limit(List<T> items, int size) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.size() <= size ? items : items.subList(0, size);
    }

    /**
     * 从参数中读取字符串值。
     *
     * @param params 参数节点
     * @param key    参数名
     * @return 字符串值（null 或缺失返回 null）
     */
    protected String paramString(JsonNode params, String key) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * 从参数中读取整数值。
     *
     * @param params       参数节点
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 整数值（null 或缺失返回 defaultValue）
     */
    protected Integer paramInt(JsonNode params, String key, Integer defaultValue) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        try {
            return node.asInt(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * 从参数中读取长整数值。
     *
     * @param params       参数节点
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 长整数值（null 或缺失返回 defaultValue）
     */
    protected Long paramLong(JsonNode params, String key, Long defaultValue) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        try {
            return node.asLong(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * 从参数中读取双精度浮点值。
     *
     * @param params       参数节点
     * @param key          参数名
     * @param defaultValue 默认值
     * @return double 值（null 或缺失返回 defaultValue）
     */
    protected Double paramDouble(JsonNode params, String key, Double defaultValue) {
        JsonNode node = params == null ? null : params.get(key);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        try {
            return node.asDouble(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    // ===== SSE 推送与审计辅助 =====

    /**
     * 开始工具审计（推送 tool_started 事件）。
     *
     * @param ctx      工具上下文
     * @param toolName 工具名
     * @param input    工具输入参数
     * @return 审计对象
     */
    protected ToolAudit startAudit(ToolContext ctx, String toolName, Map<String, Object> input) {
        ToolAudit audit = new ToolAudit(toolName, input, System.currentTimeMillis());
        emitToolStarted(ctx, toolName, input);
        return audit;
    }

    /**
     * 推送 tool_started 事件。
     *
     * @param ctx      工具上下文
     * @param toolName 工具名
     * @param input    工具输入参数
     */
    protected void emitToolStarted(ToolContext ctx, String toolName, Map<String, Object> input) {
        if (!ctx.isStreaming() || ctx.runId() == null) {
            return;
        }
        Map<String, Object> payload = mapOf(
            "run_id", ctx.runId(),
            "tool_call_id", toolCallId(ctx.runId(), toolName),
            "tool_name", toolName,
            "input_summary", toolInputSummary(toolName, input),
            "tool_input", input == null ? Map.of() : input,
            "started_at", System.currentTimeMillis(),
            "timestamp", System.currentTimeMillis()
        );
        sendEvent(ctx, "tool_started", payload);
    }

    /**
     * 推送 tool_completed 事件。
     *
     * @param ctx           工具上下文
     * @param toolName      工具名
     * @param resultSummary 结果摘要
     * @param audit         审计对象（可为 null）
     */
    protected void emitToolCompleted(ToolContext ctx, String toolName, String resultSummary, ToolAudit audit) {
        if (!ctx.isStreaming() || ctx.runId() == null) {
            return;
        }
        Map<String, Object> payload = mapOf(
            "run_id", ctx.runId(),
            "tool_call_id", toolCallId(ctx.runId(), toolName),
            "tool_name", toolName,
            "result_summary", resultSummary,
            "timestamp", System.currentTimeMillis()
        );
        if (audit != null) {
            payload.putAll(audit.eventFields());
        }
        sendEvent(ctx, "tool_completed", payload);
    }

    /**
     * 推送 tool_failed 事件。
     *
     * @param ctx          工具上下文
     * @param toolName     工具名
     * @param errorMessage 错误信息
     */
    protected void emitToolFailed(ToolContext ctx, String toolName, String errorMessage) {
        if (!ctx.isStreaming() || ctx.runId() == null) {
            return;
        }
        Map<String, Object> payload = mapOf(
            "run_id", ctx.runId(),
            "tool_call_id", toolCallId(ctx.runId(), toolName),
            "tool_name", toolName,
            "error", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        sendEvent(ctx, "tool_failed", payload);
    }

    /**
     * 推送 result_block 事件（逐块发送结果）。
     *
     * @param ctx    工具上下文
     * @param blocks 结果块列表
     */
    protected void emitBlocks(ToolContext ctx, List<?> blocks) {
        if (!ctx.isStreaming() || ctx.runId() == null || blocks == null || blocks.isEmpty()) {
            return;
        }
        for (Object block : blocks) {
            Map<String, Object> payload = mapOf(
                "run_id", ctx.runId(),
                "block", block,
                "timestamp", System.currentTimeMillis()
            );
            sendEvent(ctx, "result_block", payload);
        }
    }

    private void sendEvent(ToolContext ctx, String eventName, Map<String, Object> payload) {
        if (ctx.emitter() == null) {
            return;
        }
        try {
            ObjectMapper mapper = ctx.objectMapper();
            String json = mapper.writeValueAsString(payload);
            ctx.emitter().send(SseEmitter.event().name(eventName).data(json));
        } catch (IOException ex) {
            // SSE 发送失败不影响工具执行结果
        }
    }

    private String toolCallId(String runId, String toolName) {
        return runId + ":" + toolName;
    }

    private String toolInputSummary(String toolName, Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return "无参数";
        }
        return input.toString();
    }

    /**
     * 工具审计信息（跟踪返回数、总数、是否截断等）。
     */
    protected static final class ToolAudit {
        private final String toolName;
        private final Map<String, Object> toolInput;
        private final long startedAt;
        private Integer returnedCount;
        private Integer totalCount;
        private Integer limit;
        private boolean truncated;

        private ToolAudit(String toolName, Map<String, Object> toolInput, long startedAt) {
            this.toolName = toolName;
            this.toolInput = toolInput == null ? Map.of() : toolInput;
            this.startedAt = startedAt;
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

        public Map<String, Object> eventFields() {
            Map<String, Object> fields = new LinkedHashMap<>();
            long completedAt = System.currentTimeMillis();
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
            return fields;
        }

        public Map<String, Object> facts() {
            Map<String, Object> fields = eventFields();
            fields.put("tool_name", toolName);
            fields.put("tool_input", toolInput);
            return fields;
        }
    }
}

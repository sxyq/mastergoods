package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Agent 工具注册表。
 *
 * <p>Spring 启动时自动扫描所有 {@link AgentTool} Bean 并注册到 {@link ConcurrentHashMap}。
 * 后续工具扩展只需实现 {@code AgentTool} + {@code @Component}，0 核心代码改动。
 *
 * <p>提供工具查询、权限过滤与兼容执行入口。{@code V2AgentAiService} 通过此注册表
 * 获取工具定义，但生产 Agent 执行链统一使用 {@link ToolExecutor}；本类的
 * {@link #executeTool} 只供隔离测试和旧调用方使用，不承担 owner、权限或确认门。
 *
 * <p>安全约束：
 * <ul>
 *   <li>{@link AgentTool.ToolType#CREATE_ONLY} 工具不直接执行写入，由调用方走草稿确认流程</li>
 *   <li>所有工具执行结果由调用方按 owner 隔离审计</li>
 * </ul>
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    /**
     * 构造注册表，Spring 自动注入所有 {@link AgentTool} Bean。
     *
     * @param registeredTools 已注册的工具列表（可为空）
     */
    public ToolRegistry(List<AgentTool> registeredTools) {
        if (registeredTools != null) {
            for (AgentTool tool : registeredTools) {
                AgentTool previous = tools.put(tool.name(), tool);
                if (previous != null) {
                    throw new IllegalStateException(
                        "AgentTool 名称冲突：" + tool.name()
                            + " 已被 " + previous.getClass().getName() + " 注册");
                }
            }
        }
    }

    /**
     * 按名称获取工具。
     *
     * @param name 工具名
     * @return 工具 Optional
     */
    public Optional<AgentTool> getTool(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 判断工具是否已注册。
     *
     * @param name 工具名
     * @return 已注册返回 true
     */
    public boolean isRegistered(String name) {
        return name != null && tools.containsKey(name);
    }

    /**
     * 获取全部已注册工具。
     *
     * @return 不可变工具列表
     */
    public List<AgentTool> listTools() {
        return tools.values().stream()
            .sorted(Comparator.comparing(AgentTool::name))
            .toList();
    }

    /**
     * 获取全部只读查询工具（可被 LLM 规划器直接调用）。
     *
     * @return 只读工具列表
     */
    public List<AgentTool> listReadOnlyTools() {
        List<AgentTool> readonly = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.READ_ONLY) {
                readonly.add(tool);
            }
        }
        readonly.sort(Comparator.comparing(AgentTool::name));
        return Collections.unmodifiableList(readonly);
    }

    /**
     * 获取全部仅创建工具（强制走草稿确认流程）。
     *
     * @return 创建工具列表
     */
    public List<AgentTool> listCreateTools() {
        List<AgentTool> createOnly = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.CREATE_ONLY) {
                createOnly.add(tool);
            }
        }
        createOnly.sort(Comparator.comparing(AgentTool::name));
        return Collections.unmodifiableList(createOnly);
    }

    /**
     * 执行已注册工具。
     *
     * <p>兼容入口：调用方负责 owner、权限审查与（对 CREATE_ONLY 工具的）草稿确认流程。
     * 生产 Agent 链路必须调用 {@link ToolExecutor#execute}，此方法不应视为 Agent 安全执行入口。
     *
     * @param name   工具名
     * @param ctx    执行上下文
     * @param params LLM 提取的参数，可为 null
     * @return 工具执行结果 Optional（工具未注册时返回 empty）
     */
    public Optional<ToolResult> executeTool(String name, ToolContext ctx, JsonNode params) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            return Optional.empty();
        }
        if (ctx == null || ctx.ownerUserId() == null) {
            return Optional.of(ToolResult.failure(name, "工具执行上下文无效"));
        }
        JsonNode safeParams = params == null || params.isNull() ? ctx.objectMapper().createObjectNode() : params;
        JsonNode schema = tool.parameterSchema();
        if (schema != null && schema.isObject() && !satisfiesSchema(schema, safeParams)) {
            return Optional.of(ToolResult.failure(name, "工具参数不符合声明的参数约束"));
        }
        return Optional.of(tool.execute(ctx, safeParams));
    }

    /**
     * 判断模型是否已经给齐工具声明的必填参数。
     *
     * <p>创建类工具经常需要先查询商品、账户或订单才能拿到 ID。这个判断用于阻止
     * “先查出来、同时拿空 ID 创建”的错误调用，让 Agent 有机会在下一轮用真实 facts
     * 补齐参数后再生成草稿。
     */
    public boolean hasAllRequiredParameters(String name, JsonNode params) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            return false;
        }
        JsonNode schema = tool.parameterSchema();
        JsonNode required = schema == null ? null : schema.get("required");
        if (required == null || !required.isArray() || required.isEmpty()) {
            if (params == null || params.isNull()) {
                return true;
            }
            return schema == null || !schema.isObject() || satisfiesSchema(schema, params);
        }
        if (params == null || params.isNull()) {
            return false;
        }
        return satisfiesSchema(schema, params);
    }

    private boolean hasRequiredValues(JsonNode schema, JsonNode params) {
        if (schema == null || !schema.isObject() || params == null || !params.isObject()) {
            return false;
        }
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode requiredName : required) {
                if (!requiredName.isTextual() || !hasMeaningfulValue(params, requiredName.asText())) {
                    return false;
                }
                JsonNode value = params.get(requiredName.asText());
                JsonNode propertySchema = schema.path("properties").get(requiredName.asText());
                if (propertySchema != null && !satisfiesSchema(propertySchema, value)) {
                    return false;
                }
            }
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            var fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = params.get(entry.getKey());
                if (value != null && !value.isNull() && !satisfiesSchema(entry.getValue(), value)) {
                    return false;
                }
            }
            if (schema.path("additionalProperties").isBoolean()
                && !schema.path("additionalProperties").asBoolean()) {
                var inputFields = params.fieldNames();
                while (inputFields.hasNext()) {
                    if (!properties.has(inputFields.next())) {
                        return false;
                    }
                }
            }
        } else if (schema.path("additionalProperties").isBoolean()
            && !schema.path("additionalProperties").asBoolean()
            && params.size() > 0) {
            return false;
        }
        return true;
    }

    /*
     * Keep the registry check deliberately small and deterministic.  Tool
     * schemas are generated in-process, so a full JSON Schema dependency
     * would add more runtime surface than this boundary needs.
     */
    private boolean satisfiesSchema(JsonNode schema, JsonNode value) {
        if (schema == null || !schema.isObject()) {
            return true;
        }
        if (value == null || value.isNull()) {
            return false;
        }

        String type = schema.path("type").asText("");
        if ("object".equals(type)) {
            return value.isObject() && hasRequiredValues(schema, value);
        }
        if ("array".equals(type)) {
            if (!value.isArray()) {
                return false;
            }
            int minItems = schema.path("minItems").asInt(0);
            int maxItems = schema.path("maxItems").asInt(Integer.MAX_VALUE);
            if (value.size() < minItems || value.size() > maxItems) {
                return false;
            }
            JsonNode itemSchema = schema.get("items");
            if (itemSchema != null && itemSchema.isObject()) {
                for (JsonNode item : value) {
                    if (!satisfiesSchema(itemSchema, item)) {
                        return false;
                    }
                }
            }
            return true;
        }
        if ("string".equals(type)) {
            if (!value.isTextual()) {
                return false;
            }
            int minLength = schema.path("minLength").asInt(0);
            int maxLength = schema.path("maxLength").asInt(Integer.MAX_VALUE);
            return value.asText().length() >= minLength && value.asText().length() <= maxLength;
        }
        if ("integer".equals(type) && !value.isIntegralNumber()) {
            return false;
        }
        if ("boolean".equals(type) && !value.isBoolean()) {
            return false;
        }
        if ("number".equals(type) && (!value.isNumber() || !Double.isFinite(value.asDouble()))) {
            return false;
        }

        JsonNode enumValues = schema.get("enum");
        if (enumValues != null && enumValues.isArray()) {
            boolean matches = false;
            for (JsonNode enumValue : enumValues) {
                if (enumValue.equals(value)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                return false;
            }
        }

        JsonNode minimum = schema.get("minimum");
        if (minimum != null && minimum.isNumber()
            && (!value.isNumber() || value.asDouble() < minimum.asDouble())) {
            return false;
        }
        JsonNode exclusiveMinimum = schema.get("exclusiveMinimum");
        if (exclusiveMinimum != null && exclusiveMinimum.isNumber()
            && (!value.isNumber() || value.asDouble() <= exclusiveMinimum.asDouble())) {
            return false;
        }
        JsonNode maximum = schema.get("maximum");
        if (maximum != null && maximum.isNumber()
            && (!value.isNumber() || value.asDouble() > maximum.asDouble())) {
            return false;
        }
        JsonNode exclusiveMaximum = schema.get("exclusiveMaximum");
        if (exclusiveMaximum != null && exclusiveMaximum.isNumber()
            && (!value.isNumber() || value.asDouble() >= exclusiveMaximum.asDouble())) {
            return false;
        }
        return true;
    }

    private boolean hasMeaningfulValue(JsonNode params, String name) {
        if (params == null || !params.isObject()) {
            return false;
        }
        JsonNode value = params.get(name);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isTextual()) {
            return !value.asText().isBlank();
        }
        if (value.isArray() || value.isObject()) {
            return !value.isEmpty();
        }
        return true;
    }

    /**
     * 生成给 LLM 规划器的工具描述清单。
     *
     * <p>每行格式：{@code - {工具名}：{描述}}，用于拼入规划器 system prompt。
     * 同时列出只读查询工具和仅创建工具；仅创建工具描述追加草稿确认提示。
     *
     * @return 工具描述文本
     */
    public String buildToolCatalogForLlm() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            if (tool.type() == AgentTool.ToolType.READ_ONLY) {
                sb.append("- ").append(tool.name()).append("：").append(tool.description()).append('\n');
            } else if (tool.type() == AgentTool.ToolType.CREATE_ONLY) {
                sb.append("- ").append(tool.name()).append("：").append(tool.description())
                    .append("（生成草稿，需用户确认后执行）").append('\n');
            }
        }
        return sb.toString();
    }
}

package com.zhihuiji.backend.application.service.v2.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具参数结构化校验器。
 *
 * <p>在业务 Repository 访问前完成 required、类型、minimum、maximum、minItems、
 * maxItems、枚举与非法字段（additionalProperties=false）检查。每个违反返回带
 * 字段路径（如 {@code $.items[0].quantity}）、约束类型与稳定错误码的结构化错误，
 * 供模型修正参数与客户端展示使用。
 *
 * <p>参数错误不得进入业务 Repository；错误码统一为 TOOL_ARGUMENTS_INVALID，
 * 每条 violation 携带 constraint 区分具体约束。
 */
public final class ToolArgumentsValidator {

    public static final String CODE_REQUIRED_MISSING = "required_missing";
    public static final String CODE_TYPE_MISMATCH = "type_mismatch";
    public static final String CODE_MINIMUM = "minimum";
    public static final String CODE_MAXIMUM = "maximum";
    public static final String CODE_EXCLUSIVE_MINIMUM = "exclusive_minimum";
    public static final String CODE_EXCLUSIVE_MAXIMUM = "exclusive_maximum";
    public static final String CODE_MIN_LENGTH = "min_length";
    public static final String CODE_MAX_LENGTH = "max_length";
    public static final String CODE_MIN_ITEMS = "min_items";
    public static final String CODE_MAX_ITEMS = "max_items";
    public static final String CODE_ENUM = "enum";
    public static final String CODE_UNKNOWN_FIELD = "unknown_field";
    public static final String CODE_NOT_OBJECT = "not_object";

    private ToolArgumentsValidator() {
    }

    /** 单条结构化参数错误：字段路径 + 约束 + 稳定码 + 安全描述。 */
    public record Violation(String fieldPath, String constraint, String code, String message) {}

    /**
     * 按工具声明的 JSON Schema 校验参数，返回全部违反项（空列表表示通过）。
     *
     * @param schema 工具参数 Schema（可为 null 表示无约束）
     * @param params 模型给出的参数
     * @return 违反列表
     */
    public static List<Violation> validate(JsonNode schema, JsonNode params) {
        List<Violation> violations = new ArrayList<>();
        if (schema == null || !schema.isObject()) {
            return violations;
        }
        if (params == null || params.isNull()) {
            // 仅当存在 required 字段时报缺失；否则空参数合法。
            JsonNode required = schema.get("required");
            if (required != null && required.isArray()) {
                for (JsonNode name : required) {
                    if (name.isTextual()) {
                        violations.add(new Violation(
                            "$." + name.asText(), "required", CODE_REQUIRED_MISSING,
                            "缺少必填字段 " + name.asText()
                        ));
                    }
                }
            }
            return violations;
        }
        validateNode(schema, params, "$", violations);
        return violations;
    }

    private static void validateNode(JsonNode schema, JsonNode value, String path, List<Violation> violations) {
        String type = schema.path("type").asText("");
        if ("object".equals(type)) {
            validateObject(schema, value, path, violations);
            return;
        }
        if ("array".equals(type)) {
            validateArray(schema, value, path, violations);
            return;
        }
        validateScalar(schema, value, path, violations);
    }

    private static void validateObject(JsonNode schema, JsonNode value, String path, List<Violation> violations) {
        if (!value.isObject()) {
            violations.add(new Violation(path, "type=object", CODE_TYPE_MISMATCH, path + " 应为对象"));
            return;
        }
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode name : required) {
                if (!name.isTextual()) {
                    continue;
                }
                JsonNode fieldValue = value.get(name.asText());
                if (isMissing(fieldValue)) {
                    violations.add(new Violation(
                        path + "." + name.asText(), "required", CODE_REQUIRED_MISSING,
                        "缺少必填字段 " + name.asText()
                    ));
                }
            }
        }
        JsonNode properties = schema.get("properties");
        boolean propertiesIsObject = properties != null && properties.isObject();
        if (propertiesIsObject) {
            var fields = properties.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode fieldValue = value.get(entry.getKey());
                if (!isMissing(fieldValue)) {
                    validateNode(entry.getValue(), fieldValue, path + "." + entry.getKey(), violations);
                }
            }
        }
        JsonNode additional = schema.get("additionalProperties");
        if (additional != null && additional.isBoolean() && !additional.asBoolean()) {
            var inputFields = value.fieldNames();
            while (inputFields.hasNext()) {
                String fieldName = inputFields.next();
                if (!propertiesIsObject || !properties.has(fieldName)) {
                    violations.add(new Violation(
                        path + "." + fieldName, "additionalProperties=false", CODE_UNKNOWN_FIELD,
                        "非法字段 " + fieldName
                    ));
                }
            }
        }
    }

    private static void validateArray(JsonNode schema, JsonNode value, String path, List<Violation> violations) {
        if (!value.isArray()) {
            violations.add(new Violation(path, "type=array", CODE_TYPE_MISMATCH, path + " 应为数组"));
            return;
        }
        int minItems = schema.path("minItems").asInt(0);
        int maxItems = schema.path("maxItems").asInt(Integer.MAX_VALUE);
        if (value.size() < minItems) {
            violations.add(new Violation(path, "minItems=" + minItems, CODE_MIN_ITEMS,
                path + " 至少需要 " + minItems + " 项"));
        }
        if (value.size() > maxItems) {
            violations.add(new Violation(path, "maxItems=" + maxItems, CODE_MAX_ITEMS,
                path + " 最多允许 " + maxItems + " 项"));
        }
        JsonNode itemSchema = schema.get("items");
        if (itemSchema != null && itemSchema.isObject()) {
            for (int i = 0; i < value.size(); i++) {
                validateNode(itemSchema, value.get(i), path + "[" + i + "]", violations);
            }
        }
    }

    private static void validateScalar(JsonNode schema, JsonNode value, String path, List<Violation> violations) {
        String type = schema.path("type").asText("");
        switch (type) {
            case "string" -> {
                if (!value.isTextual()) {
                    violations.add(new Violation(path, "type=string", CODE_TYPE_MISMATCH, path + " 应为字符串"));
                    return;
                }
                int minLength = schema.path("minLength").asInt(0);
                int maxLength = schema.path("maxLength").asInt(Integer.MAX_VALUE);
                int length = value.asText().length();
                if (length < minLength) {
                    violations.add(new Violation(path, "minLength=" + minLength, CODE_MIN_LENGTH,
                        path + " 长度不足"));
                }
                if (length > maxLength) {
                    violations.add(new Violation(path, "maxLength=" + maxLength, CODE_MAX_LENGTH,
                        path + " 超过最大长度"));
                }
            }
            case "integer" -> {
                if (!value.isIntegralNumber()) {
                    violations.add(new Violation(path, "type=integer", CODE_TYPE_MISMATCH, path + " 应为整数"));
                    return;
                }
            }
            case "number" -> {
                if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
                    violations.add(new Violation(path, "type=number", CODE_TYPE_MISMATCH, path + " 应为数字"));
                    return;
                }
            }
            case "boolean" -> {
                if (!value.isBoolean()) {
                    violations.add(new Violation(path, "type=boolean", CODE_TYPE_MISMATCH, path + " 应为布尔值"));
                    return;
                }
            }
            default -> {
                // 未声明类型时不做类型检查，仅继续校验数值与枚举约束。
            }
        }
        if (!value.isNumber()) {
            return;
        }
        double numeric = value.asDouble();
        JsonNode minimum = schema.get("minimum");
        if (minimum != null && minimum.isNumber() && numeric < minimum.asDouble()) {
            violations.add(new Violation(path, "minimum=" + minimum.asDouble(), CODE_MINIMUM,
                path + " 小于最小值 " + minimum.asDouble()));
        }
        JsonNode exclusiveMinimum = schema.get("exclusiveMinimum");
        if (exclusiveMinimum != null && exclusiveMinimum.isNumber() && numeric <= exclusiveMinimum.asDouble()) {
            violations.add(new Violation(path, "exclusiveMinimum=" + exclusiveMinimum.asDouble(),
                CODE_EXCLUSIVE_MINIMUM, path + " 必须大于 " + exclusiveMinimum.asDouble()));
        }
        JsonNode maximum = schema.get("maximum");
        if (maximum != null && maximum.isNumber() && numeric > maximum.asDouble()) {
            violations.add(new Violation(path, "maximum=" + maximum.asDouble(), CODE_MAXIMUM,
                path + " 大于最大值 " + maximum.asDouble()));
        }
        JsonNode exclusiveMaximum = schema.get("exclusiveMaximum");
        if (exclusiveMaximum != null && exclusiveMaximum.isNumber() && numeric >= exclusiveMaximum.asDouble()) {
            violations.add(new Violation(path, "exclusiveMaximum=" + exclusiveMaximum.asDouble(),
                CODE_EXCLUSIVE_MAXIMUM, path + " 必须小于 " + exclusiveMaximum.asDouble()));
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
                violations.add(new Violation(path, "enum", CODE_ENUM, path + " 不在允许的枚举值内"));
            }
        }
    }

    private static boolean isMissing(JsonNode value) {
        if (value == null || value.isNull()) {
            return true;
        }
        if (value.isTextual()) {
            return value.asText().isBlank();
        }
        if (value.isArray() || value.isObject()) {
            return value.isEmpty();
        }
        return false;
    }
}

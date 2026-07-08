package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Decides whether queried facts should be rendered as adaptive result blocks.
 *
 * <p>This tool does not query business data. It lets the model explicitly opt in
 * to table/chart/KPI rendering after selecting the real data tools for the turn.
 */
@Component
public class ResultVisualizationTool extends ToolSupport {

    private final ObjectMapper objectMapper;

    public ResultVisualizationTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "result_visualization";
    }

    @Override
    public String displayName() {
        return "自适应结果展示";
    }

    @Override
    public String description() {
        return "当用户明确要求表格、图表、统计卡片、趋势、排行、对比等结构化展示时调用；"
            + "本工具只决定展示方式，不查询业务数据，必须和真实查询工具一起使用";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("mode")
            .put("type", "string")
            .put("description", "展示偏好：auto/table/chart/kpi/timeline，默认 auto");
        properties.putObject("reason")
            .put("type", "string")
            .put("description", "为什么本轮回答需要结构化展示");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String mode = paramString(params, "mode");
        if (mode == null || mode.isBlank()) {
            mode = "auto";
        }
        String reason = paramString(params, "reason");
        if (reason == null || reason.isBlank()) {
            reason = "本轮问题已明确要求结构化展示";
        }
        JsonNode facts = toJsonNode(ctx, Map.of(
            "visualization_enabled", true,
            "mode", mode,
            "reason", reason,
            "query_audit", Map.of(
                "owner_scope", "current_owner",
                "returned_count", 0,
                "total_count", 0,
                "limit", 0,
                "is_truncated", false
            )
        ));
        return ToolResult.success("", List.of(), facts, "已启用" + mode + "自适应结果展示");
    }
}

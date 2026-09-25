// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 数据导出辅助工具（framework-level 能力壳）。
 *
 * <p>返回可导出的数据范围说明、数据量预估、列字段清单与导出指令，
 * 不实际生成文件，由前端按指令调用现有导出端点。
 *
 * <p>旧领域的数据量预估与字段清单（销售/采购/库存/客户/供应商/收支）已随业务域删除；
 * 新领域的导出类型、列定义与预估查询在此壳内接入。
 */
@Component
public class DataExportTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final ObjectMapper objectMapper;

    public DataExportTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "data_export_tool";
    }

    @Override
    public String displayName() {
        return "数据导出辅助";
    }

    @Override
    public String description() {
        return "返回可导出数据范围、数据量预估、字段清单与导出指令";
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
        ObjectNode dataType = properties.putObject("data_type");
        dataType.put("type", "string");
        dataType.put("description", "导出数据类型（必填）");
        ObjectNode format = properties.putObject("format");
        format.put("type", "string");
        format.put("description", "导出格式：csv/json，默认 csv");
        ObjectNode days = properties.putObject("days");
        days.put("type", "integer");
        days.put("description", "导出时间窗口天数，默认 30");
        schema.putArray("required")
            .add("data_type");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String dataType = paramString(params, "data_type");
        String format = paramString(params, "format");
        int days = Math.max(1, paramInt(params, "days", 30));
        String fmt = format == null ? "csv" : format.toLowerCase();
        if (dataType == null) {
            String err = "缺少必填参数 data_type";
            emitToolFailed(ctx, name(), err);
            return ToolResult.failure(name(), err);
        }
        String type = dataType.toLowerCase();
        Map<String, Object> input = mapOf(
            "data_type", type,
            "format", fmt,
            "days", days
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<String> columns = columnsFor(type);
        String instruction = "请前往对应数据模块，选择时间范围近"
            + days + "天，格式 " + fmt + "，点击导出下载。";
        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "已生成 " + type + " 导出指引", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "导出概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "数据类型", "value", type, "trend_direction", "flat"),
                    mapOf("label", "导出格式", "value", fmt.toUpperCase(), "trend_direction", "flat"),
                    mapOf("label", "时间范围", "value", "近" + days + "天", "trend_direction", "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto columnBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "导出字段清单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("字段"),
                "rows", buildColumnRows(columns),
                "row_count", columns.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, columnBlock);
        int columnCount = columns.size();
        String toolSummary = "导出 " + type + " 指引已生成，格式 " + fmt
            + "，字段 " + columnCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "data_type", type,
            "format", fmt,
            "days", days,
            "column_count", columnCount,
            "columns", columns,
            "instruction", instruction,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildColumnRows(List<String> columns) {
        List<List<Object>> rows = new ArrayList<>(columns.size());
        for (String column : columns) {
            rows.add(List.of(column));
        }
        return rows;
    }

    private List<String> columnsFor(String type) {
        // 旧领域字段清单已删除；新领域导出类型的列定义在此接入。
        return List.of();
    }
}

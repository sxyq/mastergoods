package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.V2SyncService;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 同步状态查询工具。
 *
 * <p>查询当前账号的数据同步服务健康状态，返回服务状态、支持的实体类型与可上传实体类型，
 * 帮助用户了解同步能力与可用范围。
 */
@Component
public class SyncStatusLookupTool extends ToolSupport {

    private final V2SyncService syncService;

    public SyncStatusLookupTool(V2SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public String name() {
        return "sync_status_lookup";
    }

    @Override
    public String displayName() {
        return "同步状态查询";
    }

    @Override
    public String description() {
        return "查询数据同步服务健康状态、支持的实体类型与可上传实体类型";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Map<String, Object> input = mapOf();
        ToolAudit audit = startAudit(ctx, name(), input);

        V2SyncService.HealthResult health = syncService.health();
        emitToolCompleted(ctx, name(), "同步服务状态：" + health.status(), audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "同步服务状态",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "服务状态", "value", safeText(health.status(), "-"), "trend_direction", "ok".equalsIgnoreCase(health.status()) ? "up" : "down"),
                    mapOf("label", "支持实体数", "value", String.valueOf(health.supportedEntityTypes() == null ? 0 : health.supportedEntityTypes().size()), "trend_direction", "up"),
                    mapOf("label", "可上传实体数", "value", String.valueOf(health.uploadableEntityTypes() == null ? 0 : health.uploadableEntityTypes().size()), "trend_direction", "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto detailBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "同步能力详情",
            toJsonNode(ctx, mapOf(
                "headers", List.of("项目", "内容"),
                "rows", List.of(
                    List.of("服务状态", safeText(health.status(), "-")),
                    List.of("状态消息", safeText(health.message(), "-")),
                    List.of("Owner 隔离", Boolean.TRUE.equals(health.ownerScoped()) ? "是" : "否"),
                    List.of("服务器时间", String.valueOf(health.serverTime())),
                    List.of("支持实体类型", health.supportedEntityTypes() == null ? "" : String.join("、", health.supportedEntityTypes())),
                    List.of("可上传实体类型", health.uploadableEntityTypes() == null ? "" : String.join("、", health.uploadableEntityTypes()))
                ),
                "row_count", 6
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, detailBlock);
        String toolSummary = "同步服务状态 " + safeText(health.status(), "-");
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "status", health.status(),
            "message", health.message(),
            "owner_scoped", health.ownerScoped(),
            "server_time", health.serverTime(),
            "supported_entity_types", health.supportedEntityTypes(),
            "uploadable_entity_types", health.uploadableEntityTypes(),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

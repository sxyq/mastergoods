package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.CashChangeRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 资金变动查询工具，查询当前账号资金变动记录、收支流水与变动明细。
 */
@Component
public class CashChangeLookupTool extends ToolSupport {

    private final CashChangeRecordRepository cashChangeRecordRepository;

    public CashChangeLookupTool(CashChangeRecordRepository cashChangeRecordRepository) {
        this.cashChangeRecordRepository = cashChangeRecordRepository;
    }

    @Override
    public String name() {
        return "cash_change_lookup";
    }

    @Override
    public String displayName() {
        return "资金变动查询";
    }

    @Override
    public String description() {
        return "查询当前账号资金变动记录、收支流水与变动明细";
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
        Long ownerUserId = ctx.ownerUserId();
        Map<String, Object> input = mapOf("limit", DEFAULT_TOOL_LIMIT);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<CashChangeRecordEntity> records = cashChangeRecordRepository
            .findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        List<CashChangeRecordEntity> recent = limit(records, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(recent.size(), DEFAULT_TOOL_LIMIT);

        double totalChange = 0D;
        for (CashChangeRecordEntity item : recent) {
            totalChange += safeDouble(item.getChangeAmount());
        }
        emitToolCompleted(ctx, name(), "命中 " + recent.size() + " 条资金变动", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "资金变动概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近变动", "value", String.valueOf(recent.size()), "trend_direction", recent.isEmpty() ? "flat" : "up"),
                    mapOf("label", "变动金额合计", "value", money(totalChange), "trend_direction", totalChange > 0 ? "up" : (totalChange < 0 ? "down" : "flat"))
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近资金变动",
            toJsonNode(ctx, mapOf(
                "headers", List.of("账户", "变动金额", "类型", "时间"),
                "rows", buildCashChangeRows(recent),
                "row_count", recent.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "最近资金变动 " + recent.size() + " 条，合计 " + money(totalChange);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "record_count", recent.size(),
            "total_change_amount", money(totalChange),
            "query_audit", audit.facts(),
            "recent_records", buildCashChangeSummaries(recent)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildCashChangeRows(List<CashChangeRecordEntity> records) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (CashChangeRecordEntity item : records) {
            rows.add(List.of(
                safeText(item.getOrderType(), "-"),
                money(safeDouble(item.getChangeAmount())),
                safeText(item.getNotes(), "-"),
                formatTimestamp(item.getCreatedAt())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildCashChangeSummaries(List<CashChangeRecordEntity> records) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (CashChangeRecordEntity item : records) {
            items.add(mapOf(
                "order_type", safeText(item.getOrderType(), "-"),
                "change_amount", money(safeDouble(item.getChangeAmount())),
                "notes", safeText(item.getNotes(), "-"),
                "created_at", safeLong(item.getCreatedAt())
            ));
        }
        return items;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0L) {
            return "-";
        }
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.of("Asia/Shanghai"))
            .toLocalDateTime()
            .toString();
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 库存调整查询工具，查询当前账号库存调整记录、盘盈盘亏与调整明细。
 */
@Component
public class InventoryAdjustmentLookupTool extends ToolSupport {

    private static final int FLOW_TYPE_OUT = 0;
    private static final int FLOW_TYPE_IN = 1;

    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    public InventoryAdjustmentLookupTool(InventoryAdjustmentRepository inventoryAdjustmentRepository) {
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
    }

    @Override
    public String name() {
        return "inventory_adjustment_lookup";
    }

    @Override
    public String displayName() {
        return "库存调整查询";
    }

    @Override
    public String description() {
        return "查询当前账号库存调整记录、盘盈盘亏与调整明细";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        Map<String, Object> input = mapOf(
            "start_date", startDate,
            "end_date", endDate,
            "limit", DEFAULT_TOOL_LIMIT
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<InventoryAdjustmentEntity> records;
        if (startDate != null && endDate != null) {
            long startAt = Math.min(startDate, endDate);
            long endAt = Math.max(startDate, endDate);
            records = inventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, startAt, endAt);
            Collections.reverse(records);
        } else {
            records = inventoryAdjustmentRepository.findByOwnerUserIdOrderByCreatedAtAsc(ownerUserId);
            Collections.reverse(records);
        }
        List<InventoryAdjustmentEntity> recent = limit(records, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(recent.size(), DEFAULT_TOOL_LIMIT);

        double totalQuantity = 0D;
        long inflowCount = 0L;
        long outflowCount = 0L;
        for (InventoryAdjustmentEntity item : recent) {
            totalQuantity += safeDouble(item.getQuantity());
            Integer flowType = item.getFlowType();
            if (flowType != null && flowType == FLOW_TYPE_IN) {
                inflowCount += 1L;
            } else if (flowType != null && flowType == FLOW_TYPE_OUT) {
                outflowCount += 1L;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + recent.size() + " 条库存调整", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "库存调整概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "调整记录", "value", String.valueOf(recent.size()), "trend_direction", recent.isEmpty() ? "flat" : "up"),
                    mapOf("label", "盘盈次数", "value", String.valueOf(inflowCount), "trend_direction", inflowCount > 0 ? "up" : "flat"),
                    mapOf("label", "盘亏次数", "value", String.valueOf(outflowCount), "trend_direction", outflowCount > 0 ? "up" : "flat"),
                    mapOf("label", "调整数量合计", "value", formatNumber(totalQuantity), "trend_direction", totalQuantity > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "库存调整记录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "数量", "类型", "原因", "操作人"),
                "rows", buildAdjustmentRows(recent),
                "row_count", recent.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = recent.isEmpty()
            ? "当前账号下还没有库存调整记录。"
            : "我查到了最近 " + recent.size() + " 条库存调整，盘盈 " + inflowCount + " 次，盘亏 " + outflowCount + " 次。";
        String toolSummary = "库存调整 " + recent.size() + " 条，盘盈 " + inflowCount + "，盘亏 " + outflowCount;
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "record_count", recent.size(),
            "inflow_count", inflowCount,
            "outflow_count", outflowCount,
            "total_quantity", formatNumber(totalQuantity),
            "query_audit", audit.facts(),
            "records", buildAdjustmentSummaries(recent)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private String flowTypeLabel(Integer flowType) {
        if (flowType == null) {
            return "未知";
        }
        return switch (flowType) {
            case FLOW_TYPE_IN -> "盘盈";
            case FLOW_TYPE_OUT -> "盘亏";
            default -> "未知";
        };
    }

    private List<List<Object>> buildAdjustmentRows(List<InventoryAdjustmentEntity> records) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (InventoryAdjustmentEntity item : records) {
            rows.add(List.of(
                safeText(item.getProductName(), "-"),
                safeText(item.getProductCode(), "-"),
                formatNumber(safeDouble(item.getQuantity())),
                flowTypeLabel(item.getFlowType()),
                safeText(item.getReason(), "-"),
                safeText(item.getOperatorName(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildAdjustmentSummaries(List<InventoryAdjustmentEntity> records) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (InventoryAdjustmentEntity item : records) {
            items.add(mapOf(
                "id", safeLong(item.getId()),
                "product_id", safeLong(item.getProductId()),
                "product_name", safeText(item.getProductName(), "-"),
                "product_code", safeText(item.getProductCode(), "-"),
                "quantity", formatNumber(safeDouble(item.getQuantity())),
                "flow_type", flowTypeLabel(item.getFlowType()),
                "reason", safeText(item.getReason(), "-"),
                "operator_name", safeText(item.getOperatorName(), "-"),
                "created_at", safeLong(item.getCreatedAt())
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.InventorySnapshotEntity;
import com.zhihuiji.backend.infrastructure.repository.InventorySnapshotRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 库存快照查询工具。
 */
@Component
public class InventorySnapshotLookupTool extends ToolSupport {

    private final InventorySnapshotRepository inventorySnapshotRepository;

    public InventorySnapshotLookupTool(InventorySnapshotRepository inventorySnapshotRepository) {
        this.inventorySnapshotRepository = inventorySnapshotRepository;
    }

    @Override
    public String name() {
        return "inventory_snapshot_lookup";
    }

    @Override
    public String displayName() {
        return "库存快照查询";
    }

    @Override
    public String description() {
        return "查询当前账号库存快照、盘点记录与历史库存";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long snapshotDate = paramLong(params, "snapshot_date", null);
        Long productId = paramLong(params, "product_id", null);
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        Map<String, Object> input = mapOf(
            "snapshot_date", snapshotDate,
            "product_id", productId,
            "start_date", startDate,
            "end_date", endDate
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<InventorySnapshotEntity> snapshots;
        if (snapshotDate != null) {
            snapshots = inventorySnapshotRepository.findAllByOwnerUserIdAndSnapshotDateOrderByProductNameAsc(ownerUserId, snapshotDate);
        } else if (startDate != null && endDate != null) {
            snapshots = inventorySnapshotRepository.findAllByOwnerUserIdAndSnapshotDateBetweenOrderBySnapshotDateAscProductNameAsc(ownerUserId, startDate, endDate);
        } else {
            snapshots = inventorySnapshotRepository.findAllByOwnerUserIdOrderBySnapshotDateAscIdAsc(ownerUserId);
        }
        if (productId != null) {
            snapshots = snapshots.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .toList();
        }
        List<InventorySnapshotEntity> limited = limit(snapshots, DEFAULT_TOOL_LIMIT);
        List<InventorySnapshotEntity> topSnapshots = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalQuantity = 0D;
        double totalValue = 0D;
        for (InventorySnapshotEntity item : limited) {
            totalQuantity += safeDouble(item.getQuantity());
            totalValue += safeDouble(item.getTotalValue());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条库存快照", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "库存快照概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "快照条数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "库存总量", "value", formatNumber(totalQuantity), "trend_direction", totalQuantity > 0 ? "up" : "flat"),
                    mapOf("label", "库存总值", "value", money(totalValue), "trend_direction", totalValue > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近库存快照",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "库存", "单价", "总值", "快照日期"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = limited.isEmpty()
            ? "当前账号下还没有库存快照数据。"
            : "我查到了最近 " + limited.size() + " 条库存快照，库存总量 "
                + formatNumber(totalQuantity) + "，库存总值 " + money(totalValue) + "。";
        String toolSummary = "最近库存快照 " + limited.size() + " 条，库存总值 " + money(totalValue);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "snapshot_count", limited.size(),
            "total_quantity", formatNumber(totalQuantity),
            "total_value", money(totalValue),
            "query_audit", audit.facts(),
            "recent_snapshots", buildSummaries(topSnapshots)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<InventorySnapshotEntity> snapshots) {
        List<List<Object>> rows = new ArrayList<>(snapshots == null ? 0 : snapshots.size());
        if (snapshots == null) {
            return rows;
        }
        for (int index = 0; index < snapshots.size(); index += 1) {
            InventorySnapshotEntity item = snapshots.get(index);
            rows.add(List.of(
                safeText(item.getProductName(), "-"),
                safeText(item.getProductCode(), "-"),
                formatNumber(safeDouble(item.getQuantity())),
                money(safeDouble(item.getUnitCost())),
                money(safeDouble(item.getTotalValue())),
                String.valueOf(safeLong(item.getSnapshotDate()))
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<InventorySnapshotEntity> snapshots) {
        List<Map<String, Object>> items = new ArrayList<>(snapshots == null ? 0 : snapshots.size());
        if (snapshots == null) {
            return items;
        }
        for (int index = 0; index < snapshots.size(); index += 1) {
            InventorySnapshotEntity item = snapshots.get(index);
            items.add(mapOf(
                "product_name", safeText(item.getProductName(), "-"),
                "product_code", safeText(item.getProductCode(), "-"),
                "quantity", formatNumber(safeDouble(item.getQuantity())),
                "unit_cost", money(safeDouble(item.getUnitCost())),
                "total_value", money(safeDouble(item.getTotalValue())),
                "snapshot_date", safeLong(item.getSnapshotDate())
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.InventoryLedgerEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryLedgerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 库存流水查询工具。
 */
@Component
public class InventoryLedgerLookupTool extends ToolSupport {

    private final InventoryLedgerRepository inventoryLedgerRepository;

    public InventoryLedgerLookupTool(InventoryLedgerRepository inventoryLedgerRepository) {
        this.inventoryLedgerRepository = inventoryLedgerRepository;
    }

    @Override
    public String name() {
        return "inventory_ledger_lookup";
    }

    @Override
    public String displayName() {
        return "库存流水查询";
    }

    @Override
    public String description() {
        return "查询当前账号库存流水、出入库明细与来源类型";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long productId = paramLong(params, "product_id", null);
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        String sourceType = paramString(params, "source_type");
        Map<String, Object> input = mapOf(
            "product_id", productId,
            "start_date", startDate,
            "end_date", endDate,
            "source_type", sourceType == null ? "" : sourceType
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<InventoryLedgerEntity> records;
        if (productId != null) {
            records = inventoryLedgerRepository.findAllByOwnerUserIdAndProductIdOrderByCreatedAtDesc(ownerUserId, productId);
        } else if (startDate != null && endDate != null) {
            records = inventoryLedgerRepository.findAllByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(ownerUserId, startDate, endDate);
        } else {
            records = inventoryLedgerRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(
                ownerUserId,
                PageRequest.of(0, DEFAULT_TOOL_LIMIT * 5)
            ).getContent();
        }
        if (StringUtils.hasText(sourceType)) {
            records = records.stream()
                .filter(item -> sourceType.equalsIgnoreCase(item.getSourceType()))
                .toList();
        }
        List<InventoryLedgerEntity> limited = limit(records, DEFAULT_TOOL_LIMIT);
        List<InventoryLedgerEntity> topRecords = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double inQty = 0D;
        double outQty = 0D;
        for (InventoryLedgerEntity item : limited) {
            double change = safeDouble(item.getQuantityChange());
            if (change >= 0) {
                inQty += change;
            } else {
                outQty += -change;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条库存流水", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "库存流水概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "流水条数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "入库量", "value", formatNumber(inQty), "trend_direction", inQty > 0 ? "up" : "flat"),
                    mapOf("label", "出库量", "value", formatNumber(outQty), "trend_direction", outQty > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近库存流水",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "变动量", "变动后", "来源", "单号"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = limited.isEmpty()
            ? "当前账号下还没有库存流水数据。"
            : "我查到了最近 " + limited.size() + " 条库存流水，入库量 "
                + formatNumber(inQty) + "，出库量 " + formatNumber(outQty) + "。";
        String toolSummary = "最近库存流水 " + limited.size() + " 条，入库 " + formatNumber(inQty) + "，出库 " + formatNumber(outQty);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "ledger_count", limited.size(),
            "in_quantity", formatNumber(inQty),
            "out_quantity", formatNumber(outQty),
            "query_audit", audit.facts(),
            "recent_records", buildSummaries(topRecords)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<InventoryLedgerEntity> records) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (int index = 0; index < records.size(); index += 1) {
            InventoryLedgerEntity item = records.get(index);
            rows.add(List.of(
                safeText(item.getProductName(), "-"),
                safeText(item.getProductCode(), "-"),
                formatNumber(safeDouble(item.getQuantityChange())),
                formatNumber(safeDouble(item.getQuantityAfter())),
                safeText(item.getSourceType(), "-"),
                safeText(item.getSourceNo(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<InventoryLedgerEntity> records) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (int index = 0; index < records.size(); index += 1) {
            InventoryLedgerEntity item = records.get(index);
            items.add(mapOf(
                "product_name", safeText(item.getProductName(), "-"),
                "product_code", safeText(item.getProductCode(), "-"),
                "quantity_change", formatNumber(safeDouble(item.getQuantityChange())),
                "quantity_after", formatNumber(safeDouble(item.getQuantityAfter())),
                "source_type", safeText(item.getSourceType(), "-"),
                "source_no", safeText(item.getSourceNo(), "-")
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

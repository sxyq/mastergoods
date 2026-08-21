package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 采购入库查询工具。
 */
@Component
public class PurchaseReceiptLookupTool extends ToolSupport {

    private final PurchaseReceiptRepository purchaseReceiptRepository;

    public PurchaseReceiptLookupTool(PurchaseReceiptRepository purchaseReceiptRepository) {
        this.purchaseReceiptRepository = purchaseReceiptRepository;
    }

    @Override
    public String name() {
        return "purchase_receipt_lookup";
    }

    @Override
    public String displayName() {
        return "采购入库查询";
    }

    @Override
    public String description() {
        return "查询当前账号采购入库单、到货明细与状态";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "采购入库单关键词，可选");
        addIntegerProperty(schema, "status", "采购入库单状态，可选");
        addIntegerProperty(schema, "purchase_order_id", "采购单 ID，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = normalizedKeyword(paramString(params, "keyword"));
        Integer status = paramInt(params, "status", null);
        Long purchaseOrderId = paramLong(params, "purchase_order_id", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword,
            "status", status,
            "purchase_order_id", purchaseOrderId
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PurchaseReceiptEntity> receipts;
        if (purchaseOrderId != null) {
            receipts = purchaseReceiptRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, purchaseOrderId);
        } else {
            receipts = purchaseReceiptRepository.search(ownerUserId, keyword, status);
        }
        List<PurchaseReceiptEntity> limited = limit(receipts, DEFAULT_TOOL_LIMIT);
        List<PurchaseReceiptEntity> topReceipts = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        for (PurchaseReceiptEntity item : limited) {
            totalAmount += safeDouble(item.getTotalAmount());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条采购入库单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "采购入库概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近入库单", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "入库总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近采购入库单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "供应商", "总额", "状态"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "最近采购入库单 " + limited.size() + " 条，入库总额 " + money(totalAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "receipt_count", limited.size(),
            "total_amount", money(totalAmount),
            "query_audit", audit.facts(),
            "recent_receipts", buildSummaries(topReceipts)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private String normalizedKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : "";
    }

    private List<List<Object>> buildRows(List<PurchaseReceiptEntity> receipts) {
        List<List<Object>> rows = new ArrayList<>(receipts == null ? 0 : receipts.size());
        if (receipts == null) {
            return rows;
        }
        for (int index = 0; index < receipts.size(); index += 1) {
            PurchaseReceiptEntity item = receipts.get(index);
            rows.add(List.of(
                safeText(item.getReceiptNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                statusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<PurchaseReceiptEntity> receipts) {
        List<Map<String, Object>> items = new ArrayList<>(receipts == null ? 0 : receipts.size());
        if (receipts == null) {
            return items;
        }
        for (int index = 0; index < receipts.size(); index += 1) {
            PurchaseReceiptEntity item = receipts.get(index);
            items.add(mapOf(
                "receipt_no", safeText(item.getReceiptNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "status", statusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已确认";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 采购退货查询工具。
 */
@Component
public class PurchaseReturnLookupTool extends ToolSupport {

    private final PurchaseReturnRepository purchaseReturnRepository;

    public PurchaseReturnLookupTool(PurchaseReturnRepository purchaseReturnRepository) {
        this.purchaseReturnRepository = purchaseReturnRepository;
    }

    @Override
    public String name() {
        return "purchase_return_lookup";
    }

    @Override
    public String displayName() {
        return "采购退货查询";
    }

    @Override
    public String description() {
        return "查询当前账号采购退货单、退货明细与状态";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "采购退货单关键词，可选");
        addIntegerProperty(schema, "status", "采购退货单状态，可选");
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

        List<PurchaseReturnEntity> returns;
        if (purchaseOrderId != null) {
            returns = purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, purchaseOrderId);
        } else {
            returns = purchaseReturnRepository.search(ownerUserId, keyword, status);
        }
        List<PurchaseReturnEntity> limited = limit(returns, DEFAULT_TOOL_LIMIT);
        List<PurchaseReturnEntity> topReturns = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        double refundAmount = 0D;
        for (PurchaseReturnEntity item : limited) {
            totalAmount += safeDouble(item.getTotalAmount());
            refundAmount += safeDouble(item.getRefundAmount());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条采购退货单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "采购退货概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近退货单", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "退货总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已退款额", "value", money(refundAmount), "trend_direction", refundAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近采购退货单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "供应商", "总额", "退款额", "状态"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "最近采购退货单 " + limited.size() + " 条，退货总额 " + money(totalAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "return_count", limited.size(),
            "total_amount", money(totalAmount),
            "refund_amount", money(refundAmount),
            "query_audit", audit.facts(),
            "recent_returns", buildSummaries(topReturns)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private String normalizedKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : "";
    }

    private List<List<Object>> buildRows(List<PurchaseReturnEntity> returns) {
        List<List<Object>> rows = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return rows;
        }
        for (int index = 0; index < returns.size(); index += 1) {
            PurchaseReturnEntity item = returns.get(index);
            rows.add(List.of(
                safeText(item.getReturnNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getRefundAmount())),
                statusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<PurchaseReturnEntity> returns) {
        List<Map<String, Object>> items = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return items;
        }
        for (int index = 0; index < returns.size(); index += 1) {
            PurchaseReturnEntity item = returns.get(index);
            items.add(mapOf(
                "return_no", safeText(item.getReturnNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "refund_amount", money(safeDouble(item.getRefundAmount())),
                "status", statusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已确认";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

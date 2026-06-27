package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 采购单查询工具，迁移自 V2AgentAiService.buildPurchaseOrderResponse。
 */
@Component
public class PurchaseOrderLookupTool extends ToolSupport {

    private final PurchaseOrderRepository purchaseOrderRepository;

    public PurchaseOrderLookupTool(PurchaseOrderRepository purchaseOrderRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Override
    public String name() {
        return "purchase_order_lookup";
    }

    @Override
    public String displayName() {
        return "采购单查询";
    }

    @Override
    public String description() {
        return "查询当前账号采购单、供应商采购与到货情况";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        Integer status = paramInt(params, "status", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PurchaseOrderEntity> recentOrders = purchaseOrderRepository.search(
            ownerUserId,
            keyword,
            status,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<PurchaseOrderEntity> topOrders = limit(recentOrders, 5);
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        double receivedAmount = 0D;
        for (PurchaseOrderEntity item : recentOrders) {
            totalAmount += safeDouble(item.getTotalAmount());
            receivedAmount += safeDouble(item.getReceivedAmount());
        }
        emitToolCompleted(ctx, name(), "命中 " + recentOrders.size() + " 条采购单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "采购单概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近采购单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询采购额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "查询已到货", "value", money(receivedAmount), "trend_direction", receivedAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近采购单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "供应商", "总额", "已付", "状态"),
                "rows", buildPurchaseOrderRows(recentOrders),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有采购单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条采购单，查询采购额 "
                + money(totalAmount) + "，查询已到货金额 " + money(receivedAmount) + "。";
        String toolSummary = "最近采购单 " + recentOrders.size() + " 条，查询采购额 " + money(totalAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "order_count", recentOrders.size(),
            "recent_total_amount", money(totalAmount),
            "recent_received_amount", money(receivedAmount),
            "query_audit", audit.facts(),
            "recent_orders", buildPurchaseOrderSummaries(topOrders)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildPurchaseOrderRows(List<PurchaseOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PurchaseOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getPaidAmount())),
                purchaseOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildPurchaseOrderSummaries(List<PurchaseOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PurchaseOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "paid_amount", money(safeDouble(item.getPaidAmount())),
                "status", purchaseOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String purchaseOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已下单";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

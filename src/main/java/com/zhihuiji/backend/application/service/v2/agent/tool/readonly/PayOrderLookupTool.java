package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 付款单查询工具，迁移自 V2AgentAiService.buildPayOrderResponse。
 */
@Component
public class PayOrderLookupTool extends ToolSupport {

    private final PayOrderRepository payOrderRepository;

    public PayOrderLookupTool(PayOrderRepository payOrderRepository) {
        this.payOrderRepository = payOrderRepository;
    }

    @Override
    public String name() {
        return "pay_order_lookup";
    }

    @Override
    public String displayName() {
        return "付款单查询";
    }

    @Override
    public String description() {
        return "查询当前账号付款单、付款状态与金额";
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
        Long createdAfter = paramLong(params, "created_after", null);
        Long createdBefore = paramLong(params, "created_before", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status,
            "created_after", createdAfter,
            "created_before", createdBefore
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PayOrderEntity> recentOrders = payOrderRepository.search(
            ownerUserId,
            keyword,
            status,
            createdAfter,
            createdBefore,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<PayOrderEntity> topOrders = limit(recentOrders, 5);
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        long pendingCount = 0L;
        for (PayOrderEntity item : recentOrders) {
            totalAmount += safeDouble(item.getAmount());
            if (item.getStatus() != null && item.getStatus() == 0) {
                pendingCount++;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + recentOrders.size() + " 条付款单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "付款单概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近付款单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询付款额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "待付款单", "value", String.valueOf(pendingCount), "trend_direction", pendingCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近付款单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "供应商", "金额", "方式", "状态"),
                "rows", buildPayOrderRows(recentOrders),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有付款单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条付款单，查询付款额 "
                + money(totalAmount) + "，其中待付款单 " + pendingCount + " 条。";
        String toolSummary = "最近付款单 " + recentOrders.size() + " 条，待付款 " + pendingCount + " 条";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "pay_order_count", recentOrders.size(),
            "recent_total_amount", money(totalAmount),
            "pending_count", pendingCount,
            "query_audit", audit.facts(),
            "recent_orders", buildPayOrderSummaries(topOrders)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildPayOrderRows(List<PayOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PayOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getAmount())),
                paymentMethodLabel(item.getMethod()),
                payOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildPayOrderSummaries(List<PayOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PayOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "amount", money(safeDouble(item.getAmount())),
                "method", paymentMethodLabel(item.getMethod()),
                "status", payOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String payOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String paymentMethodLabel(Integer method) {
        return switch (method == null ? -1 : method) {
            case 0 -> "现金";
            case 1 -> "微信";
            case 2 -> "支付宝";
            case 3 -> "银行卡";
            default -> "其他";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

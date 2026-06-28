package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 销售单查询工具，迁移自 V2AgentAiService.buildSaleOrderResponse。
 */
@Component
public class SaleOrderLookupTool extends ToolSupport {

    private final SaleOrderRepository saleOrderRepository;

    public SaleOrderLookupTool(SaleOrderRepository saleOrderRepository) {
        this.saleOrderRepository = saleOrderRepository;
    }

    @Override
    public String name() {
        return "sale_order_lookup";
    }

    @Override
    public String displayName() {
        return "销售单查询";
    }

    @Override
    public String description() {
        return "查询当前账号销售单、客户订单与收款情况";
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
        Double minTotal = paramDouble(params, "min_total", null);
        Double maxTotal = paramDouble(params, "max_total", null);
        Long createdAfter = paramLong(params, "created_after", null);
        Long createdBefore = paramLong(params, "created_before", null);
        String productKeyword = paramString(params, "product_keyword");
        Integer paymentStatus = paramInt(params, "payment_status", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status,
            "min_total", minTotal,
            "max_total", maxTotal,
            "created_after", createdAfter,
            "created_before", createdBefore,
            "product_keyword", productKeyword == null ? "" : productKeyword,
            "payment_status", paymentStatus
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<SaleOrderEntity> recentOrders = saleOrderRepository.search(
            ownerUserId,
            keyword,
            status,
            minTotal,
            maxTotal,
            createdAfter,
            createdBefore,
            productKeyword,
            paymentStatus,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<SaleOrderEntity> topOrders = limit(recentOrders, 5);
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        long unpaidCount = 0L;
        double recentTotal = 0D;
        double recentPaid = 0D;
        for (SaleOrderEntity item : recentOrders) {
            double totalAmount = safeDouble(item.getTotalAmount());
            double paidAmount = safeDouble(item.getPaidAmount());
            recentTotal += totalAmount;
            recentPaid += paidAmount;
            if (paidAmount + 0.000001 < totalAmount) {
                unpaidCount++;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + recentOrders.size() + " 条销售单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "销售单概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近销售单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询销售额", "value", money(recentTotal), "trend_direction", recentTotal > 0 ? "up" : "flat"),
                    mapOf("label", "未收清单", "value", String.valueOf(unpaidCount), "trend_direction", unpaidCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近销售单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "客户", "总额", "已收", "状态"),
                "rows", buildSaleOrderRows(recentOrders),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        if (recentOrders.isEmpty() && hasMeaningfulFilter(keyword, status, minTotal, maxTotal, createdAfter, createdBefore, productKeyword, paymentStatus)) {
            return ToolResult.emptyInsufficient("按当前条件未匹配到销售单，建议放宽筛选后重试");
        }
        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有销售单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条销售单，查询销售额 "
                + money(recentTotal) + "，已收 " + money(recentPaid) + "，还有 "
                + unpaidCount + " 条未收清。";
        String toolSummary = "最近销售单 " + recentOrders.size() + " 条，未收清 " + unpaidCount + " 条";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "order_count", recentOrders.size(),
            "recent_total_amount", money(recentTotal),
            "recent_paid_amount", money(recentPaid),
            "unpaid_count", unpaidCount,
            "query_audit", audit.facts(),
            "recent_orders", buildSaleOrderSummaries(topOrders)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildSaleOrderRows(List<SaleOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            SaleOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getCustomerName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getPaidAmount())),
                saleOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSaleOrderSummaries(List<SaleOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            SaleOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "customer_name", safeText(item.getCustomerName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "paid_amount", money(safeDouble(item.getPaidAmount())),
                "status", saleOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String saleOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean hasMeaningfulFilter(String keyword,
                                        Integer status,
                                        Double minTotal,
                                        Double maxTotal,
                                        Long createdAfter,
                                        Long createdBefore,
                                        String productKeyword,
                                        Integer paymentStatus) {
        return StringUtils.hasText(keyword)
            || status != null
            || minTotal != null
            || maxTotal != null
            || createdAfter != null
            || createdBefore != null
            || StringUtils.hasText(productKeyword)
            || paymentStatus != null;
    }
}

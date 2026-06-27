package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 经营概览查询工具，迁移自 V2AgentAiService.buildOverviewResponse。
 */
@Component
public class SalesOverviewLookupTool extends ToolSupport {

    private static final int CANCELLED_SALE_ORDER_STATUS = 2;
    private static final long DAY_BUCKET_MILLIS = 24L * 60 * 60 * 1000;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final ZoneId CHART_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final int OVERVIEW_SIGNAL_LIMIT = 5;

    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public SalesOverviewLookupTool(SaleOrderRepository saleOrderRepository,
                                   ProductRepository productRepository,
                                   CustomerRepository customerRepository) {
        this.saleOrderRepository = saleOrderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "sales_overview_lookup";
    }

    @Override
    public String displayName() {
        return "经营概览查询";
    }

    @Override
    public String description() {
        return "查询当前账号近7天销售、回款、经营概览";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        int windowDays = Math.max(1, paramInt(params, "window_days", 7));
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        long now = System.currentTimeMillis();
        long endAt = endDate != null ? endDate : now;
        long startAt = startDate != null ? startDate : endAt - windowDays * DAY_MILLIS;
        Map<String, Object> input = mapOf(
            "window_days", windowDays,
            "start_date", startAt,
            "end_date", endAt,
            "rank_limit", OVERVIEW_SIGNAL_LIMIT,
            "low_stock_limit", OVERVIEW_SIGNAL_LIMIT
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        double salesAmount = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, startAt, endAt));
        double paidAmount = safeDouble(saleOrderRepository.sumPaidAmountBetween(ownerUserId, startAt, endAt));
        long salesCount = safeLong(saleOrderRepository.countNonCancelledBetween(ownerUserId, startAt, endAt));
        List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, OVERVIEW_SIGNAL_LIMIT));
        List<String> lowStockNames = new ArrayList<>(Math.min(lowStockProducts.size(), 3));
        for (int index = 0; index < lowStockProducts.size() && index < 3; index += 1) {
            lowStockNames.add(lowStockProducts.get(index).getName());
        }
        double receivable = safeDouble(customerRepository.sumPositiveBalance(ownerUserId));
        List<Object[]> customerSales = saleOrderRepository.customerSales(
            ownerUserId,
            startAt,
            endAt,
            CANCELLED_SALE_ORDER_STATUS,
            PageRequest.of(0, OVERVIEW_SIGNAL_LIMIT)
        );
        List<Map<String, Object>> customerSalesRank = buildCustomerSalesRank(customerSales);
        audit.markReturned(Math.max(lowStockProducts.size(), customerSales.size()));
        emitToolCompleted(ctx, name(), "已汇总近" + windowDays + "天销售、应收和库存信号", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "经营概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "近" + windowDays + "天销售额", "value", money(salesAmount), "trend_direction", salesAmount > 0 ? "up" : "flat"),
                    mapOf("label", "近" + windowDays + "天回款", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat"),
                    mapOf("label", "销售单数", "value", String.valueOf(salesCount), "trend_direction", salesCount > 0 ? "up" : "flat"),
                    mapOf("label", "当前应收", "value", money(receivable), "trend_direction", receivable > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "客户销售排行",
            toJsonNode(ctx, mapOf("items", customerSalesRank))
        );
        V2AgentDtos.ResultBlockDto trendBlock = buildSalesTrendBlock(ctx, ownerUserId, startAt, endAt);
        V2AgentDtos.ResultBlockDto amountBlock = new V2AgentDtos.ResultBlockDto(
            "bar_chart",
            "经营金额对比",
            toJsonNode(ctx, mapOf(
                "title", "经营金额对比",
                "labels", List.of("销售额", "回款", "当前应收"),
                "series", List.of(mapOf(
                    "name", "金额",
                    "data", List.of(salesAmount, paidAmount, receivable),
                    "color", "#005BBF"
                ))
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "经营提醒",
            toJsonNode(ctx, mapOf(
                "level", lowStockProducts.isEmpty() ? "low" : "medium",
                "title", lowStockProducts.isEmpty() ? "暂无显著库存风险" : "存在低库存商品",
                "description", lowStockProducts.isEmpty()
                    ? "近" + windowDays + "天经营数据已汇总，当前库存预警不明显。"
                    : "建议同步关注补货与回款，避免销售增长带来缺货。",
                "affected_items", lowStockNames,
                "suggested_action", lowStockProducts.isEmpty() ? "继续观察趋势" : "优先处理低库存商品并跟进重点客户回款"
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, trendBlock, amountBlock, rankBlock, riskBlock);
        String answer = "我已经把当前账号近" + windowDays + "天的销售、回款、应收和库存风险汇总好了，可以先从重点客户回款和低库存商品两条线并行处理。";
        String toolSummary = "近" + windowDays + "天销售 " + salesCount + " 笔，销售额 " + money(salesAmount) + "，回款 " + money(paidAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "window_days", windowDays,
            "sales_amount", money(salesAmount),
            "paid_amount", money(paidAmount),
            "sales_count", salesCount,
            "current_receivable", money(receivable),
            "low_stock_count", lowStockProducts.size(),
            "query_audit", audit.facts(),
            "top_customer_sales", customerSalesRank
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private V2AgentDtos.ResultBlockDto buildSalesTrendBlock(ToolContext ctx, Long ownerUserId, long startAt, long endAt) {
        LocalDate endDate = Instant.ofEpochMilli(endAt).atZone(CHART_ZONE).toLocalDate();
        long chartStartAt = endDate.minusDays(6).atStartOfDay(CHART_ZONE).toInstant().toEpochMilli();
        List<Object[]> trendRows = saleOrderRepository.salesTrendBuckets(
            ownerUserId,
            chartStartAt,
            endAt,
            DAY_BUCKET_MILLIS,
            CANCELLED_SALE_ORDER_STATUS
        );
        Map<Long, Object[]> rowsByBucket = new LinkedHashMap<>();
        for (Object[] row : trendRows) {
            rowsByBucket.put(safeLong(row[0]), row);
        }

        List<String> labels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Double> paidData = new ArrayList<>();

        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = endDate.minusDays(offset);
            labels.add(date.format(MONTH_DAY_FORMATTER));
            Object[] row = rowsByBucket.get((long) (6 - offset));
            salesData.add(row == null ? 0D : safeDouble(row[1]));
            paidData.add(row == null ? 0D : safeDouble(row[3]));
        }

        return new V2AgentDtos.ResultBlockDto(
            "line_chart",
            "近7天销售趋势",
            toJsonNode(ctx, mapOf(
                "title", "近7天销售趋势",
                "labels", labels,
                "series", List.of(
                    mapOf("name", "销售额", "data", salesData, "color", "#005BBF"),
                    mapOf("name", "回款", "data", paidData, "color", "#34A853")
                )
            ))
        );
    }

    private List<Map<String, Object>> buildCustomerSalesRank(List<Object[]> rows) {
        List<Map<String, Object>> items = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows == null) {
            return items;
        }
        for (int index = 0; index < rows.size(); index++) {
            Object[] row = rows.get(index);
            String name = row[1] == null ? "未命名客户" : String.valueOf(row[1]);
            double amount = row[3] instanceof Number number ? number.doubleValue() : 0;
            items.add(mapOf(
                "rank", index + 1,
                "name", name,
                "value", money(amount),
                "change_direction", "up"
            ));
        }
        return items;
    }
}

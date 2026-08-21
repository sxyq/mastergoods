package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 销售趋势查询工具，按日/周/月分桶聚合销售趋势并支持环比分析。
 */
@Component
public class SalesTrendLookupTool extends ToolSupport {

    private static final int CANCELLED_SALE_ORDER_STATUS = 2;
    private static final long DAY_BUCKET_MILLIS = 24L * 60 * 60 * 1000;
    private static final long WEEK_BUCKET_MILLIS = 7L * DAY_BUCKET_MILLIS;
    private static final long MONTH_BUCKET_MILLIS = 30L * DAY_BUCKET_MILLIS;
    private static final int DEFAULT_WINDOW_DAYS = 7;
    private static final int MAX_BUCKETS = 120;
    private static final ZoneId CHART_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BUCKET_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private final SaleOrderRepository saleOrderRepository;
    private final ObjectMapper objectMapper;

    public SalesTrendLookupTool(SaleOrderRepository saleOrderRepository, ObjectMapper objectMapper) {
        this.saleOrderRepository = saleOrderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "sales_trend_lookup";
    }

    @Override
    public String displayName() {
        return "销售趋势查询";
    }

    @Override
    public String description() {
        return "查询当前账号销售趋势、按日/周/月分桶聚合与环比分析";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("window_days")
            .put("type", "integer")
            .put("description", "统计窗口天数；用户说近一年时使用 365，默认 7")
            .put("minimum", 1)
            .put("maximum", 365);
        properties.putObject("bucket")
            .put("type", "string")
            .put("description", "分桶粒度")
            .putArray("enum")
            .add("")
            .add("day")
            .add("week")
            .add("month");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Integer windowDays = paramInt(params, "window_days", DEFAULT_WINDOW_DAYS);
        String bucket = paramString(params, "bucket");
        long bucketMillis = normalizeBucketMillis(bucket);
        int safeWindowDays = windowDays == null || windowDays <= 0 ? DEFAULT_WINDOW_DAYS : windowDays;
        Map<String, Object> input = mapOf(
            "window_days", safeWindowDays,
            "bucket", bucket == null ? "" : bucket
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        long endAt = System.currentTimeMillis();
        long startAt = endAt - (safeWindowDays * DAY_BUCKET_MILLIS);
        int bucketCount = Math.max(1, Math.min(MAX_BUCKETS, (int) ((endAt - startAt) / bucketMillis) + 1));

        List<Object[]> trendRows = saleOrderRepository.salesTrendBuckets(
            ownerUserId,
            startAt,
            endAt,
            bucketMillis,
            CANCELLED_SALE_ORDER_STATUS
        );
        Map<Long, Object[]> rowsByBucket = new LinkedHashMap<>();
        for (Object[] row : trendRows) {
            rowsByBucket.put(safeLong(row[0]), row);
        }
        audit.markLimitedResult(bucketCount, bucketCount);

        List<String> labels = new ArrayList<>(bucketCount);
        List<Double> salesData = new ArrayList<>(bucketCount);
        List<Double> paidData = new ArrayList<>(bucketCount);
        List<Long> orderCountData = new ArrayList<>(bucketCount);
        double totalSales = 0D;
        double totalPaid = 0D;
        long totalOrders = 0L;
        for (int index = 0; index < bucketCount; index++) {
            long bucketStart = startAt + (bucketMillis * index);
            labels.add(Instant.ofEpochMilli(bucketStart).atZone(CHART_ZONE).toLocalDate().format(BUCKET_LABEL_FORMATTER));
            Object[] row = rowsByBucket.get((long) index);
            double sales = row == null ? 0D : safeDouble(row[1]);
            long orders = row == null ? 0L : safeLong(row[2]);
            double paid = row == null ? 0D : safeDouble(row[3]);
            salesData.add(sales);
            paidData.add(paid);
            orderCountData.add(orders);
            totalSales += sales;
            totalPaid += paid;
            totalOrders += orders;
        }
        emitToolCompleted(ctx, name(), "聚合 " + bucketCount + " 个分桶", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "销售趋势概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "分桶数", "value", String.valueOf(bucketCount), "trend_direction", "flat"),
                    mapOf("label", "区间销售额", "value", money(totalSales), "trend_direction", totalSales > 0 ? "up" : "flat"),
                    mapOf("label", "区间订单数", "value", String.valueOf(totalOrders), "trend_direction", totalOrders > 0 ? "up" : "flat"),
                    mapOf("label", "区间回款", "value", money(totalPaid), "trend_direction", totalPaid > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto lineBlock = new V2AgentDtos.ResultBlockDto(
            "line_chart",
            "销售趋势",
            toJsonNode(ctx, mapOf(
                "title", "销售趋势",
                "labels", labels,
                "series", List.of(
                    mapOf("name", "销售额", "data", salesData, "color", "#005BBF"),
                    mapOf("name", "回款", "data", paidData, "color", "#34A853")
                )
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, lineBlock);
        String toolSummary = "销售趋势 " + bucketCount + " 桶，销售额 " + money(totalSales) + "，订单 " + totalOrders;
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "bucket_count", bucketCount,
            "window_days", safeWindowDays,
            "bucket", bucket == null ? "day" : bucket,
            "total_sales_amount", money(totalSales),
            "total_paid_amount", money(totalPaid),
            "total_order_count", totalOrders,
            "query_audit", audit.facts(),
            "labels", labels,
            "sales_series", salesData,
            "paid_series", paidData,
            "order_count_series", orderCountData
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private long normalizeBucketMillis(String bucket) {
        if (bucket == null) {
            return DAY_BUCKET_MILLIS;
        }
        return switch (bucket.toLowerCase()) {
            case "week", "weekly" -> WEEK_BUCKET_MILLIS;
            case "month", "monthly" -> MONTH_BUCKET_MILLIS;
            default -> DAY_BUCKET_MILLIS;
        };
    }

}

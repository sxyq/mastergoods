package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 异常预警查询工具。
 *
 * <p>扫描近7天数据，识别销售额骤降、库存缺货、客户欠款超额等异常并给出建议动作。
 */
@Component
public class AnomalyAlertLookupTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int SCAN_DAYS = 7;
    private static final double SALES_DROP_THRESHOLD = 0.5D;
    private static final double OVERDUE_BALANCE_THRESHOLD = 1000D;

    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public AnomalyAlertLookupTool(SaleOrderRepository saleOrderRepository,
                                  ProductRepository productRepository,
                                  CustomerRepository customerRepository,
                                  ObjectMapper objectMapper) {
        this.saleOrderRepository = saleOrderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "anomaly_alert_lookup";
    }

    @Override
    public String displayName() {
        return "异常预警查询";
    }

    @Override
    public String description() {
        return "扫描近7天销售额骤降、库存缺货、客户欠款超额等异常";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode alertType = properties.putObject("alert_type");
        alertType.put("type", "string");
        alertType.put("description", "预警类型：sales_drop/stock_out/overdue/all，默认 all");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String alertType = paramString(params, "alert_type");
        String type = alertType == null ? "all" : alertType.toLowerCase();
        Map<String, Object> input = mapOf("alert_type", type);
        ToolAudit audit = startAudit(ctx, name(), input);

        long now = System.currentTimeMillis();
        long recentStart = now - (long) SCAN_DAYS * DAY_MILLIS;
        long previousStart = now - (long) (SCAN_DAYS * 2) * DAY_MILLIS;

        List<Map<String, Object>> alerts = new ArrayList<>();
        boolean checkSalesDrop = "all".equals(type) || "sales_drop".equals(type);
        boolean checkStockOut = "all".equals(type) || "stock_out".equals(type);
        boolean checkOverdue = "all".equals(type) || "overdue".equals(type);

        if (checkSalesDrop) {
            double recentSales = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, recentStart, now));
            double previousSales = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, previousStart, recentStart));
            if (previousSales > 0 && recentSales < previousSales * (1 - SALES_DROP_THRESHOLD)) {
                double dropRate = previousSales > 0 ? (previousSales - recentSales) / previousSales : 0D;
                alerts.add(mapOf(
                    "alert_type", "sales_drop",
                    "severity", "high",
                    "title", "销售额骤降",
                    "description", "近" + SCAN_DAYS + "天销售额 " + money(recentSales)
                        + " 较上一周期 " + money(previousSales) + " 下降 " + formatNumber(dropRate * 100) + "%",
                    "affected_entity", "sales",
                    "suggested_action", "排查重点客户流失与商品动销，必要时启动促销或回款跟进"
                ));
            }
        }
        if (checkStockOut) {
            List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(
                ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
            List<String> stockOutNames = new ArrayList<>();
            for (ProductEntity item : lowStockProducts) {
                if (safeDouble(item.getStock()) <= 0) {
                    stockOutNames.add(item.getName());
                }
            }
            if (!stockOutNames.isEmpty()) {
                alerts.add(mapOf(
                    "alert_type", "stock_out",
                    "severity", "high",
                    "title", "库存缺货",
                    "description", "检测到 " + stockOutNames.size() + " 个商品库存为 0，可能影响销售",
                    "affected_entity", "inventory",
                    "affected_items", stockOutNames,
                    "suggested_action", "立即补货或调整销售策略，优先保障热销商品供应"
                ));
            }
        }
        if (checkOverdue) {
            List<CustomerEntity> overdueCustomers = customerRepository
                .findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(
                    ownerUserId, OVERDUE_BALANCE_THRESHOLD, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
            if (!overdueCustomers.isEmpty()) {
                double totalOverdue = 0D;
                List<String> overdueNames = new ArrayList<>(overdueCustomers.size());
                for (CustomerEntity c : overdueCustomers) {
                    totalOverdue += safeDouble(c.getBalance());
                    overdueNames.add(c.getName());
                }
                alerts.add(mapOf(
                    "alert_type", "overdue",
                    "severity", "medium",
                    "title", "客户欠款超额",
                    "description", "检测到 " + overdueCustomers.size() + " 个客户欠款超过 "
                        + money(OVERDUE_BALANCE_THRESHOLD) + "，合计 " + money(totalOverdue),
                    "affected_entity", "customer",
                    "affected_items", overdueNames,
                    "suggested_action", "跟进重点客户回款，必要时调整授信额度或对账周期"
                ));
            }
        }

        audit.markReturned(alerts.size());
        emitToolCompleted(ctx, name(), "命中 " + alerts.size() + " 条异常预警", audit);

        long highCount = 0L;
        for (Map<String, Object> alert : alerts) {
            if ("high".equals(alert.get("severity"))) {
                highCount += 1;
            }
        }
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "异常预警概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "预警总数", "value", String.valueOf(alerts.size()), "trend_direction", alerts.isEmpty() ? "flat" : "up"),
                    mapOf("label", "高严重度", "value", String.valueOf(highCount), "trend_direction", highCount > 0 ? "up" : "flat"),
                    mapOf("label", "扫描周期", "value", "近" + SCAN_DAYS + "天", "trend_direction", "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto alertBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "异常预警明细",
            toJsonNode(ctx, mapOf(
                "level", highCount > 0 ? "high" : (alerts.isEmpty() ? "low" : "medium"),
                "title", alerts.isEmpty() ? "未检测到异常" : "检测到 " + alerts.size() + " 条异常",
                "description", alerts.isEmpty()
                    ? "近" + SCAN_DAYS + "天经营数据正常，未发现显著异常。"
                    : "建议尽快处理高严重度异常，避免影响经营。",
                "alerts", alerts,
                "suggested_action", alerts.isEmpty() ? "保持常规监控" : "按预警类型逐项处理"
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, alertBlock);
        String toolSummary = "异常预警 " + alerts.size() + " 条，高严重度 " + highCount + " 条";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "alert_type", type,
            "scan_days", SCAN_DAYS,
            "alert_count", alerts.size(),
            "high_severity_count", highCount,
            "alerts", alerts,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }
}

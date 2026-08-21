package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 多维度交叉分析工具。
 *
 * <p>同时查询销售、采购、库存汇总，按维度输出 KPI 与对比图表数据。
 */
@Component
public class CrossAnalysisLookupTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int CANCELLED_SALE_ORDER_STATUS = 2;

    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CrossAnalysisLookupTool(SaleOrderRepository saleOrderRepository,
                                   PurchaseOrderRepository purchaseOrderRepository,
                                   ProductRepository productRepository,
                                   ObjectMapper objectMapper) {
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "cross_analysis_lookup";
    }

    @Override
    public String displayName() {
        return "多维度交叉分析";
    }

    @Override
    public String description() {
        return "按销售、采购、库存维度做交叉汇总与对比分析";
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
        ObjectNode dimension = properties.putObject("dimension");
        dimension.put("type", "string");
        dimension.put("description", "分析维度：sales/purchase/inventory/all，默认 all");
        ObjectNode days = properties.putObject("days");
        days.put("type", "integer");
        days.put("description", "分析时间窗口天数，默认 30");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String dimension = paramString(params, "dimension");
        int days = Math.max(1, paramInt(params, "days", 30));
        String dim = dimension == null ? "all" : dimension.toLowerCase();
        Map<String, Object> input = mapOf(
            "dimension", dim,
            "days", days
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        long now = System.currentTimeMillis();
        long startAt = now - (long) days * DAY_MILLIS;

        boolean querySales = "all".equals(dim) || "sales".equals(dim);
        boolean queryPurchase = "all".equals(dim) || "purchase".equals(dim);
        boolean queryInventory = "all".equals(dim) || "inventory".equals(dim);

        List<Map<String, Object>> kpis = new ArrayList<>();
        List<String> compareLabels = new ArrayList<>();
        List<Double> compareAmounts = new ArrayList<>();

        if (querySales) {
            double salesAmount = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, startAt, now));
            double paidAmount = safeDouble(saleOrderRepository.sumPaidAmountBetween(ownerUserId, startAt, now));
            long salesCount = safeLong(saleOrderRepository.countNonCancelledBetween(ownerUserId, startAt, now));
            kpis.add(mapOf("label", "销售额", "value", money(salesAmount), "trend_direction", salesAmount > 0 ? "up" : "flat"));
            kpis.add(mapOf("label", "销售回款", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat"));
            kpis.add(mapOf("label", "销售单数", "value", String.valueOf(salesCount), "trend_direction", salesCount > 0 ? "up" : "flat"));
            compareLabels.add("销售额");
            compareAmounts.add(salesAmount);
            compareLabels.add("销售回款");
            compareAmounts.add(paidAmount);
        }
        if (queryPurchase) {
            List<PurchaseOrderEntity> purchaseOrders = purchaseOrderRepository
                .findByOwnerUserIdAndCreatedAtBetween(ownerUserId, startAt, now);
            double purchaseAmount = 0D;
            double purchasePaid = 0D;
            for (PurchaseOrderEntity order : purchaseOrders) {
                purchaseAmount += safeDouble(order.getTotalAmount());
                purchasePaid += safeDouble(order.getPaidAmount());
            }
            kpis.add(mapOf("label", "采购额", "value", money(purchaseAmount), "trend_direction", purchaseAmount > 0 ? "up" : "flat"));
            kpis.add(mapOf("label", "采购已付", "value", money(purchasePaid), "trend_direction", purchasePaid > 0 ? "up" : "flat"));
            kpis.add(mapOf("label", "采购单数", "value", String.valueOf(purchaseOrders.size()), "trend_direction", purchaseOrders.isEmpty() ? "flat" : "up"));
            compareLabels.add("采购额");
            compareAmounts.add(purchaseAmount);
            compareLabels.add("采购已付");
            compareAmounts.add(purchasePaid);
        }
        if (queryInventory) {
            double totalStock = safeDouble(productRepository.sumStockByOwnerUserId(ownerUserId));
            long productCount = productRepository.countByOwnerUserId(ownerUserId);
            long lowStockCount = productRepository.countLowStockByOwnerUserId(ownerUserId);
            kpis.add(mapOf("label", "库存总量", "value", formatNumber(totalStock), "trend_direction", "flat"));
            kpis.add(mapOf("label", "商品总数", "value", String.valueOf(productCount), "trend_direction", productCount > 0 ? "up" : "flat"));
            kpis.add(mapOf("label", "低库存数", "value", String.valueOf(lowStockCount), "trend_direction", lowStockCount > 0 ? "down" : "flat"));
        }

        audit.markReturned(kpis.size());
        emitToolCompleted(ctx, name(), "已汇总 " + dim + " 维度 " + kpis.size() + " 项指标", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "交叉分析概览（近" + days + "天）",
            toJsonNode(ctx, mapOf("kpis", kpis))
        );
        V2AgentDtos.ResultBlockDto compareBlock = new V2AgentDtos.ResultBlockDto(
            "bar_chart",
            "金额对比",
            toJsonNode(ctx, mapOf(
                "title", "金额对比",
                "labels", compareLabels,
                "series", List.of(mapOf(
                    "name", "金额",
                    "data", compareAmounts,
                    "color", "#005BBF"
                ))
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, compareBlock);
        String toolSummary = "交叉分析维度 " + dim + "，指标 " + kpis.size() + " 项";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "dimension", dim,
            "days", days,
            "kpi_count", kpis.size(),
            "kpis", kpis,
            "compare_labels", compareLabels,
            "compare_amounts", compareAmounts,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }
}

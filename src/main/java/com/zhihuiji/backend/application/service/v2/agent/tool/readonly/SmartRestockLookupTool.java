package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 智能补货建议查询工具。
 *
 * <p>结合低库存商品与近30天销售信号，给出建议补货量与补货紧急度。
 */
@Component
public class SmartRestockLookupTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int RECENT_DAYS = 30;

    private final ProductRepository productRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ObjectMapper objectMapper;

    public SmartRestockLookupTool(ProductRepository productRepository,
                                  SaleOrderRepository saleOrderRepository,
                                  ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "smart_restock_lookup";
    }

    @Override
    public String displayName() {
        return "智能补货建议";
    }

    @Override
    public String description() {
        return "结合低库存商品与近30天销售给出补货建议量与紧急度";
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
        ObjectNode categoryId = properties.putObject("category_id");
        categoryId.put("type", "integer");
        categoryId.put("description", "商品分类 ID（可选，用于过滤指定分类）");
        ObjectNode limit = properties.putObject("limit");
        limit.put("type", "integer");
        limit.put("description", "返回条数上限，默认 20");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long categoryId = paramLong(params, "category_id", null);
        int queryLimit = Math.min(50, Math.max(1, paramInt(params, "limit", 20)));
        Map<String, Object> input = mapOf(
            "category_id", categoryId,
            "limit", queryLimit
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        long now = System.currentTimeMillis();
        long salesStartAt = now - RECENT_DAYS * DAY_MILLIS;
        double recentSalesAmount = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, salesStartAt, now));
        long recentSalesCount = safeLong(saleOrderRepository.countNonCancelledBetween(ownerUserId, salesStartAt, now));

        List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(
            ownerUserId, PageRequest.of(0, queryLimit));
        List<ProductEntity> filtered = filterByCategory(lowStockProducts, categoryId);
        List<ProductEntity> limited = limit(filtered, queryLimit);
        List<ProductEntity> topProducts = limit(limited, 5);
        audit.markLimitedResult(limited.size(), queryLimit);
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 个待补货商品", audit);

        List<List<Object>> rows = new ArrayList<>(limited.size());
        List<Map<String, Object>> topItems = new ArrayList<>(topProducts.size());
        int highCount = 0;
        for (ProductEntity item : limited) {
            double stock = safeDouble(item.getStock());
            double safeStock = safeDouble(item.getSafeStock());
            double suggested = Math.max(safeStock * 2 - stock, safeStock - stock);
            String urgency = urgencyLabel(stock, safeStock);
            if ("high".equals(urgency)) {
                highCount += 1;
            }
            rows.add(List.of(
                item.getName(),
                item.getCode(),
                formatNumber(stock),
                formatNumber(safeStock),
                formatNumber(Math.max(0D, suggested)),
                urgency
            ));
        }
        for (ProductEntity item : topProducts) {
            double stock = safeDouble(item.getStock());
            double safeStock = safeDouble(item.getSafeStock());
            double suggested = Math.max(safeStock * 2 - stock, safeStock - stock);
            topItems.add(mapOf(
                "name", item.getName(),
                "code", item.getCode(),
                "stock", formatNumber(stock),
                "safe_stock", formatNumber(safeStock),
                "suggested_restock", formatNumber(Math.max(0D, suggested)),
                "urgency", urgencyLabel(stock, safeStock)
            ));
        }

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "补货概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "待补货商品", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "高紧急度", "value", String.valueOf(highCount), "trend_direction", highCount > 0 ? "up" : "flat"),
                    mapOf("label", "近30天销售额", "value", money(recentSalesAmount), "trend_direction", recentSalesAmount > 0 ? "up" : "flat"),
                    mapOf("label", "近30天单数", "value", String.valueOf(recentSalesCount), "trend_direction", recentSalesCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "补货建议列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "当前库存", "安全库存", "建议补货量", "紧急度"),
                "rows", rows,
                "row_count", limited.size()
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "补货提醒",
            toJsonNode(ctx, mapOf(
                "level", highCount > 0 ? "high" : (limited.isEmpty() ? "low" : "medium"),
                "title", limited.isEmpty() ? "暂无补货需求" : "检测到待补货商品",
                "description", limited.isEmpty()
                    ? "当前库存处于健康水平。"
                    : "建议优先处理高紧急度商品，结合近30天销售节奏安排补货。",
                "affected_items", buildAffectedItems(topItems),
                "suggested_action", limited.isEmpty() ? "保持现有补货节奏" : "优先补货高紧急度商品"
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock, riskBlock);
        String answer = limited.isEmpty()
            ? "当前账号下没有需要补货的商品。"
            : "已识别 " + limited.size() + " 个待补货商品，其中 " + highCount + " 个高紧急度，建议优先处理。";
        String toolSummary = "待补货商品 " + limited.size() + " 个，高紧急度 " + highCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "restock_count", limited.size(),
            "high_urgency_count", highCount,
            "recent_sales_amount", money(recentSalesAmount),
            "recent_sales_count", recentSalesCount,
            "query_audit", audit.facts(),
            "top_items", topItems
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<ProductEntity> filterByCategory(List<ProductEntity> products, Long categoryId) {
        if (categoryId == null || products == null || products.isEmpty()) {
            return products == null ? List.of() : products;
        }
        List<ProductEntity> result = new ArrayList<>(products.size());
        for (ProductEntity item : products) {
            if (categoryId.equals(item.getCategoryId())) {
                result.add(item);
            }
        }
        return result;
    }

    private String urgencyLabel(double stock, double safeStock) {
        if (stock <= 0) {
            return "high";
        }
        if (safeStock <= 0) {
            return stock <= 0 ? "high" : "low";
        }
        if (stock < safeStock * 0.5) {
            return "high";
        }
        if (stock < safeStock) {
            return "medium";
        }
        return "low";
    }

    private List<String> buildAffectedItems(List<Map<String, Object>> topItems) {
        List<String> items = new ArrayList<>(topItems == null ? 0 : topItems.size());
        if (topItems == null) {
            return items;
        }
        for (Map<String, Object> item : topItems) {
            items.add(String.valueOf(item.get("name")));
        }
        return items;
    }
}

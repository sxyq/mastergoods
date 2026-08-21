package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品库存全景工具。
 *
 * <p>输出单商品库存健康度：当前库存、安全库存、近 30 天销量、周转天数与建议补货量。
 */
@Component
public class InventoryPanoramaLookupTool extends ToolSupport {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int RECENT_DAYS = 30;

    private final ProductRepository productRepository;
    private final InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ObjectMapper objectMapper;

    public InventoryPanoramaLookupTool(ProductRepository productRepository,
                                       InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository,
                                       SaleOrderItemRepository saleOrderItemRepository,
                                       ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.inventoryMonthlyStatsRepository = inventoryMonthlyStatsRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "inventory_panorama_lookup";
    }

    @Override
    public String displayName() {
        return "商品库存全景";
    }

    @Override
    public String description() {
        return "查询库存现状，按商品输出当前库存、安全库存、近30天销量和周转天数，并附带参考补货量；用户同时明确需要补货建议时，还必须调用 smart_restock_lookup，不能用本工具替代补货建议";
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
        ObjectNode productId = properties.putObject("product_id");
        productId.put("type", "integer");
        productId.put("description", "商品 ID");
        ObjectNode keyword = properties.putObject("keyword");
        keyword.put("type", "string");
        keyword.put("description", "商品名称或编码关键词");
        ObjectNode limit = properties.putObject("limit");
        limit.put("type", "integer");
        limit.put("description", "返回条数上限，默认 10");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long productId = paramLong(params, "product_id", null);
        String keyword = paramString(params, "keyword");
        int queryLimit = Math.min(20, Math.max(1, paramInt(params, "limit", DEFAULT_TOOL_LIMIT)));
        Map<String, Object> input = mapOf(
            "product_id", productId,
            "keyword", keyword == null ? "" : keyword,
            "limit", queryLimit
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductEntity> products = resolveProducts(ownerUserId, productId, keyword, queryLimit);
        List<ProductPanorama> panoramas = buildPanoramas(ownerUserId, products);
        panoramas.sort(Comparator.comparingDouble(ProductPanorama::healthScore).reversed());
        List<ProductPanorama> limited = limit(panoramas, queryLimit);
        ProductPanorama focus = limited.isEmpty() ? null : limited.get(0);
        int healthyCount = 0;
        int riskCount = 0;
        double totalSuggestedRestock = 0D;
        for (ProductPanorama item : limited) {
            totalSuggestedRestock += item.suggestedRestock();
            if ("healthy".equals(item.healthLevel())) {
                healthyCount += 1;
            } else {
                riskCount += 1;
            }
        }
        audit.markLimitedResult(limited.size(), queryLimit);
        emitToolCompleted(ctx, name(), "返回 " + limited.size() + " 个商品库存全景", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "库存健康概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "命中商品数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "健康商品", "value", String.valueOf(healthyCount), "trend_direction", healthyCount > 0 ? "up" : "flat"),
                    mapOf("label", "风险商品", "value", String.valueOf(riskCount), "trend_direction", riskCount > 0 ? "down" : "flat"),
                    mapOf("label", "建议补货量", "value", formatNumber(totalSuggestedRestock), "trend_direction", totalSuggestedRestock > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "商品库存全景",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "当前库存", "安全库存", "近30天销量", "周转天数", "建议补货量", "健康度"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "库存健康建议",
            toJsonNode(ctx, mapOf(
                "level", riskCount > 0 ? "medium" : "low",
                "title", riskCount > 0 ? "检测到库存偏紧商品" : "库存健康度整体平稳",
                "description", focus == null
                    ? "当前未匹配到商品，请提供更精确的商品名称或编码。"
                    : buildSuggestionText(focus, riskCount),
                "affected_items", buildAffectedItems(limited),
                "suggested_action", focus == null ? "补充商品线索后重新查询" : buildActionText(focus, riskCount)
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock, riskBlock);
        if (focus == null) {
            return ToolResult.empty("未匹配到商品，请提供商品名称、编码或更精确的线索。");
        }
        String toolSummary = "商品 " + focus.productName() + "，库存 " + formatNumber(focus.stock())
            + "，近30天销量 " + formatNumber(focus.recentSalesQuantity())
            + "，建议补货 " + formatNumber(focus.suggestedRestock());
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "product_name", focus.productName(),
            "product_code", focus.productCode(),
            "current_stock", formatNumber(focus.stock()),
            "safe_stock", formatNumber(focus.safeStock()),
            "recent_sales_quantity", formatNumber(focus.recentSalesQuantity()),
            "turnover_days", turnoverText(focus.turnoverDays()),
            "suggested_restock", formatNumber(focus.suggestedRestock()),
            "health_level", healthLabel(focus.healthLevel()),
            "query_audit", audit.facts(),
            "top_items", buildSummaries(limit(limited, 5))
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<ProductEntity> resolveProducts(Long ownerUserId, Long productId, String keyword, int limit) {
        if (productId != null) {
            return productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
                .map(List::of)
                .orElse(List.of());
        }
        List<ProductEntity> fetched = productRepository.findAllByOwnerUserIdOrderByNameAsc(ownerUserId, PageRequest.of(0, limit));
        if (!StringUtils.hasText(keyword)) {
            return fetched;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        List<ProductEntity> filtered = new ArrayList<>(fetched.size());
        for (ProductEntity item : fetched) {
            String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
            String code = item.getCode() == null ? "" : item.getCode().toLowerCase(Locale.ROOT);
            if (name.contains(lowerKeyword) || code.contains(lowerKeyword)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private List<ProductPanorama> buildPanoramas(Long ownerUserId, List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<ProductPanorama> items = new ArrayList<>(products.size());
        for (ProductEntity product : products) {
            double stock = safeDouble(product.getStock());
            double safeStock = safeDouble(product.getSafeStock());
            double recentSales = recentSalesQuantity(ownerUserId, product);
            double suggestedRestock = Math.max(0D, Math.max(safeStock * 2 - stock, recentSales - stock));
            Double turnoverDays = calculateTurnoverDays(stock, recentSales);
            String healthLevel = healthLevel(stock, safeStock, recentSales);
            double score = healthScore(stock, safeStock, recentSales);
            items.add(new ProductPanorama(
                safeLong(product.getId()),
                safeText(product.getName(), "-"),
                safeText(product.getCode(), "-"),
                stock,
                safeStock,
                recentSales,
                turnoverDays,
                suggestedRestock,
                healthLevel,
                score
            ));
        }
        return items;
    }

    private double recentSalesQuantity(Long ownerUserId, ProductEntity product) {
        InventoryMonthlyStatsEntity currentMonth = currentMonthStats(ownerUserId, product);
        if (currentMonth != null && safeDouble(currentMonth.getQuantityOut()) > 0D) {
            return safeDouble(currentMonth.getQuantityOut());
        }
        InventoryMonthlyStatsEntity previousMonth = previousMonthStats(ownerUserId, product);
        if (previousMonth != null && safeDouble(previousMonth.getQuantityOut()) > 0D) {
            return safeDouble(previousMonth.getQuantityOut());
        }
        long now = System.currentTimeMillis();
        long startAt = now - RECENT_DAYS * DAY_MILLIS;
        List<Object[]> rows = saleOrderItemRepository.recentStockOutRows(ownerUserId, startAt, now, 2, PageRequest.of(0, 200));
        double fallback = 0D;
        for (Object[] row : rows) {
            if (row.length == 0 || !(row[0] instanceof com.zhihuiji.backend.domain.entity.SaleOrderItemEntity item)) {
                continue;
            }
            if (product.getId() != null && product.getId().equals(item.getProductId())) {
                fallback += safeDouble(item.getQuantity());
            }
        }
        return fallback;
    }

    private InventoryMonthlyStatsEntity currentMonthStats(Long ownerUserId, ProductEntity product) {
        long now = System.currentTimeMillis();
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault());
        return inventoryMonthlyStatsRepository.findByOwnerUserIdAndProductIdAndYearAndMonth(
            ownerUserId, product.getId(), current.getYear(), current.getMonthValue()
        ).orElse(null);
    }

    private InventoryMonthlyStatsEntity previousMonthStats(Long ownerUserId, ProductEntity product) {
        long now = System.currentTimeMillis();
        ZonedDateTime previous = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).minusMonths(1);
        return inventoryMonthlyStatsRepository.findByOwnerUserIdAndProductIdAndYearAndMonth(
            ownerUserId, product.getId(), previous.getYear(), previous.getMonthValue()
        ).orElse(null);
    }

    private Double calculateTurnoverDays(double stock, double recentSalesQuantity) {
        if (recentSalesQuantity <= 0D) {
            return null;
        }
        double dailySales = recentSalesQuantity / RECENT_DAYS;
        if (dailySales <= 0D) {
            return null;
        }
        return stock / dailySales;
    }

    private String healthLevel(double stock, double safeStock, double recentSalesQuantity) {
        if (stock <= 0D) {
            return "critical";
        }
        if (safeStock > 0D && stock < safeStock) {
            return "risk";
        }
        if (recentSalesQuantity > 0D && stock < recentSalesQuantity / 2D) {
            return "risk";
        }
        return "healthy";
    }

    private double healthScore(double stock, double safeStock, double recentSalesQuantity) {
        double score = 0D;
        if (safeStock > 0D) {
            score += Math.min(2D, stock / safeStock);
        } else {
            score += stock > 0D ? 1D : 0D;
        }
        if (recentSalesQuantity <= 0D) {
            score += 1D;
        } else {
            score += Math.min(2D, stock / Math.max(1D, recentSalesQuantity / 2D));
        }
        return score;
    }

    private List<List<Object>> buildRows(List<ProductPanorama> items) {
        List<List<Object>> rows = new ArrayList<>(items == null ? 0 : items.size());
        if (items == null) {
            return rows;
        }
        for (ProductPanorama item : items) {
            rows.add(List.of(
                item.productName(),
                item.productCode(),
                formatNumber(item.stock()),
                formatNumber(item.safeStock()),
                formatNumber(item.recentSalesQuantity()),
                turnoverText(item.turnoverDays()),
                formatNumber(item.suggestedRestock()),
                healthLabel(item.healthLevel())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<ProductPanorama> items) {
        List<Map<String, Object>> rows = new ArrayList<>(items == null ? 0 : items.size());
        if (items == null) {
            return rows;
        }
        for (ProductPanorama item : items) {
            rows.add(mapOf(
                "product_name", item.productName(),
                "product_code", item.productCode(),
                "current_stock", formatNumber(item.stock()),
                "safe_stock", formatNumber(item.safeStock()),
                "recent_sales_quantity", formatNumber(item.recentSalesQuantity()),
                "turnover_days", turnoverText(item.turnoverDays()),
                "suggested_restock", formatNumber(item.suggestedRestock()),
                "health_level", healthLabel(item.healthLevel())
            ));
        }
        return rows;
    }

    private List<String> buildAffectedItems(List<ProductPanorama> items) {
        List<String> rows = new ArrayList<>(items == null ? 0 : items.size());
        if (items == null) {
            return rows;
        }
        for (ProductPanorama item : items) {
            if (!"healthy".equals(item.healthLevel())) {
                rows.add(item.productName());
            }
        }
        if (rows.isEmpty() && !items.isEmpty()) {
            rows.add(items.get(0).productName());
        }
        return rows;
    }

    private String buildSuggestionText(ProductPanorama focus, int riskCount) {
        if (riskCount <= 0) {
            return "当前库存高于安全线，近 30 天销量与库存匹配，暂不需要额外补货。";
        }
        return "重点关注「" + focus.productName() + "」，建议补货量约 "
            + formatNumber(focus.suggestedRestock()) + "，并结合近 30 天销量复核安全库存设置。";
    }

    private String buildActionText(ProductPanorama focus, int riskCount) {
        if (riskCount <= 0) {
            return "保持当前库存节奏，并持续观察近 30 天销量变化";
        }
        return "优先补足「" + focus.productName() + "」的安全库存，再检查其他风险商品";
    }

    private String turnoverText(Double turnoverDays) {
        if (turnoverDays == null) {
            return "暂无销量";
        }
        return String.format(Locale.US, "%.1f天", turnoverDays);
    }

    private String healthLabel(String healthLevel) {
        return switch (healthLevel) {
            case "critical" -> "严重偏紧";
            case "risk" -> "偏紧";
            default -> "健康";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record ProductPanorama(
        long productId,
        String productName,
        String productCode,
        double stock,
        double safeStock,
        double recentSalesQuantity,
        Double turnoverDays,
        double suggestedRestock,
        String healthLevel,
        double healthScore
    ) {
    }
}

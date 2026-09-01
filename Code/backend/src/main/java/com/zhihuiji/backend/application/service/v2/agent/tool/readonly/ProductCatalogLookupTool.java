package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品目录查询工具，迁移自 V2AgentAiService.buildProductCatalogResponse。
 */
@Component
public class ProductCatalogLookupTool extends ToolSupport {

    private final ProductRepository productRepository;

    public ProductCatalogLookupTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String name() {
        return "product_catalog_lookup";
    }

    @Override
    public String displayName() {
        return "商品目录查询";
    }

    @Override
    public String description() {
        return "查询当前账号商品、库存、价格、类别相关数据";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "商品名称或编码关键词，可选");
        addIntegerProperty(schema, "status", "商品状态，可选；传 null 表示不限，0 表示停用，1 表示启用");
        addIntegerProperty(schema, "category_id", "商品分类 ID，可选；没有明确分类时传 null，不要传 0");
        addIntegerProperty(schema, "unit_id", "商品单位 ID，可选；没有明确单位时传 null，不要传 0");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        Integer status = paramInt(params, "status", null);
        Long categoryId = paramPositiveLong(params, "category_id");
        Long unitId = paramPositiveLong(params, "unit_id");
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status,
            "category_id", categoryId,
            "unit_id", unitId
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductEntity> products = productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(
            ownerUserId,
            keyword,
            status,
            categoryId,
            unitId,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<ProductEntity> topProducts = limit(products, 5);
        long totalProductCount = safeLong(productRepository.countByOwnerUserId(ownerUserId));
        double totalStock = safeDouble(productRepository.sumStockByOwnerUserId(ownerUserId));
        long lowStockCount = safeLong(productRepository.countLowStockByOwnerUserId(ownerUserId));
        audit.markLimitedResult(products.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "返回 " + products.size() + " 个商品，总计 " + totalProductCount + " 个商品", audit);

        ProductEntity maxStockProduct = null;
        double maxStock = Double.NEGATIVE_INFINITY;
        List<List<Object>> rows = new ArrayList<>(products.size());
        List<Map<String, Object>> topProductItems = new ArrayList<>(topProducts.size());
        for (int index = 0; index < products.size(); index += 1) {
            ProductEntity item = products.get(index);
            double stock = safeDouble(item.getStock());
            if (stock > maxStock) {
                maxStock = stock;
                maxStockProduct = item;
            }
            rows.add(List.of(
                item.getName(),
                item.getCode(),
                safeText(item.getCategory(), "-"),
                formatNumber(stock),
                money(safeDouble(item.getSalePrice()))
            ));
        }
        for (int index = 0; index < topProducts.size(); index += 1) {
            ProductEntity item = topProducts.get(index);
            topProductItems.add(mapOf(
                "product_id", item.getId(),
                "name", item.getName(),
                "code", item.getCode(),
                "category", safeText(item.getCategory(), "-"),
                "stock", formatNumber(safeDouble(item.getStock())),
                "sale_price", money(safeDouble(item.getSalePrice()))
            ));
        }

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "商品概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "商品总数", "value", String.valueOf(totalProductCount), "trend_direction", totalProductCount == 0L ? "flat" : "up"),
                    mapOf("label", "库存总计", "value", formatNumber(totalStock), "trend_direction", totalStock > 0 ? "up" : "flat"),
                    mapOf("label", "低库存商品", "value", String.valueOf(lowStockCount), "trend_direction", lowStockCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "商品列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "分类", "库存", "售价"),
                "rows", rows,
                "row_count", products.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "商品总数 " + totalProductCount + " 个，返回 " + products.size() + " 个，低库存 " + lowStockCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "product_count", totalProductCount,
            "returned_product_count", products.size(),
            "stock_total", formatNumber(totalStock),
            "low_stock_count", lowStockCount,
            "query_audit", audit.facts(),
            "top_products", topProductItems
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

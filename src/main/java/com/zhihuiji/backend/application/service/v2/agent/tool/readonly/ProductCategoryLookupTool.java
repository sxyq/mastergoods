package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductCategoryEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductCategoryRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品分类查询工具，查询当前账号商品分类、分类树与分类统计。
 */
@Component
public class ProductCategoryLookupTool extends ToolSupport {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;

    public ProductCategoryLookupTool(ProductCategoryRepository productCategoryRepository,
                                     ProductRepository productRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public String name() {
        return "product_category_lookup";
    }

    @Override
    public String displayName() {
        return "商品分类查询";
    }

    @Override
    public String description() {
        return "查询当前账号商品分类、分类树与分类统计";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Map<String, Object> input = mapOf("limit", DEFAULT_TOOL_LIMIT);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductCategoryEntity> categories = productCategoryRepository
            .findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        List<ProductCategoryEntity> recent = limit(categories, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(recent.size(), DEFAULT_TOOL_LIMIT);

        long totalProductCount = 0L;
        for (ProductCategoryEntity category : recent) {
            totalProductCount += safeLong(productRepository.countByOwnerUserIdAndCategoryId(ownerUserId, category.getId()));
        }
        emitToolCompleted(ctx, name(), "命中 " + recent.size() + " 个商品分类", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "商品分类概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "分类数", "value", String.valueOf(recent.size()), "trend_direction", recent.isEmpty() ? "flat" : "up"),
                    mapOf("label", "关联商品数", "value", String.valueOf(totalProductCount), "trend_direction", totalProductCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "商品分类",
            toJsonNode(ctx, mapOf(
                "headers", List.of("名称", "排序", "商品数"),
                "rows", buildCategoryRows(ownerUserId, recent),
                "row_count", recent.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = recent.isEmpty()
            ? "当前账号下还没有商品分类数据。"
            : "我查到了 " + recent.size() + " 个商品分类，关联商品共 " + totalProductCount + " 件。";
        String toolSummary = "商品分类 " + recent.size() + " 个，关联商品 " + totalProductCount + " 件";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "category_count", recent.size(),
            "total_product_count", totalProductCount,
            "query_audit", audit.facts(),
            "categories", buildCategorySummaries(ownerUserId, recent)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildCategoryRows(Long ownerUserId, List<ProductCategoryEntity> categories) {
        List<List<Object>> rows = new ArrayList<>(categories == null ? 0 : categories.size());
        if (categories == null) {
            return rows;
        }
        for (ProductCategoryEntity category : categories) {
            long productCount = safeLong(productRepository.countByOwnerUserIdAndCategoryId(ownerUserId, category.getId()));
            rows.add(List.of(
                safeText(category.getName(), "-"),
                String.valueOf(category.getSortOrder() == null ? 0 : category.getSortOrder()),
                String.valueOf(productCount)
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildCategorySummaries(Long ownerUserId, List<ProductCategoryEntity> categories) {
        List<Map<String, Object>> items = new ArrayList<>(categories == null ? 0 : categories.size());
        if (categories == null) {
            return items;
        }
        for (ProductCategoryEntity category : categories) {
            long productCount = safeLong(productRepository.countByOwnerUserIdAndCategoryId(ownerUserId, category.getId()));
            items.add(mapOf(
                "id", safeLong(category.getId()),
                "name", safeText(category.getName(), "-"),
                "sort_order", category.getSortOrder() == null ? 0 : category.getSortOrder(),
                "product_count", productCount
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

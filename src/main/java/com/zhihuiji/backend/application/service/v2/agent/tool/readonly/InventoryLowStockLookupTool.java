package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 低库存查询工具，迁移自 V2AgentAiService.buildInventoryResponse。
 */
@Component
public class InventoryLowStockLookupTool extends ToolSupport {

    private final ProductRepository productRepository;

    public InventoryLowStockLookupTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String name() {
        return "inventory_low_stock_lookup";
    }

    @Override
    public String displayName() {
        return "低库存查询";
    }

    @Override
    public String description() {
        return "查询当前账号低库存、补货、缺货相关数据";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        int queryLimit = Math.min(50, Math.max(1, paramInt(params, "limit", DEFAULT_TOOL_LIMIT)));
        Map<String, Object> input = mapOf("limit", queryLimit);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductEntity> products = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, queryLimit));
        List<ProductEntity> topProducts = limit(products, 5);
        audit.markLimitedResult(products.size(), queryLimit);
        emitToolCompleted(ctx, name(), "命中 " + products.size() + " 个低库存商品", audit);

        List<String> affectedItems = new ArrayList<>(topProducts.size());
        List<List<Object>> rows = new ArrayList<>(products.size());
        List<Map<String, Object>> topItems = new ArrayList<>(topProducts.size());
        for (int index = 0; index < topProducts.size(); index += 1) {
            affectedItems.add(topProducts.get(index).getName());
        }
        for (int index = 0; index < products.size(); index += 1) {
            ProductEntity item = products.get(index);
            rows.add(List.of(
                item.getName(),
                item.getCode(),
                formatNumber(item.getStock()),
                formatNumber(item.getSafeStock()),
                money(safeDouble(item.getSalePrice()))
            ));
        }
        for (int index = 0; index < topProducts.size(); index += 1) {
            ProductEntity item = topProducts.get(index);
            topItems.add(mapOf(
                "name", item.getName(),
                "code", item.getCode(),
                "stock", formatNumber(item.getStock()),
                "safe_stock", formatNumber(item.getSafeStock()),
                "sale_price", money(safeDouble(item.getSalePrice()))
            ));
        }
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "库存风险",
            toJsonNode(ctx, mapOf(
                "level", products.isEmpty() ? "low" : "high",
                "title", products.isEmpty() ? "暂无低库存风险" : "检测到低库存商品",
                "description", products.isEmpty() ? "当前库存没有低于安全库存的商品。" : "建议优先补货，避免影响销售。",
                "affected_items", affectedItems,
                "suggested_action", products.isEmpty() ? "保持当前补货节奏" : "先处理前 3 个低库存商品"
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "低库存商品列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品", "编码", "当前库存", "安全库存", "销售价"),
                "rows", rows,
                "row_count", products.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(riskBlock, tableBlock);
        String answer = products.isEmpty()
            ? "我已经检查了当前账号下的库存，暂时没有发现低于安全库存的商品。"
            : "我已经找出当前账号下最需要关注的低库存商品，建议先处理排在前面的补货项。";
        String toolSummary = "低库存商品 " + products.size() + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "low_stock_count", products.size(),
            "query_audit", audit.facts(),
            "top_items", topItems
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }
}

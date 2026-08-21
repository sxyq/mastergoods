package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.product.ProductSupplierRelationEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductSupplierRelationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品供应商关系查询工具。
 *
 * <p>查询当前账号下商品与供应商的供货关系，支持按商品 ID 过滤。返回默认供应商、采购优先级、
 * 最近采购价等信息，帮助用户了解商品进货来源。
 */
@Component
public class ProductSupplierRelationLookupTool extends ToolSupport {

    private final ProductSupplierRelationRepository relationRepository;

    public ProductSupplierRelationLookupTool(ProductSupplierRelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    @Override
    public String name() {
        return "product_supplier_relation_lookup";
    }

    @Override
    public String displayName() {
        return "商品供应商关系查询";
    }

    @Override
    public String description() {
        return "查询商品的供应商供货关系，包含默认供应商、采购优先级与最近采购价";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addIntegerProperty(schema, "product_id", "商品 ID，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long productId = paramLong(params, "product_id", null);
        Map<String, Object> input = mapOf("product_id", productId);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductSupplierRelationEntity> relations = productId == null
            ? relationRepository.findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(ownerUserId)
            : relationRepository.findAllByOwnerUserIdAndProductIdOrderByIsDefaultDescPurchasePriorityAscCreatedAtAsc(ownerUserId, productId);
        List<ProductSupplierRelationEntity> limited = limit(relations, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条商品供应商关系", audit);

        long defaultCount = 0L;
        for (ProductSupplierRelationEntity relation : limited) {
            if (Boolean.TRUE.equals(relation.getIsDefault())) {
                defaultCount += 1;
            }
        }
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "商品供应商关系概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "关系总数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "默认供应商数", "value", String.valueOf(defaultCount), "trend_direction", defaultCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "商品供应商关系列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("商品ID", "供应商ID", "默认", "采购优先级", "最近采购价", "备注"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "商品供应商关系 " + limited.size() + " 条，默认 " + defaultCount + " 条";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "relation_count", limited.size(),
            "default_count", defaultCount,
            "query_audit", audit.facts(),
            "recent_relations", buildSummaries(limited)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<ProductSupplierRelationEntity> relations) {
        List<List<Object>> rows = new ArrayList<>(relations == null ? 0 : relations.size());
        if (relations == null) {
            return rows;
        }
        for (ProductSupplierRelationEntity item : relations) {
            rows.add(List.of(
                String.valueOf(item.getProductId()),
                String.valueOf(item.getSupplierId()),
                Boolean.TRUE.equals(item.getIsDefault()) ? "是" : "否",
                item.getPurchasePriority() == null ? "-" : String.valueOf(item.getPurchasePriority()),
                money(safeDouble(item.getLastPurchasePrice())),
                safeText(item.getNotes(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<ProductSupplierRelationEntity> relations) {
        List<Map<String, Object>> items = new ArrayList<>(relations == null ? 0 : relations.size());
        if (relations == null) {
            return items;
        }
        for (ProductSupplierRelationEntity item : relations) {
            items.add(mapOf(
                "id", item.getId(),
                "product_id", item.getProductId(),
                "supplier_id", item.getSupplierId(),
                "is_default", Boolean.TRUE.equals(item.getIsDefault()),
                "purchase_priority", item.getPurchasePriority(),
                "last_purchase_price", money(safeDouble(item.getLastPurchasePrice()))
            ));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

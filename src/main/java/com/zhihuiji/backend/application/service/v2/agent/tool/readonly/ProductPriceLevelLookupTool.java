package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.ProductPriceLevelEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductPriceLevelRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 商品价格等级查询工具。
 *
 * <p>查询当前账号下定义的商品价格等级（如零售价、批发价、会员价等），
 * 返回等级编码、名称、状态与排序，帮助用户了解商品定价体系。
 */
@Component
public class ProductPriceLevelLookupTool extends ToolSupport {

    private final ProductPriceLevelRepository priceLevelRepository;

    public ProductPriceLevelLookupTool(ProductPriceLevelRepository priceLevelRepository) {
        this.priceLevelRepository = priceLevelRepository;
    }

    @Override
    public String name() {
        return "product_price_level_lookup";
    }

    @Override
    public String displayName() {
        return "商品价格等级查询";
    }

    @Override
    public String description() {
        return "查询商品价格等级定义，包含等级编码、名称、状态与排序";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Map<String, Object> input = mapOf();
        ToolAudit audit = startAudit(ctx, name(), input);

        List<ProductPriceLevelEntity> levels = priceLevelRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        List<ProductPriceLevelEntity> limited = limit(levels, DEFAULT_TOOL_LIMIT);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 个价格等级", audit);

        long activeCount = 0L;
        for (ProductPriceLevelEntity level : limited) {
            if (Integer.valueOf(1).equals(level.getStatus())) {
                activeCount += 1;
            }
        }
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "商品价格等级概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "等级总数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "启用等级数", "value", String.valueOf(activeCount), "trend_direction", activeCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "价格等级列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("编码", "名称", "状态", "排序"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = limited.isEmpty()
            ? "当前账号下还没有商品价格等级数据。"
            : "我查到了 " + limited.size() + " 个商品价格等级，其中 " + activeCount + " 个为启用状态。";
        String toolSummary = "价格等级 " + limited.size() + " 个，启用 " + activeCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "level_count", limited.size(),
            "active_count", activeCount,
            "query_audit", audit.facts(),
            "recent_levels", buildSummaries(limited)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<ProductPriceLevelEntity> levels) {
        List<List<Object>> rows = new ArrayList<>(levels == null ? 0 : levels.size());
        if (levels == null) {
            return rows;
        }
        for (ProductPriceLevelEntity item : levels) {
            rows.add(List.of(
                safeText(item.getCode(), "-"),
                safeText(item.getName(), "-"),
                statusLabel(item.getStatus()),
                item.getSortOrder() == null ? "-" : String.valueOf(item.getSortOrder())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<ProductPriceLevelEntity> levels) {
        List<Map<String, Object>> items = new ArrayList<>(levels == null ? 0 : levels.size());
        if (levels == null) {
            return items;
        }
        for (ProductPriceLevelEntity item : levels) {
            items.add(mapOf(
                "id", item.getId(),
                "code", item.getCode(),
                "name", item.getName(),
                "status", item.getStatus(),
                "status_label", statusLabel(item.getStatus()),
                "sort_order", item.getSortOrder()
            ));
        }
        return items;
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 1 -> "启用";
            case 0 -> "停用";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

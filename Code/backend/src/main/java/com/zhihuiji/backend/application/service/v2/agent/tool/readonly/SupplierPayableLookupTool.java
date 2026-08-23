package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 供应商应付查询工具，迁移自 V2AgentAiService.buildSupplierPayableResponse。
 */
@Component
public class SupplierPayableLookupTool extends ToolSupport {

    private final SupplierRepository supplierRepository;

    public SupplierPayableLookupTool(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public String name() {
        return "supplier_payable_lookup";
    }

    @Override
    public String displayName() {
        return "供应商应付查询";
    }

    @Override
    public String description() {
        return "查询当前账号供应商欠款、应付与采购相关数据";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "供应商关键词，可选");
        addIntegerProperty(schema, "status", "供应商状态，可选");
        addIntegerProperty(schema, "group_id", "供应商分组 ID，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        Integer status = paramInt(params, "status", null);
        Long groupId = paramLong(params, "group_id", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status,
            "group_id", groupId
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<SupplierEntity> topPayables = supplierRepository.findPayablesByOwnerUserIdAndFilters(
            ownerUserId, 0.0, keyword, status, groupId, PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<SupplierEntity> topSuppliers = limit(topPayables, 6);
        SupplierEntity highestPayableSupplier = topPayables.isEmpty() ? null : topPayables.get(0);
        long totalSupplierCount = safeLong(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0));
        double totalPayable = safeDouble(supplierRepository.sumPositiveBalance(ownerUserId));
        double topPayable = sumSupplierBalances(topPayables);
        audit.markLimitedResult(topPayables.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "返回 " + topPayables.size() + " 个应付供应商，总计 " + totalSupplierCount + " 个", audit);

        List<String> labels = new ArrayList<>(topSuppliers.size());
        List<Double> values = new ArrayList<>(topSuppliers.size());
        List<Map<String, Object>> topSupplierItems = new ArrayList<>(Math.min(topSuppliers.size(), 5));
        for (int index = 0; index < topSuppliers.size(); index += 1) {
            SupplierEntity item = topSuppliers.get(index);
            labels.add(compactChartLabel(item.getName()));
            values.add(safeDouble(item.getBalance()));
            if (index < 5) {
                topSupplierItems.add(mapOf(
                    "name", item.getName(),
                    "balance", money(safeDouble(item.getBalance())),
                    "phone", safeText(item.getPhone(), "-")
                ));
            }
        }

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "应付概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "应付供应商总数", "value", String.valueOf(totalSupplierCount), "trend_direction", totalSupplierCount == 0L ? "flat" : "up"),
                    mapOf("label", "应付总额", "value", money(totalPayable), "trend_direction", totalPayable > 0 ? "up" : "flat"),
                    mapOf("label", "最高单户应付", "value", highestPayableSupplier == null ? money(0) : money(safeDouble(highestPayableSupplier.getBalance())), "trend_direction", highestPayableSupplier == null ? "flat" : "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "供应商应付排行",
            toJsonNode(ctx, mapOf("items", buildSupplierPayableRank(topPayables)))
        );
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        blocks.add(kpiBlock);
        if (!topPayables.isEmpty()) {
            V2AgentDtos.ResultBlockDto barBlock = new V2AgentDtos.ResultBlockDto(
                "bar_chart",
                "Top 供应商应付柱状图",
                toJsonNode(ctx, mapOf(
                    "title", "Top 供应商应付柱状图",
                    "labels", labels,
                    "series", List.of(mapOf(
                        "name", "应付余额",
                        "data", values,
                        "color", "#FB8C00"
                    ))
                ))
            );
            blocks.add(barBlock);
        }
        blocks.add(rankBlock);
        String toolSummary = "应付供应商总数 " + totalSupplierCount + " 个，应付总额 " + money(totalPayable) + "，Top10 应付合计 " + money(topPayable);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "supplier_count", totalSupplierCount,
            "returned_supplier_count", topPayables.size(),
            "total_payable", money(totalPayable),
            "top10_payable_total", money(topPayable),
            "query_audit", audit.facts(),
            "top_suppliers", topSupplierItems
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private double sumSupplierBalances(List<SupplierEntity> suppliers) {
        double total = 0D;
        if (suppliers == null) {
            return total;
        }
        for (SupplierEntity supplier : suppliers) {
            total += safeDouble(supplier.getBalance());
        }
        return total;
    }

    private List<Map<String, Object>> buildSupplierPayableRank(List<SupplierEntity> suppliers) {
        List<Map<String, Object>> items = new ArrayList<>(suppliers == null ? 0 : suppliers.size());
        if (suppliers == null) {
            return items;
        }
        for (int index = 0; index < suppliers.size(); index++) {
            SupplierEntity supplier = suppliers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", supplier.getName(),
                "value", money(safeDouble(supplier.getBalance())),
                "change_direction", "up"
            ));
        }
        return items;
    }

    private String compactChartLabel(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "-";
        return text.length() <= 8 ? text : text.substring(0, 8) + "...";
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

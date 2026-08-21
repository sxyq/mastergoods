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
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 供应商主数据查询工具。
 *
 * <p>与应付查询分开：没有欠款的供应商仍然可以作为采购草稿的真实引用对象。
 */
@Component
public class SupplierDirectoryLookupTool extends ToolSupport {

    private final SupplierRepository supplierRepository;

    public SupplierDirectoryLookupTool(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public String name() {
        return "supplier_directory_lookup";
    }

    @Override
    public String displayName() {
        return "供应商目录查询";
    }

    @Override
    public String description() {
        return "查询当前账号所有可用供应商的真实 ID、名称、电话、状态和余额；没有欠款的供应商也会返回";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "供应商名称或电话关键词，可选");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String keyword = paramString(params, "keyword");
        Map<String, Object> input = mapOf("keyword", keyword == null ? "" : keyword);
        ToolAudit audit = startAudit(ctx, name(), input);

        List<SupplierEntity> fetched = supplierRepository.findAllByOwnerUserIdOrderByNameAsc(
            ctx.ownerUserId(), PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<SupplierEntity> suppliers = filter(fetched, keyword);
        audit.markLimitedResult(suppliers.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "返回 " + suppliers.size() + " 个真实供应商", audit);

        V2AgentDtos.ResultBlockDto table = new V2AgentDtos.ResultBlockDto(
            "table",
            "供应商目录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("供应商 ID", "名称", "电话", "状态", "余额"),
                "rows", buildRows(suppliers),
                "row_count", suppliers.size()
            ))
        );
        JsonNode facts = toJsonNode(ctx, mapOf(
            "supplier_count", suppliers.size(),
            "suppliers", buildFacts(suppliers),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(table), facts, "真实供应商 " + suppliers.size() + " 个");
    }

    private List<SupplierEntity> filter(List<SupplierEntity> suppliers, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return suppliers == null ? List.of() : suppliers;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        List<SupplierEntity> result = new ArrayList<>();
        if (suppliers != null) {
            for (SupplierEntity supplier : suppliers) {
                String name = supplier.getName() == null ? "" : supplier.getName().toLowerCase(Locale.ROOT);
                String phone = supplier.getPhone() == null ? "" : supplier.getPhone().toLowerCase(Locale.ROOT);
                if (name.contains(needle) || phone.contains(needle)) {
                    result.add(supplier);
                }
            }
        }
        return result;
    }

    private List<List<Object>> buildRows(List<SupplierEntity> suppliers) {
        List<List<Object>> rows = new ArrayList<>();
        for (SupplierEntity supplier : suppliers) {
            rows.add(List.of(
                safeLong(supplier.getId()),
                safeText(supplier.getName(), "-"),
                safeText(supplier.getPhone(), "-"),
                String.valueOf(supplier.getStatus()),
                money(safeDouble(supplier.getBalance()))
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildFacts(List<SupplierEntity> suppliers) {
        List<Map<String, Object>> facts = new ArrayList<>();
        for (SupplierEntity supplier : suppliers) {
            facts.add(mapOf(
                "supplier_id", safeLong(supplier.getId()),
                "name", safeText(supplier.getName(), "-"),
                "phone", safeText(supplier.getPhone(), "-"),
                "status", supplier.getStatus(),
                "balance", money(safeDouble(supplier.getBalance()))
            ));
        }
        return facts;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

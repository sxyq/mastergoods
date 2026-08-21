package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 客户主数据查询工具，供销售草稿绑定当前账号中的真实客户。 */
@Component
public class CustomerDirectoryLookupTool extends ToolSupport {

    private final CustomerRepository customerRepository;

    public CustomerDirectoryLookupTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "customer_directory_lookup";
    }

    @Override
    public String displayName() {
        return "客户目录查询";
    }

    @Override
    public String description() {
        return "查询当前账号所有客户的真实 ID、姓名、电话、状态和余额；用于销售草稿选择真实客户";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addStringProperty(schema, "keyword", "客户姓名或电话关键词，可选");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String keyword = paramString(params, "keyword");
        ToolAudit audit = startAudit(ctx, name(), mapOf("keyword", keyword == null ? "" : keyword));
        List<CustomerEntity> fetched = customerRepository.findAllByOwnerUserIdOrderByNameAsc(
            ctx.ownerUserId(), PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<CustomerEntity> customers = filter(fetched, keyword);
        audit.markLimitedResult(customers.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(ctx, name(), "返回 " + customers.size() + " 个真实客户", audit);

        V2AgentDtos.ResultBlockDto table = new V2AgentDtos.ResultBlockDto(
            "table",
            "客户目录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("客户 ID", "姓名", "电话", "状态", "余额"),
                "rows", buildRows(customers),
                "row_count", customers.size()
            ))
        );
        JsonNode facts = toJsonNode(ctx, mapOf(
            "customer_count", customers.size(),
            "customers", buildFacts(customers),
            "query_audit", audit.facts()
        ));
        return ToolResult.success(List.of(table), facts, "真实客户 " + customers.size() + " 个");
    }

    private List<CustomerEntity> filter(List<CustomerEntity> customers, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return customers == null ? List.of() : customers;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        List<CustomerEntity> result = new ArrayList<>();
        if (customers != null) {
            for (CustomerEntity customer : customers) {
                String name = customer.getName() == null ? "" : customer.getName().toLowerCase(Locale.ROOT);
                String phone = customer.getPhone() == null ? "" : customer.getPhone().toLowerCase(Locale.ROOT);
                if (name.contains(needle) || phone.contains(needle)) {
                    result.add(customer);
                }
            }
        }
        return result;
    }

    private List<List<Object>> buildRows(List<CustomerEntity> customers) {
        List<List<Object>> rows = new ArrayList<>();
        for (CustomerEntity customer : customers) {
            rows.add(List.of(
                safeLong(customer.getId()),
                safeText(customer.getName(), "-"),
                safeText(customer.getPhone(), "-"),
                String.valueOf(customer.getStatus()),
                money(safeDouble(customer.getBalance()))
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildFacts(List<CustomerEntity> customers) {
        List<Map<String, Object>> facts = new ArrayList<>();
        for (CustomerEntity customer : customers) {
            facts.add(mapOf(
                "customer_id", safeLong(customer.getId()),
                "name", safeText(customer.getName(), "-"),
                "phone", safeText(customer.getPhone(), "-"),
                "status", customer.getStatus(),
                "balance", money(safeDouble(customer.getBalance()))
            ));
        }
        return facts;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

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

/**
 * 客户应收查询工具，迁移自 V2AgentAiService.buildReceivableResponse。
 */
@Component
public class CustomerReceivableLookupTool extends ToolSupport {

    private final CustomerRepository customerRepository;

    public CustomerReceivableLookupTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String name() {
        return "customer_receivable_lookup";
    }

    @Override
    public String displayName() {
        return "客户应收查询";
    }

    @Override
    public String description() {
        return "查询当前账号客户欠款、应收、回款优先级";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
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

        List<CustomerEntity> fetched = customerRepository
            .findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(ownerUserId, 0.0, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        List<CustomerEntity> customers = filterInMemory(fetched, keyword, status, groupId);
        List<CustomerEntity> topCustomers = limit(customers, 6);
        CustomerEntity highestReceivableCustomer = customers.isEmpty() ? null : customers.get(0);
        audit.markLimitedResult(customers.size(), DEFAULT_TOOL_LIMIT);
        long totalCustomerCount = safeLong(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0));
        double totalReceivable = safeDouble(customerRepository.sumPositiveBalance(ownerUserId));
        emitToolCompleted(ctx, name(), "返回 " + customers.size() + " 个欠款客户，总计 " + totalCustomerCount + " 个", audit);

        double topReceivable = sumCustomerBalances(customers);
        List<String> labels = new ArrayList<>(topCustomers.size());
        List<Double> values = new ArrayList<>(topCustomers.size());
        List<Map<String, Object>> topCustomerItems = new ArrayList<>(Math.min(topCustomers.size(), 5));
        for (int index = 0; index < topCustomers.size(); index += 1) {
            CustomerEntity item = topCustomers.get(index);
            labels.add(compactChartLabel(item.getName()));
            values.add(safeDouble(item.getBalance()));
            if (index < 5) {
                topCustomerItems.add(mapOf(
                    "name", item.getName(),
                    "balance", money(safeDouble(item.getBalance())),
                    "level", item.getLevel()
                ));
            }
        }
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "应收概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "欠款客户总数", "value", String.valueOf(totalCustomerCount), "trend_direction", totalCustomerCount == 0L ? "flat" : "up"),
                    mapOf("label", "应收总额", "value", money(totalReceivable), "trend_direction", totalReceivable > 0 ? "down" : "flat"),
                    mapOf("label", "最高单户欠款", "value", highestReceivableCustomer == null ? money(0) : money(safeDouble(highestReceivableCustomer.getBalance())), "trend_direction", highestReceivableCustomer == null ? "flat" : "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "欠款客户排行",
            toJsonNode(ctx, mapOf(
                "items", buildRankItems(customers)
            ))
        );
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        blocks.add(kpiBlock);
        if (!customers.isEmpty()) {
            V2AgentDtos.ResultBlockDto barBlock = new V2AgentDtos.ResultBlockDto(
                "bar_chart",
                "Top 客户应收柱状图",
                toJsonNode(ctx, mapOf(
                    "title", "Top 客户应收柱状图",
                    "labels", labels,
                    "series", List.of(mapOf(
                        "name", "应收余额",
                        "data", values,
                        "color", "#005BBF"
                    ))
                ))
            );
            blocks.add(barBlock);
        }
        blocks.add(rankBlock);
        String answer = customers.isEmpty()
            ? "当前账号下没有查询到欠款客户，应收风险比较低。"
            : "我已经按欠款金额从高到低整理出客户排行，可以先跟进前几位客户的回款。";
        String toolSummary = "欠款客户总数 " + totalCustomerCount + " 个，应收总额 " + money(totalReceivable) + "，Top10 应收合计 " + money(topReceivable);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "customer_count", totalCustomerCount,
            "returned_customer_count", customers.size(),
            "total_receivable", money(totalReceivable),
            "top10_receivable_total", money(topReceivable),
            "query_audit", audit.facts(),
            "top_customers", topCustomerItems
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<CustomerEntity> filterInMemory(List<CustomerEntity> customers, String keyword, Integer status, Long groupId) {
        if (customers == null || customers.isEmpty()) {
            return List.of();
        }
        boolean hasKeyword = StringUtils.hasText(keyword);
        String lowerKeyword = hasKeyword ? keyword.toLowerCase(Locale.ROOT) : null;
        List<CustomerEntity> filtered = new ArrayList<>(customers.size());
        for (CustomerEntity item : customers) {
            if (status != null && !status.equals(item.getStatus())) {
                continue;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                continue;
            }
            if (hasKeyword) {
                String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
                String phone = item.getPhone() == null ? "" : item.getPhone().toLowerCase(Locale.ROOT);
                if (!name.contains(lowerKeyword) && !phone.contains(lowerKeyword)) {
                    continue;
                }
            }
            filtered.add(item);
        }
        return filtered;
    }

    private double sumCustomerBalances(List<CustomerEntity> customers) {
        double total = 0D;
        if (customers == null) {
            return total;
        }
        for (CustomerEntity customer : customers) {
            total += safeDouble(customer.getBalance());
        }
        return total;
    }

    private List<Map<String, Object>> buildRankItems(List<CustomerEntity> customers) {
        List<Map<String, Object>> items = new ArrayList<>(customers == null ? 0 : customers.size());
        if (customers == null) {
            return items;
        }
        for (int index = 0; index < customers.size(); index++) {
            CustomerEntity customer = customers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", customer.getName(),
                "value", money(safeDouble(customer.getBalance())),
                "change_direction", "down"
            ));
        }
        return items;
    }

    private String compactChartLabel(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "-";
        return text.length() <= 8 ? text : text.substring(0, 8) + "...";
    }
}

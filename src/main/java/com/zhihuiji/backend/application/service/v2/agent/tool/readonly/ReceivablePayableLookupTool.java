package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 应收应付对账工具。
 *
 * <p>汇总客户应收与供应商应付，给出对账差额、重点往来方与跟进建议。
 */
@Component
public class ReceivablePayableLookupTool extends ToolSupport {

    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public ReceivablePayableLookupTool(CustomerRepository customerRepository,
                                       SupplierRepository supplierRepository) {
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public String name() {
        return "receivable_payable_lookup";
    }

    @Override
    public String displayName() {
        return "应收应付对账";
    }

    @Override
    public String description() {
        return "汇总当前账号的客户应收、供应商应付、差额与重点往来方";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<CustomerEntity> receivableFetched = customerRepository
            .findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(ownerUserId, 0.0, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        List<SupplierEntity> payableFetched = supplierRepository
            .findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(ownerUserId, 0.0, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        List<CustomerEntity> receivableCustomers = filterCustomers(receivableFetched, keyword);
        List<SupplierEntity> payableSuppliers = filterSuppliers(payableFetched, keyword);

        long receivableCustomerCount = safeLong(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0));
        long payableSupplierCount = safeLong(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0));
        double totalReceivable = safeDouble(customerRepository.sumPositiveBalance(ownerUserId));
        double totalPayable = safeDouble(supplierRepository.sumPositiveBalance(ownerUserId));
        double receivableTopTotal = sumCustomerBalances(receivableCustomers);
        double payableTopTotal = sumSupplierBalances(payableSuppliers);
        double netExposure = totalReceivable - totalPayable;
        boolean keywordFiltered = StringUtils.hasText(keyword);

        audit.markReturned(receivableCustomers.size() + payableSuppliers.size());
        emitToolCompleted(
            ctx,
            name(),
            "应收客户 " + receivableCustomerCount + " 个、应付供应商 " + payableSupplierCount + " 个",
            audit
        );

        CustomerEntity topCustomer = receivableCustomers.isEmpty() ? null : receivableCustomers.get(0);
        SupplierEntity topSupplier = payableSuppliers.isEmpty() ? null : payableSuppliers.get(0);
        String followUp = buildFollowUpAdvice(totalReceivable, totalPayable, topCustomer, topSupplier);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "应收应付概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "应收总额", "value", money(totalReceivable), "trend_direction", totalReceivable > 0 ? "down" : "flat"),
                    mapOf("label", "应付总额", "value", money(totalPayable), "trend_direction", totalPayable > 0 ? "up" : "flat"),
                    mapOf("label", "净敞口", "value", money(netExposure), "trend_direction", netExposure >= 0 ? "up" : "down"),
                    mapOf("label", "重点往来方", "value", buildCounterpartyLabel(topCustomer, topSupplier), "trend_direction", "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto amountBlock = new V2AgentDtos.ResultBlockDto(
            "bar_chart",
            "应收应付金额对比",
            toJsonNode(ctx, mapOf(
                "title", "应收应付金额对比",
                "labels", List.of("应收", "应付", "净敞口"),
                "series", List.of(mapOf(
                    "name", "金额",
                    "data", List.of(totalReceivable, totalPayable, netExposure),
                    "color", "#005BBF"
                ))
            ))
        );
        V2AgentDtos.ResultBlockDto receivableRankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "重点应收客户",
            toJsonNode(ctx, mapOf(
                "items", buildCustomerRank(receivableCustomers)
            ))
        );
        V2AgentDtos.ResultBlockDto payableRankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "重点应付供应商",
            toJsonNode(ctx, mapOf(
                "items", buildSupplierRank(payableSuppliers)
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "对账提醒",
            toJsonNode(ctx, mapOf(
                "level", riskLevel(totalReceivable, totalPayable),
                "title", keywordFiltered ? "已按关键词聚焦往来对账" : "往来对账已汇总",
                "description", followUp,
                "affected_items", buildAffectedItems(topCustomer, topSupplier),
                "suggested_action", followUp
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(
            kpiBlock,
            amountBlock,
            receivableRankBlock,
            payableRankBlock,
            riskBlock
        );
        String answer = buildAnswer(totalReceivable, totalPayable, netExposure, receivableCustomerCount, payableSupplierCount, followUp);
        String toolSummary = "应收 " + money(totalReceivable) + "，应付 " + money(totalPayable) + "，净敞口 " + money(netExposure);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "receivable_customer_count", receivableCustomerCount,
            "payable_supplier_count", payableSupplierCount,
            "returned_receivable_count", receivableCustomers.size(),
            "returned_payable_count", payableSuppliers.size(),
            "total_receivable", money(totalReceivable),
            "total_payable", money(totalPayable),
            "net_exposure", money(netExposure),
            "top10_receivable_total", money(receivableTopTotal),
            "top10_payable_total", money(payableTopTotal),
            "query_audit", audit.facts(),
            "top_receivable_customers", buildCustomerSummaries(receivableCustomers),
            "top_payable_suppliers", buildSupplierSummaries(payableSuppliers)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<CustomerEntity> filterCustomers(List<CustomerEntity> customers, String keyword) {
        if (!StringUtils.hasText(keyword) || customers == null || customers.isEmpty()) {
            return customers == null ? List.of() : customers;
        }
        String normalized = keyword.trim().toLowerCase();
        List<CustomerEntity> filtered = new ArrayList<>(customers.size());
        for (CustomerEntity customer : customers) {
            String name = customer.getName() == null ? "" : customer.getName().toLowerCase();
            String phone = customer.getPhone() == null ? "" : customer.getPhone().toLowerCase();
            if (name.contains(normalized) || phone.contains(normalized)) {
                filtered.add(customer);
            }
        }
        return filtered;
    }

    private List<SupplierEntity> filterSuppliers(List<SupplierEntity> suppliers, String keyword) {
        if (!StringUtils.hasText(keyword) || suppliers == null || suppliers.isEmpty()) {
            return suppliers == null ? List.of() : suppliers;
        }
        String normalized = keyword.trim().toLowerCase();
        List<SupplierEntity> filtered = new ArrayList<>(suppliers.size());
        for (SupplierEntity supplier : suppliers) {
            String name = supplier.getName() == null ? "" : supplier.getName().toLowerCase();
            String phone = supplier.getPhone() == null ? "" : supplier.getPhone().toLowerCase();
            if (name.contains(normalized) || phone.contains(normalized)) {
                filtered.add(supplier);
            }
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

    private List<Map<String, Object>> buildCustomerRank(List<CustomerEntity> customers) {
        List<Map<String, Object>> items = new ArrayList<>(customers == null ? 0 : customers.size());
        if (customers == null) {
            return items;
        }
        for (int index = 0; index < customers.size(); index++) {
            CustomerEntity customer = customers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", safeText(customer.getName(), "-"),
                "value", money(safeDouble(customer.getBalance())),
                "change_direction", "down"
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildSupplierRank(List<SupplierEntity> suppliers) {
        List<Map<String, Object>> items = new ArrayList<>(suppliers == null ? 0 : suppliers.size());
        if (suppliers == null) {
            return items;
        }
        for (int index = 0; index < suppliers.size(); index++) {
            SupplierEntity supplier = suppliers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", safeText(supplier.getName(), "-"),
                "value", money(safeDouble(supplier.getBalance())),
                "change_direction", "up"
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildCustomerSummaries(List<CustomerEntity> customers) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (customers == null) {
            return items;
        }
        List<CustomerEntity> limited = limit(customers, 5);
        for (CustomerEntity customer : limited) {
            items.add(mapOf(
                "name", safeText(customer.getName(), "-"),
                "phone", safeText(customer.getPhone(), "-"),
                "balance", money(safeDouble(customer.getBalance()))
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildSupplierSummaries(List<SupplierEntity> suppliers) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (suppliers == null) {
            return items;
        }
        List<SupplierEntity> limited = limit(suppliers, 5);
        for (SupplierEntity supplier : limited) {
            items.add(mapOf(
                "name", safeText(supplier.getName(), "-"),
                "phone", safeText(supplier.getPhone(), "-"),
                "balance", money(safeDouble(supplier.getBalance()))
            ));
        }
        return items;
    }

    private String buildAnswer(double totalReceivable,
                               double totalPayable,
                               double netExposure,
                               long receivableCustomerCount,
                               long payableSupplierCount,
                               String followUp) {
        return "当前应收总额 " + money(totalReceivable)
            + "，覆盖 " + receivableCustomerCount + " 个欠款客户；应付总额 "
            + money(totalPayable) + "，覆盖 " + payableSupplierCount + " 个应付供应商；净敞口 "
            + money(netExposure) + "。" + followUp;
    }

    private String buildFollowUpAdvice(double totalReceivable,
                                       double totalPayable,
                                       CustomerEntity topCustomer,
                                       SupplierEntity topSupplier) {
        if (totalReceivable == 0D && totalPayable == 0D) {
            return "当前往来余额整体平稳，暂未发现明显的应收应付压力。";
        }
        if (totalReceivable >= totalPayable) {
            return "建议优先跟进客户「" + safeText(topCustomer == null ? null : topCustomer.getName(), "重点客户")
                + "」的回款，同时评估供应商付款节奏。";
        }
        return "建议优先安排供应商「" + safeText(topSupplier == null ? null : topSupplier.getName(), "重点供应商")
            + "」付款，并同步催收重点客户回款以平衡现金流。";
    }

    private String riskLevel(double totalReceivable, double totalPayable) {
        if (totalReceivable == 0D && totalPayable == 0D) {
            return "low";
        }
        if (Math.abs(totalReceivable - totalPayable) >= 1000D) {
            return "high";
        }
        return "medium";
    }

    private String buildCounterpartyLabel(CustomerEntity topCustomer, SupplierEntity topSupplier) {
        if (topCustomer == null && topSupplier == null) {
            return "暂无";
        }
        if (topCustomer == null) {
            return "供应商:" + safeText(topSupplier.getName(), "-");
        }
        if (topSupplier == null) {
            return "客户:" + safeText(topCustomer.getName(), "-");
        }
        return safeText(topCustomer.getName(), "-") + " / " + safeText(topSupplier.getName(), "-");
    }

    private List<String> buildAffectedItems(CustomerEntity topCustomer, SupplierEntity topSupplier) {
        List<String> items = new ArrayList<>(2);
        if (topCustomer != null) {
            items.add("客户:" + safeText(topCustomer.getName(), "-"));
        }
        if (topSupplier != null) {
            items.add("供应商:" + safeText(topSupplier.getName(), "-"));
        }
        return items;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

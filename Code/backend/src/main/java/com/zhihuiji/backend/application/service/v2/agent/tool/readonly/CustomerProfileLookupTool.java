package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 客户画像与催收建议工具。
 *
 * <p>结合客户余额、最近订单、收款方式和退货记录生成轻量客户画像。
 */
@Component
public class CustomerProfileLookupTool extends ToolSupport {

    private static final int CANCELLED_SALE_ORDER_STATUS = 2;

    private final CustomerRepository customerRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PaymentRepository paymentRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final ObjectMapper objectMapper;

    public CustomerProfileLookupTool(CustomerRepository customerRepository,
                                     SaleOrderRepository saleOrderRepository,
                                     PaymentRepository paymentRepository,
                                     SalesReturnRepository salesReturnRepository,
                                     ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.paymentRepository = paymentRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "customer_profile_lookup";
    }

    @Override
    public String displayName() {
        return "客户画像与催收建议";
    }

    @Override
    public String description() {
        return "查询一个真实客户及其余额、订单、收款和退货画像；未指定客户且当前仅有一个客户时可直接使用该客户";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode customerId = properties.putObject("customer_id");
        customerId.put("type", "integer");
        customerId.put("description", "客户 ID，可选；没有明确 ID 时传 null，不要传 0");
        ObjectNode keyword = properties.putObject("keyword");
        keyword.put("type", "string");
        keyword.put("description", "客户姓名或手机号关键词，可选；没有明确关键词时传 null，不要传字符串 null");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long customerId = paramPositiveLong(params, "customer_id");
        String keyword = paramString(params, "keyword");
        Map<String, Object> input = mapOf(
            "customer_id", customerId,
            "keyword", keyword == null ? "" : keyword
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        CustomerEntity customer = resolveCustomer(ownerUserId, customerId, keyword);
        if (customer == null) {
            emitToolCompleted(ctx, name(), "未匹配到客户画像对象", audit);
            return ToolResult.empty("未匹配到客户，请提供客户姓名、手机号或更精确的线索。");
        }

        List<SaleOrderEntity> orders = loadCustomerOrders(ownerUserId, customer);
        List<PaymentEntity> payments = loadCustomerPayments(ownerUserId, orders);
        List<SalesReturnEntity> returns = loadCustomerReturns(ownerUserId, orders);

        double totalSalesAmount = sumOrderAmount(orders);
        double totalPaidAmount = sumOrderPaid(orders);
        double totalReceivedAmount = sumReceivedPayments(payments);
        double balance = safeDouble(customer.getBalance());
        double totalRefundAmount = sumReturnRefund(returns);
        long orderCount = orders.size();
        long returnCount = returns.size();
        SaleOrderEntity latestOrder = orders.isEmpty() ? null : orders.get(0);
        String paymentHabit = paymentHabit(payments);
        String collectionSuggestion = collectionSuggestion(balance, latestOrder, paymentHabit, returnCount);

        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "客户 " + safeText(customer.getName(), "-") + " 画像已生成", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "客户画像概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "累计销售额", "value", money(totalSalesAmount), "trend_direction", totalSalesAmount > 0 ? "up" : "flat"),
                    mapOf("label", "当前欠款", "value", money(balance), "trend_direction", balance > 0 ? "down" : "flat"),
                    mapOf("label", "最近收款", "value", money(totalReceivedAmount), "trend_direction", totalReceivedAmount > 0 ? "up" : "flat"),
                    mapOf("label", "付款习惯", "value", paymentHabit, "trend_direction", "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto profileBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "客户画像详情",
            toJsonNode(ctx, mapOf(
                "headers", List.of("客户", "手机号", "等级", "最近订单", "最近下单时间", "退货次数"),
                "rows", List.of(List.of(
                    safeText(customer.getName(), "-"),
                    safeText(customer.getPhone(), "-"),
                    customerLevelLabel(customer.getLevel()),
                    latestOrder == null ? "-" : safeText(latestOrder.getOrderNo(), "-"),
                    latestOrder == null ? "-" : String.valueOf(safeLong(latestOrder.getCreatedAt())),
                    String.valueOf(returnCount)
                )),
                "row_count", 1
            ))
        );
        V2AgentDtos.ResultBlockDto actionBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "催收与经营建议",
            toJsonNode(ctx, mapOf(
                "level", balance >= 1000D ? "high" : (balance > 0 ? "medium" : "low"),
                "title", balance > 0 ? "建议跟进客户回款" : "客户往来相对健康",
                "description", collectionSuggestion,
                "affected_items", List.of(safeText(customer.getName(), "-")),
                "suggested_action", collectionSuggestion
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, profileBlock, actionBlock);
        String toolSummary = "客户 " + safeText(customer.getName(), "-") + "，欠款 " + money(balance) + "，订单 " + orderCount + " 笔";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "customer_id", safeLong(customer.getId()),
            "customer_name", safeText(customer.getName(), ""),
            "customer_phone", safeText(customer.getPhone(), ""),
            "customer_level", customerLevelLabel(customer.getLevel()),
            "order_count", orderCount,
            "total_sales_amount", money(totalSalesAmount),
            "total_paid_amount", money(totalPaidAmount),
            "total_received_amount", money(totalReceivedAmount),
            "balance", money(balance),
            "latest_order_no", latestOrder == null ? "" : safeText(latestOrder.getOrderNo(), ""),
            "latest_order_at", latestOrder == null ? 0L : safeLong(latestOrder.getCreatedAt()),
            "payment_habit", paymentHabit,
            "return_count", returnCount,
            "refund_amount", money(totalRefundAmount),
            "collection_suggestion", collectionSuggestion,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private CustomerEntity resolveCustomer(Long ownerUserId, Long customerId, String keyword) {
        if (customerId != null) {
            return customerRepository.findByIdAndOwnerUserId(customerId, ownerUserId).orElse(null);
        }
        if (!StringUtils.hasText(keyword)) {
            List<CustomerEntity> candidates = customerRepository
                .findAllByOwnerUserIdOrderByNameAsc(ownerUserId, PageRequest.of(0, 2));
            return candidates.size() == 1 ? candidates.get(0) : null;
        }
        List<CustomerEntity> matches = customerRepository.search(ownerUserId, keyword.trim(), null, null);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private List<SaleOrderEntity> loadCustomerOrders(Long ownerUserId, CustomerEntity customer) {
        String customerName = safeText(customer.getName(), "");
        List<SaleOrderEntity> matches = saleOrderRepository.search(
            ownerUserId,
            customerName,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        List<SaleOrderEntity> filtered = new ArrayList<>();
        for (SaleOrderEntity order : matches) {
            if (order.getStatus() != null && order.getStatus() == CANCELLED_SALE_ORDER_STATUS) {
                continue;
            }
            boolean sameId = customer.getId() != null && customer.getId().equals(order.getCustomerId());
            boolean sameName = customerName.equalsIgnoreCase(safeText(order.getCustomerName(), ""));
            if (sameId || sameName) {
                filtered.add(order);
            }
        }
        filtered.sort(Comparator.comparingLong((SaleOrderEntity item) -> safeLong(item.getCreatedAt())).reversed());
        return filtered;
    }

    private List<PaymentEntity> loadCustomerPayments(Long ownerUserId, List<SaleOrderEntity> orders) {
        List<PaymentEntity> payments = new ArrayList<>();
        for (SaleOrderEntity order : orders) {
            payments.addAll(paymentRepository.findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(ownerUserId, order.getId()));
        }
        payments.sort(Comparator.comparingLong((PaymentEntity item) -> safeLong(item.getCreatedAt())).reversed());
        return payments;
    }

    private List<SalesReturnEntity> loadCustomerReturns(Long ownerUserId, List<SaleOrderEntity> orders) {
        List<SalesReturnEntity> returns = new ArrayList<>();
        for (SaleOrderEntity order : orders) {
            returns.addAll(salesReturnRepository.findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc(ownerUserId, order.getId()));
        }
        returns.sort(Comparator.comparingLong((SalesReturnEntity item) -> safeLong(item.getCreatedAt())).reversed());
        return returns;
    }

    private double sumOrderAmount(List<SaleOrderEntity> orders) {
        double total = 0D;
        for (SaleOrderEntity order : orders) {
            total += safeDouble(order.getTotalAmount());
        }
        return total;
    }

    private double sumOrderPaid(List<SaleOrderEntity> orders) {
        double total = 0D;
        for (SaleOrderEntity order : orders) {
            total += safeDouble(order.getPaidAmount());
        }
        return total;
    }

    private double sumReceivedPayments(List<PaymentEntity> payments) {
        double total = 0D;
        for (PaymentEntity item : payments) {
            if (safeDouble(item.getAmount()) > 0D) {
                total += safeDouble(item.getAmount());
            }
        }
        return total;
    }

    private double sumReturnRefund(List<SalesReturnEntity> returns) {
        double total = 0D;
        for (SalesReturnEntity item : returns) {
            total += safeDouble(item.getRefundAmount());
        }
        return total;
    }

    private String paymentHabit(List<PaymentEntity> payments) {
        int cash = 0;
        int wechat = 0;
        int alipay = 0;
        int bank = 0;
        for (PaymentEntity payment : payments) {
            switch (payment.getMethod() == null ? -1 : payment.getMethod()) {
                case 0 -> cash += 1;
                case 1 -> wechat += 1;
                case 2 -> alipay += 1;
                case 3 -> bank += 1;
                default -> {
                }
            }
        }
        int max = Math.max(Math.max(cash, wechat), Math.max(alipay, bank));
        if (max <= 0) {
            return "待观察";
        }
        if (max == cash) {
            return "现金";
        }
        if (max == wechat) {
            return "微信";
        }
        if (max == alipay) {
            return "支付宝";
        }
        return "银行卡";
    }

    private String collectionSuggestion(double balance,
                                        SaleOrderEntity latestOrder,
                                        String paymentHabit,
                                        long returnCount) {
        if (balance <= 0D) {
            return "当前没有明显欠款压力，可继续保持现有合作节奏。";
        }
        String recentSignal = latestOrder == null
            ? "最近暂无订单"
            : "最近订单「" + safeText(latestOrder.getOrderNo(), "-") + "」金额 " + money(safeDouble(latestOrder.getTotalAmount()));
        String returnSignal = returnCount > 0 ? "近期待退货/退款信号较明显，沟通时需同步确认售后体验。" : "近期退货较少，可优先推进账期确认。";
        if (balance >= 1000D) {
            return recentSignal + "，建议优先电话跟进回款并结合" + paymentHabit + "习惯给出明确付款节点。" + returnSignal;
        }
        return recentSignal + "，建议在下次成交前顺带确认回款安排，优先沿用" + paymentHabit + "方式降低沟通成本。" + returnSignal;
    }

    private String customerLevelLabel(Integer level) {
        return switch (level == null ? -1 : level) {
            case 0 -> "普通";
            case 1 -> "标准";
            case 2 -> "重要";
            case 3 -> "核心";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

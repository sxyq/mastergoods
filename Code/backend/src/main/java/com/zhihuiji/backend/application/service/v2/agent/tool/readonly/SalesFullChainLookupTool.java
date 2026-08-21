package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 销售全链路追踪工具。
 *
 * <p>串联销售单与其关联收款记录、退货记录，输出完整业务链路。
 */
@Component
public class SalesFullChainLookupTool extends ToolSupport {

    private final SaleOrderRepository saleOrderRepository;
    private final PaymentRepository paymentRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final ObjectMapper objectMapper;

    public SalesFullChainLookupTool(SaleOrderRepository saleOrderRepository,
                                    PaymentRepository paymentRepository,
                                    SalesReturnRepository salesReturnRepository,
                                    ObjectMapper objectMapper) {
        this.saleOrderRepository = saleOrderRepository;
        this.paymentRepository = paymentRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "sales_full_chain_lookup";
    }

    @Override
    public String displayName() {
        return "销售全链路追踪";
    }

    @Override
    public String description() {
        return "查询指定销售单（需要销售单号、客户名或其他关键词）及其关联收款、退货记录的完整业务链路；"
            + "用户只问最近销售单列表、客户和收款情况时使用 sale_order_lookup";
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
        ObjectNode keyword = properties.putObject("keyword");
        keyword.put("type", "string");
        keyword.put("description", "销售单关键词（单号或客户名）");
        ObjectNode orderId = properties.putObject("order_id");
        orderId.put("type", "integer");
        orderId.put("description", "指定销售单 ID");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long orderId = paramLong(params, "order_id", null);
        String keyword = paramString(params, "keyword");
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "order_id", orderId
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        SaleOrderEntity order = resolveOrder(ownerUserId, orderId, keyword);
        if (order == null) {
            emitToolCompleted(ctx, name(), "未匹配到销售单", audit);
            return ToolResult.empty("未匹配到销售单");
        }

        List<PaymentEntity> payments = paymentRepository
            .findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(ownerUserId, order.getId());
        List<SalesReturnEntity> returns = salesReturnRepository
            .findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc(ownerUserId, order.getId());
        List<PaymentEntity> limitedPayments = limit(payments, DEFAULT_TOOL_LIMIT);
        List<SalesReturnEntity> limitedReturns = limit(returns, DEFAULT_TOOL_LIMIT);
        audit.markReturned(1 + limitedPayments.size() + limitedReturns.size());
        emitToolCompleted(ctx, name(),
            "销售单 " + safeText(order.getOrderNo(), "-") + " 关联收款 " + payments.size() + " 条、退货 " + returns.size() + " 条", audit);

        double totalAmount = safeDouble(order.getTotalAmount());
        double paidAmount = safeDouble(order.getPaidAmount());
        double unpaidAmount = Math.max(0D, totalAmount - paidAmount);
        double returnRefund = sumReturnRefund(returns);
        double returnTotal = sumReturnTotal(returns);
        double receivedAmount = sumReceivedPayments(payments);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "销售单链路概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "销售总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已收金额", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat"),
                    mapOf("label", "未收金额", "value", money(unpaidAmount), "trend_direction", unpaidAmount > 0 ? "down" : "flat"),
                    mapOf("label", "退货金额", "value", money(returnTotal), "trend_direction", returnTotal > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto orderBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "销售单信息",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "客户", "总额", "已收", "状态"),
                "rows", List.of(List.of(
                    safeText(order.getOrderNo(), "-"),
                    safeText(order.getCustomerName(), "-"),
                    money(totalAmount),
                    money(paidAmount),
                    saleOrderStatusLabel(order.getStatus())
                )),
                "row_count", 1
            ))
        );
        V2AgentDtos.ResultBlockDto paymentBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "关联收款记录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("金额", "方式", "类型", "单号"),
                "rows", buildPaymentRows(limitedPayments),
                "row_count", limitedPayments.size()
            ))
        );
        V2AgentDtos.ResultBlockDto returnBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "关联退货记录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("退货单号", "客户", "退货金额", "已退金额", "状态"),
                "rows", buildReturnRows(limitedReturns),
                "row_count", limitedReturns.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, orderBlock, paymentBlock, returnBlock);
        String toolSummary = "销售单 " + safeText(order.getOrderNo(), "-") + " 收款 " + payments.size()
            + " 条、退货 " + returns.size() + " 条，未收 " + money(unpaidAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "order_id", safeLong(order.getId()),
            "order_no", safeText(order.getOrderNo(), ""),
            "customer_name", safeText(order.getCustomerName(), ""),
            "total_amount", money(totalAmount),
            "paid_amount", money(paidAmount),
            "unpaid_amount", money(unpaidAmount),
            "payment_count", payments.size(),
            "return_count", returns.size(),
            "return_total_amount", money(returnTotal),
            "return_refund_amount", money(returnRefund),
            "received_amount", money(receivedAmount),
            "query_audit", audit.facts(),
            "recent_payments", buildPaymentSummaries(limitedPayments),
            "recent_returns", buildReturnSummaries(limitedReturns)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private SaleOrderEntity resolveOrder(Long ownerUserId, Long orderId, String keyword) {
        if (orderId != null) {
            return saleOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElse(null);
        }
        if (StringUtils.hasText(keyword)) {
            List<SaleOrderEntity> matches = saleOrderRepository.search(
                ownerUserId, keyword, null, null, null, null, null, null, null
            );
            return matches.isEmpty() ? null : matches.get(0);
        }
        return null;
    }

    private List<List<Object>> buildPaymentRows(List<PaymentEntity> payments) {
        List<List<Object>> rows = new ArrayList<>(payments == null ? 0 : payments.size());
        if (payments == null) {
            return rows;
        }
        for (PaymentEntity item : payments) {
            rows.add(List.of(
                money(safeDouble(item.getAmount())),
                methodLabel(item.getMethod()),
                typeLabel(item.getType()),
                safeText(item.getReferenceNo(), "-")
            ));
        }
        return rows;
    }

    private List<List<Object>> buildReturnRows(List<SalesReturnEntity> returns) {
        List<List<Object>> rows = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return rows;
        }
        for (SalesReturnEntity item : returns) {
            rows.add(List.of(
                safeText(item.getReturnNo(), "-"),
                safeText(item.getCustomerName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getRefundAmount())),
                returnStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildPaymentSummaries(List<PaymentEntity> payments) {
        List<Map<String, Object>> items = new ArrayList<>(payments == null ? 0 : payments.size());
        if (payments == null) {
            return items;
        }
        for (PaymentEntity item : payments) {
            items.add(mapOf(
                "amount", money(safeDouble(item.getAmount())),
                "method", methodLabel(item.getMethod()),
                "type", typeLabel(item.getType()),
                "reference_no", safeText(item.getReferenceNo(), "-")
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildReturnSummaries(List<SalesReturnEntity> returns) {
        List<Map<String, Object>> items = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return items;
        }
        for (SalesReturnEntity item : returns) {
            items.add(mapOf(
                "return_no", safeText(item.getReturnNo(), ""),
                "customer_name", safeText(item.getCustomerName(), ""),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "refund_amount", money(safeDouble(item.getRefundAmount())),
                "status", returnStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private double sumReturnRefund(List<SalesReturnEntity> returns) {
        double total = 0D;
        if (returns == null) {
            return total;
        }
        for (SalesReturnEntity item : returns) {
            total += safeDouble(item.getRefundAmount());
        }
        return total;
    }

    private double sumReturnTotal(List<SalesReturnEntity> returns) {
        double total = 0D;
        if (returns == null) {
            return total;
        }
        for (SalesReturnEntity item : returns) {
            total += safeDouble(item.getTotalAmount());
        }
        return total;
    }

    private double sumReceivedPayments(List<PaymentEntity> payments) {
        double total = 0D;
        if (payments == null) {
            return total;
        }
        for (PaymentEntity item : payments) {
            double amount = safeDouble(item.getAmount());
            if (amount > 0) {
                total += amount;
            }
        }
        return total;
    }

    private String saleOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String returnStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String methodLabel(Integer method) {
        return switch (method == null ? -1 : method) {
            case 0 -> "现金";
            case 1 -> "微信";
            case 2 -> "支付宝";
            case 3 -> "银行卡";
            default -> "其他";
        };
    }

    private String typeLabel(Integer type) {
        return switch (type == null ? -1 : type) {
            case 0 -> "付款";
            case 1 -> "收款";
            case 2 -> "退款";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

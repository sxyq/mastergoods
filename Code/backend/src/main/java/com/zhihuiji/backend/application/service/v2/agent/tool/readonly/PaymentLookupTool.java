package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 收付款记录查询工具。
 */
@Component
public class PaymentLookupTool extends ToolSupport {

    private final PaymentRepository paymentRepository;

    public PaymentLookupTool(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String name() {
        return "payment_lookup";
    }

    @Override
    public String displayName() {
        return "收付款记录查询";
    }

    @Override
    public String description() {
        return "查询当前账号收付款记录、回款明细与付款状态";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        addIntegerProperty(schema, "order_id", "关联订单 ID，可选");
        addStringProperty(schema, "type", "收付款类型，可选");
        addIntegerProperty(schema, "start_date", "起始时间，Unix epoch 毫秒，可选");
        addIntegerProperty(schema, "end_date", "结束时间，Unix epoch 毫秒，可选");
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long orderId = paramLong(params, "order_id", null);
        String type = paramString(params, "type");
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        Integer typeInt = parseTypeInt(type);
        Map<String, Object> input = mapOf(
            "order_id", orderId,
            "type", type == null ? "" : type,
            "start_date", startDate,
            "end_date", endDate
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<PaymentEntity> payments;
        if (orderId != null) {
            payments = paymentRepository.findByOwnerUserIdAndOrderId(ownerUserId, orderId);
        } else if (typeInt != null && startDate != null && endDate != null) {
            payments = paymentRepository.findByOwnerUserIdAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                ownerUserId,
                typeInt,
                startDate,
                endDate,
                PageRequest.of(0, DEFAULT_TOOL_LIMIT)
            );
        } else if (startDate != null && endDate != null) {
            payments = paymentRepository.findByOwnerUserIdAndCreatedAtBetween(
                ownerUserId, startDate, endDate, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        } else {
            payments = paymentRepository.findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        }
        List<PaymentEntity> limited = payments;
        List<PaymentEntity> topPayments = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double receivedAmount = 0D;
        double paidAmount = 0D;
        for (PaymentEntity item : limited) {
            double amount = safeDouble(item.getAmount());
            if (amount >= 0) {
                receivedAmount += amount;
            } else {
                paidAmount += -amount;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条收付款记录", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "收付款概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "记录条数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "收款金额", "value", money(receivedAmount), "trend_direction", receivedAmount > 0 ? "up" : "flat"),
                    mapOf("label", "付款金额", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近收付款记录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("订单ID", "金额", "方式", "类型", "单号"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "最近收付款记录 " + limited.size() + " 条，收款 " + money(receivedAmount) + "，付款 " + money(paidAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "payment_count", limited.size(),
            "received_amount", money(receivedAmount),
            "paid_amount", money(paidAmount),
            "query_audit", audit.facts(),
            "recent_payments", buildSummaries(topPayments)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<PaymentEntity> payments) {
        List<List<Object>> rows = new ArrayList<>(payments == null ? 0 : payments.size());
        if (payments == null) {
            return rows;
        }
        for (int index = 0; index < payments.size(); index += 1) {
            PaymentEntity item = payments.get(index);
            rows.add(List.of(
                String.valueOf(safeLong(item.getOrderId())),
                money(safeDouble(item.getAmount())),
                methodLabel(item.getMethod()),
                typeLabel(item.getType()),
                safeText(item.getReferenceNo(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<PaymentEntity> payments) {
        List<Map<String, Object>> items = new ArrayList<>(payments == null ? 0 : payments.size());
        if (payments == null) {
            return items;
        }
        for (int index = 0; index < payments.size(); index += 1) {
            PaymentEntity item = payments.get(index);
            items.add(mapOf(
                "order_id", safeLong(item.getOrderId()),
                "amount", money(safeDouble(item.getAmount())),
                "method", methodLabel(item.getMethod()),
                "type", typeLabel(item.getType()),
                "reference_no", safeText(item.getReferenceNo(), "-")
            ));
        }
        return items;
    }

    private Integer parseTypeInt(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return Integer.valueOf(type.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
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

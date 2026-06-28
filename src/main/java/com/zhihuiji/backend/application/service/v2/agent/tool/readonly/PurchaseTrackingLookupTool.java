package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 采购到货跟踪工具。
 *
 * <p>串联采购单及其关联入库单、采购退货单，输出完整采购链路。
 */
@Component
public class PurchaseTrackingLookupTool extends ToolSupport {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final ObjectMapper objectMapper;

    public PurchaseTrackingLookupTool(PurchaseOrderRepository purchaseOrderRepository,
                                      PurchaseReceiptRepository purchaseReceiptRepository,
                                      PurchaseReturnRepository purchaseReturnRepository,
                                      ObjectMapper objectMapper) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseReceiptRepository = purchaseReceiptRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "purchase_tracking_lookup";
    }

    @Override
    public String displayName() {
        return "采购到货跟踪";
    }

    @Override
    public String description() {
        return "查询采购单及其关联入库单、采购退货单的完整业务链路";
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
        keyword.put("description", "采购单关键词（单号或供应商名）");
        ObjectNode orderId = properties.putObject("order_id");
        orderId.put("type", "integer");
        orderId.put("description", "指定采购单 ID");
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

        PurchaseOrderEntity order = resolveOrder(ownerUserId, orderId, keyword);
        if (order == null) {
            emitToolCompleted(ctx, name(), "未匹配到采购单", audit);
            return ToolResult.empty("未匹配到采购单");
        }

        List<PurchaseReceiptEntity> receipts = purchaseReceiptRepository
            .findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, order.getId());
        List<PurchaseReturnEntity> returns = purchaseReturnRepository
            .findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, order.getId());
        List<PurchaseReceiptEntity> limitedReceipts = limit(receipts, DEFAULT_TOOL_LIMIT);
        List<PurchaseReturnEntity> limitedReturns = limit(returns, DEFAULT_TOOL_LIMIT);

        double totalAmount = safeDouble(order.getTotalAmount());
        double paidAmount = safeDouble(order.getPaidAmount());
        double receivedAmount = safeDouble(order.getReceivedAmount());
        double outstandingAmount = Math.max(0D, totalAmount - paidAmount);
        double receiptTotalAmount = sumReceiptAmount(receipts);
        double returnTotalAmount = sumReturnAmount(returns);
        double refundAmount = sumRefundAmount(returns);

        audit.markReturned(1 + limitedReceipts.size() + limitedReturns.size());
        emitToolCompleted(
            ctx,
            name(),
            "采购单 " + safeText(order.getOrderNo(), "-") + " 关联入库 " + receipts.size() + " 条、退货 " + returns.size() + " 条",
            audit
        );

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "采购链路概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "采购总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已到货", "value", money(receivedAmount), "trend_direction", receivedAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已付款", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat"),
                    mapOf("label", "待付款", "value", money(outstandingAmount), "trend_direction", outstandingAmount > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto orderBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "采购单信息",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "供应商", "总额", "已付", "已到货", "状态"),
                "rows", List.of(List.of(
                    safeText(order.getOrderNo(), "-"),
                    safeText(order.getSupplierName(), "-"),
                    money(totalAmount),
                    money(paidAmount),
                    money(receivedAmount),
                    purchaseOrderStatusLabel(order.getStatus())
                )),
                "row_count", 1
            ))
        );
        V2AgentDtos.ResultBlockDto receiptBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "关联采购入库单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("入库单号", "供应商", "金额", "状态"),
                "rows", buildReceiptRows(limitedReceipts),
                "row_count", limitedReceipts.size()
            ))
        );
        V2AgentDtos.ResultBlockDto returnBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "关联采购退货单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("退货单号", "供应商", "退货金额", "退款金额", "状态"),
                "rows", buildReturnRows(limitedReturns),
                "row_count", limitedReturns.size()
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "采购跟踪建议",
            toJsonNode(ctx, mapOf(
                "level", outstandingAmount > 0 ? "medium" : "low",
                "title", outstandingAmount > 0 ? "建议继续跟进入库与付款" : "采购链路相对完整",
                "description", buildSuggestion(order, receipts.size(), returns.size(), outstandingAmount, refundAmount),
                "affected_items", List.of(safeText(order.getOrderNo(), "-")),
                "suggested_action", buildAction(outstandingAmount, returns.size())
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, orderBlock, receiptBlock, returnBlock, riskBlock);
        String answer = "采购单 " + safeText(order.getOrderNo(), "-")
            + "（" + safeText(order.getSupplierName(), "-") + "）总额 " + money(totalAmount)
            + "，已到货 " + money(receivedAmount) + "，已付款 " + money(paidAmount)
            + "，待付款 " + money(outstandingAmount)
            + "；关联入库 " + receipts.size() + " 条，采购退货 " + returns.size() + " 条。";
        String toolSummary = "采购单 " + safeText(order.getOrderNo(), "-") + " 入库 " + receipts.size()
            + " 条、退货 " + returns.size() + " 条，待付款 " + money(outstandingAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "order_id", safeLong(order.getId()),
            "order_no", safeText(order.getOrderNo(), ""),
            "supplier_name", safeText(order.getSupplierName(), ""),
            "total_amount", money(totalAmount),
            "paid_amount", money(paidAmount),
            "received_amount", money(receivedAmount),
            "outstanding_amount", money(outstandingAmount),
            "receipt_count", receipts.size(),
            "receipt_total_amount", money(receiptTotalAmount),
            "return_count", returns.size(),
            "return_total_amount", money(returnTotalAmount),
            "refund_amount", money(refundAmount),
            "query_audit", audit.facts(),
            "recent_receipts", buildReceiptSummaries(limitedReceipts),
            "recent_returns", buildReturnSummaries(limitedReturns)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private PurchaseOrderEntity resolveOrder(Long ownerUserId, Long orderId, String keyword) {
        if (orderId != null) {
            return purchaseOrderRepository.findByIdAndOwnerUserId(orderId, ownerUserId).orElse(null);
        }
        if (StringUtils.hasText(keyword)) {
            List<PurchaseOrderEntity> matches = purchaseOrderRepository.search(ownerUserId, keyword, null);
            return matches.isEmpty() ? null : matches.get(0);
        }
        return null;
    }

    private List<List<Object>> buildReceiptRows(List<PurchaseReceiptEntity> receipts) {
        List<List<Object>> rows = new ArrayList<>(receipts == null ? 0 : receipts.size());
        if (receipts == null) {
            return rows;
        }
        for (PurchaseReceiptEntity item : receipts) {
            rows.add(List.of(
                safeText(item.getReceiptNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                receiptStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<List<Object>> buildReturnRows(List<PurchaseReturnEntity> returns) {
        List<List<Object>> rows = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return rows;
        }
        for (PurchaseReturnEntity item : returns) {
            rows.add(List.of(
                safeText(item.getReturnNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getRefundAmount())),
                returnStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildReceiptSummaries(List<PurchaseReceiptEntity> receipts) {
        List<Map<String, Object>> items = new ArrayList<>(receipts == null ? 0 : receipts.size());
        if (receipts == null) {
            return items;
        }
        for (PurchaseReceiptEntity item : receipts) {
            items.add(mapOf(
                "receipt_no", safeText(item.getReceiptNo(), ""),
                "supplier_name", safeText(item.getSupplierName(), ""),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "status", receiptStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildReturnSummaries(List<PurchaseReturnEntity> returns) {
        List<Map<String, Object>> items = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return items;
        }
        for (PurchaseReturnEntity item : returns) {
            items.add(mapOf(
                "return_no", safeText(item.getReturnNo(), ""),
                "supplier_name", safeText(item.getSupplierName(), ""),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "refund_amount", money(safeDouble(item.getRefundAmount())),
                "status", returnStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private double sumReceiptAmount(List<PurchaseReceiptEntity> receipts) {
        double total = 0D;
        if (receipts == null) {
            return total;
        }
        for (PurchaseReceiptEntity item : receipts) {
            total += safeDouble(item.getTotalAmount());
        }
        return total;
    }

    private double sumReturnAmount(List<PurchaseReturnEntity> returns) {
        double total = 0D;
        if (returns == null) {
            return total;
        }
        for (PurchaseReturnEntity item : returns) {
            total += safeDouble(item.getTotalAmount());
        }
        return total;
    }

    private double sumRefundAmount(List<PurchaseReturnEntity> returns) {
        double total = 0D;
        if (returns == null) {
            return total;
        }
        for (PurchaseReturnEntity item : returns) {
            total += safeDouble(item.getRefundAmount());
        }
        return total;
    }

    private String buildSuggestion(PurchaseOrderEntity order, int receiptCount, int returnCount, double outstandingAmount, double refundAmount) {
        if (outstandingAmount <= 0D && returnCount == 0) {
            return "该采购单已基本完成到货与结算，可继续观察后续补货节奏。";
        }
        return "采购单「" + safeText(order.getOrderNo(), "-") + "」当前关联入库 " + receiptCount
            + " 条、退货 " + returnCount + " 条，待付款 " + money(outstandingAmount)
            + (refundAmount > 0 ? "，并已产生退款 " + money(refundAmount) : "")
            + "，建议继续核对到货与退款节奏。";
    }

    private String buildAction(double outstandingAmount, int returnCount) {
        if (outstandingAmount > 0D) {
            return "优先核对剩余到货与付款计划，避免采购链路长期挂起";
        }
        if (returnCount > 0) {
            return "复核退货与退款是否已完整闭环";
        }
        return "保持当前采购履约节奏";
    }

    private String purchaseOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已下单";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private String receiptStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已确认";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String returnStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已确认";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

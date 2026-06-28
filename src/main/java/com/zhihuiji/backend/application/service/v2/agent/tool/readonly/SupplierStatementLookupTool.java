package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 供应商对账单工具。
 *
 * <p>结合供应商余额、采购单、退货记录生成对账单与付款建议。
 */
@Component
public class SupplierStatementLookupTool extends ToolSupport {

    private static final int CANCELLED_PURCHASE_ORDER_STATUS = 2;

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final ObjectMapper objectMapper;

    public SupplierStatementLookupTool(SupplierRepository supplierRepository,
                                       PurchaseOrderRepository purchaseOrderRepository,
                                       PurchaseReturnRepository purchaseReturnRepository,
                                       ObjectMapper objectMapper) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "supplier_statement_lookup";
    }

    @Override
    public String displayName() {
        return "供应商对账单";
    }

    @Override
    public String description() {
        return "根据供应商余额、采购单和退货记录生成对账单与付款建议";
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
        ObjectNode supplierId = properties.putObject("supplier_id");
        supplierId.put("type", "integer");
        supplierId.put("description", "供应商 ID");
        ObjectNode keyword = properties.putObject("keyword");
        keyword.put("type", "string");
        keyword.put("description", "供应商名称或手机号关键词");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long supplierId = paramLong(params, "supplier_id", null);
        String keyword = paramString(params, "keyword");
        Map<String, Object> input = mapOf(
            "supplier_id", supplierId,
            "keyword", keyword == null ? "" : keyword
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        SupplierEntity supplier = resolveSupplier(ownerUserId, supplierId, keyword);
        if (supplier == null) {
            emitToolCompleted(ctx, name(), "未匹配到供应商对账对象", audit);
            return ToolResult.empty("未匹配到供应商，请提供供应商名称、手机号或更精确的线索。");
        }

        List<PurchaseOrderEntity> orders = loadSupplierOrders(ownerUserId, supplier);
        List<PurchaseReturnEntity> returns = loadSupplierReturns(ownerUserId, orders);

        double totalPurchaseAmount = sumOrderAmount(orders);
        double totalPaidAmount = sumOrderPaid(orders);
        double balance = safeDouble(supplier.getBalance());
        double totalRefundAmount = sumReturnRefund(returns);
        long orderCount = orders.size();
        long returnCount = returns.size();
        PurchaseOrderEntity latestOrder = orders.isEmpty() ? null : orders.get(0);
        double outstandingAmount = Math.max(0D, totalPurchaseAmount - totalPaidAmount - totalRefundAmount);
        String paymentSuggestion = paymentSuggestion(balance, outstandingAmount, latestOrder, returnCount);

        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "供应商 " + safeText(supplier.getName(), "-") + " 对账单已生成", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "供应商对账概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "累计采购额", "value", money(totalPurchaseAmount), "trend_direction", totalPurchaseAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已付款", "value", money(totalPaidAmount), "trend_direction", totalPaidAmount > 0 ? "up" : "flat"),
                    mapOf("label", "当前应付", "value", money(balance), "trend_direction", balance > 0 ? "down" : "flat"),
                    mapOf("label", "退货退款", "value", money(totalRefundAmount), "trend_direction", totalRefundAmount > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto statementBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "对账明细",
            toJsonNode(ctx, mapOf(
                "headers", List.of("采购单号", "采购总额", "已付款", "状态", "下单时间"),
                "rows", buildOrderRows(orders),
                "row_count", orders.size()
            ))
        );
        V2AgentDtos.ResultBlockDto actionBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "付款与对账建议",
            toJsonNode(ctx, mapOf(
                "level", balance >= 1000D ? "high" : (balance > 0 ? "medium" : "low"),
                "title", balance > 0 ? "建议安排供应商付款" : "供应商往来相对健康",
                "description", paymentSuggestion,
                "affected_items", List.of(safeText(supplier.getName(), "-")),
                "suggested_action", paymentSuggestion
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, statementBlock, actionBlock);
        String answer = "供应商「" + safeText(supplier.getName(), "-") + "」累计采购额 " + money(totalPurchaseAmount)
            + "，已付款 " + money(totalPaidAmount) + "，当前应付 " + money(balance)
            + "，退货退款 " + money(totalRefundAmount) + "。" + paymentSuggestion;
        String toolSummary = "供应商 " + safeText(supplier.getName(), "-") + "，应付 " + money(balance) + "，采购 " + orderCount + " 笔";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "supplier_id", safeLong(supplier.getId()),
            "supplier_name", safeText(supplier.getName(), ""),
            "supplier_phone", safeText(supplier.getPhone(), ""),
            "order_count", orderCount,
            "total_purchase_amount", money(totalPurchaseAmount),
            "total_paid_amount", money(totalPaidAmount),
            "balance", money(balance),
            "outstanding_amount", money(outstandingAmount),
            "latest_order_no", latestOrder == null ? "" : safeText(latestOrder.getOrderNo(), ""),
            "latest_order_at", latestOrder == null ? 0L : safeLong(latestOrder.getCreatedAt()),
            "return_count", returnCount,
            "refund_amount", money(totalRefundAmount),
            "payment_suggestion", paymentSuggestion,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private SupplierEntity resolveSupplier(Long ownerUserId, Long supplierId, String keyword) {
        if (supplierId != null) {
            return supplierRepository.findByIdAndOwnerUserId(supplierId, ownerUserId).orElse(null);
        }
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        List<SupplierEntity> matches = supplierRepository.search(ownerUserId, keyword.trim(), null, null);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private List<PurchaseOrderEntity> loadSupplierOrders(Long ownerUserId, SupplierEntity supplier) {
        String supplierName = safeText(supplier.getName(), "");
        List<PurchaseOrderEntity> matches = purchaseOrderRepository.search(ownerUserId, supplierName, null);
        List<PurchaseOrderEntity> filtered = new ArrayList<>();
        for (PurchaseOrderEntity order : matches) {
            if (order.getStatus() != null && order.getStatus() == CANCELLED_PURCHASE_ORDER_STATUS) {
                continue;
            }
            boolean sameId = supplier.getId() != null && supplier.getId().equals(order.getSupplierId());
            boolean sameName = supplierName.equalsIgnoreCase(safeText(order.getSupplierName(), ""));
            if (sameId || sameName) {
                filtered.add(order);
            }
        }
        filtered.sort(Comparator.comparingLong((PurchaseOrderEntity item) -> safeLong(item.getCreatedAt())).reversed());
        return filtered;
    }

    private List<PurchaseReturnEntity> loadSupplierReturns(Long ownerUserId, List<PurchaseOrderEntity> orders) {
        List<PurchaseReturnEntity> returns = new ArrayList<>();
        for (PurchaseOrderEntity order : orders) {
            returns.addAll(purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(ownerUserId, order.getId()));
        }
        returns.sort(Comparator.comparingLong((PurchaseReturnEntity item) -> safeLong(item.getCreatedAt())).reversed());
        return returns;
    }

    private double sumOrderAmount(List<PurchaseOrderEntity> orders) {
        double total = 0D;
        for (PurchaseOrderEntity order : orders) {
            total += safeDouble(order.getTotalAmount());
        }
        return total;
    }

    private double sumOrderPaid(List<PurchaseOrderEntity> orders) {
        double total = 0D;
        for (PurchaseOrderEntity order : orders) {
            total += safeDouble(order.getPaidAmount());
        }
        return total;
    }

    private double sumReturnRefund(List<PurchaseReturnEntity> returns) {
        double total = 0D;
        for (PurchaseReturnEntity item : returns) {
            total += safeDouble(item.getRefundAmount());
        }
        return total;
    }

    private List<List<String>> buildOrderRows(List<PurchaseOrderEntity> orders) {
        List<List<String>> rows = new ArrayList<>();
        List<PurchaseOrderEntity> limited = limit(orders, 10);
        for (PurchaseOrderEntity order : limited) {
            rows.add(List.of(
                safeText(order.getOrderNo(), "-"),
                money(safeDouble(order.getTotalAmount())),
                money(safeDouble(order.getPaidAmount())),
                purchaseStatusText(order.getStatus()),
                String.valueOf(safeLong(order.getCreatedAt()))
            ));
        }
        return rows;
    }

    private String paymentSuggestion(double balance, double outstanding, PurchaseOrderEntity latestOrder, long returnCount) {
        if (balance <= 0D) {
            return "当前没有明显应付压力，可继续保持现有合作节奏。";
        }
        String recentSignal = latestOrder == null
            ? "最近暂无采购单"
            : "最近采购单「" + safeText(latestOrder.getOrderNo(), "-") + "」金额 " + money(safeDouble(latestOrder.getTotalAmount()));
        String returnSignal = returnCount > 0 ? "近期有退货记录，付款前建议先核对退货抵扣。" : "近期退货较少，可直接按账期安排付款。";
        if (balance >= 1000D) {
            return recentSignal + "，建议优先安排付款以维护供应商关系。" + returnSignal;
        }
        return recentSignal + "，建议在下次采购前顺带确认付款安排。" + returnSignal;
    }

    private String purchaseStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待确认";
            case 1 -> "已确认";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

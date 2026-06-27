package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 销售退货查询工具。
 */
@Component
public class SalesReturnLookupTool extends ToolSupport {

    private final SalesReturnRepository salesReturnRepository;

    public SalesReturnLookupTool(SalesReturnRepository salesReturnRepository) {
        this.salesReturnRepository = salesReturnRepository;
    }

    @Override
    public String name() {
        return "sales_return_lookup";
    }

    @Override
    public String displayName() {
        return "销售退货查询";
    }

    @Override
    public String description() {
        return "查询当前账号销售退货单、退货明细与状态";
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
        Long originalOrderId = paramLong(params, "original_order_id", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "status", status,
            "original_order_id", originalOrderId
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<SalesReturnEntity> returns;
        if (originalOrderId != null) {
            returns = salesReturnRepository.findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc(ownerUserId, originalOrderId);
        } else {
            returns = salesReturnRepository.search(ownerUserId, keyword, status);
        }
        List<SalesReturnEntity> limited = limit(returns, DEFAULT_TOOL_LIMIT);
        List<SalesReturnEntity> topReturns = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        double refundAmount = 0D;
        for (SalesReturnEntity item : limited) {
            totalAmount += safeDouble(item.getTotalAmount());
            refundAmount += safeDouble(item.getRefundAmount());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条销售退货单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "销售退货概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近退货单", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "退货总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "已退款额", "value", money(refundAmount), "trend_direction", refundAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近销售退货单",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "客户", "总额", "退款额", "状态"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = limited.isEmpty()
            ? "当前账号下还没有销售退货数据。"
            : "我查到了最近 " + limited.size() + " 条销售退货单，退货总额 "
                + money(totalAmount) + "，已退款 " + money(refundAmount) + "。";
        String toolSummary = "最近销售退货单 " + limited.size() + " 条，退货总额 " + money(totalAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "return_count", limited.size(),
            "total_amount", money(totalAmount),
            "refund_amount", money(refundAmount),
            "query_audit", audit.facts(),
            "recent_returns", buildSummaries(topReturns)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<SalesReturnEntity> returns) {
        List<List<Object>> rows = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return rows;
        }
        for (int index = 0; index < returns.size(); index += 1) {
            SalesReturnEntity item = returns.get(index);
            rows.add(List.of(
                safeText(item.getReturnNo(), "-"),
                safeText(item.getCustomerName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getRefundAmount())),
                statusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<SalesReturnEntity> returns) {
        List<Map<String, Object>> items = new ArrayList<>(returns == null ? 0 : returns.size());
        if (returns == null) {
            return items;
        }
        for (int index = 0; index < returns.size(); index += 1) {
            SalesReturnEntity item = returns.get(index);
            items.add(mapOf(
                "return_no", safeText(item.getReturnNo(), "-"),
                "customer_name", safeText(item.getCustomerName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "refund_amount", money(safeDouble(item.getRefundAmount())),
                "status", statusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private String statusLabel(Integer status) {
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

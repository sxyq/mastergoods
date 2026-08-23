package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 账户转账查询工具。
 */
@Component
public class AccountTransferLookupTool extends ToolSupport {

    private final AccountTransferRepository accountTransferRepository;

    public AccountTransferLookupTool(AccountTransferRepository accountTransferRepository) {
        this.accountTransferRepository = accountTransferRepository;
    }

    @Override
    public String name() {
        return "account_transfer_lookup";
    }

    @Override
    public String displayName() {
        return "账户转账查询";
    }

    @Override
    public String description() {
        return "查询当前账号账户转账记录、转账明细与状态";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public JsonNode parameterSchema() {
        var schema = objectSchema();
        return schema;
    }
    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Map<String, Object> input = mapOf();
        ToolAudit audit = startAudit(ctx, name(), input);

        List<AccountTransferEntity> limited = accountTransferRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        List<AccountTransferEntity> topTransfers = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = 0D;
        double totalFee = 0D;
        for (AccountTransferEntity item : limited) {
            totalAmount += safeDouble(item.getAmount());
            totalFee += safeDouble(item.getFee());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 条账户转账记录", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "账户转账概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "转账条数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "转账总额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "手续费", "value", money(totalFee), "trend_direction", totalFee > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近账户转账记录",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "转出账户", "转入账户", "金额", "状态"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String toolSummary = "最近账户转账 " + limited.size() + " 条，转账总额 " + money(totalAmount);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "transfer_count", limited.size(),
            "total_amount", money(totalAmount),
            "total_fee", money(totalFee),
            "query_audit", audit.facts(),
            "recent_transfers", buildSummaries(topTransfers)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<AccountTransferEntity> transfers) {
        List<List<Object>> rows = new ArrayList<>(transfers == null ? 0 : transfers.size());
        if (transfers == null) {
            return rows;
        }
        for (int index = 0; index < transfers.size(); index += 1) {
            AccountTransferEntity item = transfers.get(index);
            rows.add(List.of(
                safeText(item.getTransferNo(), "-"),
                String.valueOf(safeLong(item.getFromAccountId())),
                String.valueOf(safeLong(item.getToAccountId())),
                money(safeDouble(item.getAmount())),
                statusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<AccountTransferEntity> transfers) {
        List<Map<String, Object>> items = new ArrayList<>(transfers == null ? 0 : transfers.size());
        if (transfers == null) {
            return items;
        }
        for (int index = 0; index < transfers.size(); index += 1) {
            AccountTransferEntity item = transfers.get(index);
            items.add(mapOf(
                "transfer_no", safeText(item.getTransferNo(), "-"),
                "from_account_id", safeLong(item.getFromAccountId()),
                "to_account_id", safeLong(item.getToAccountId()),
                "amount", money(safeDouble(item.getAmount())),
                "fee", money(safeDouble(item.getFee())),
                "status", statusLabel(item.getStatus()),
                "created_at", safeLong(item.getCreatedAt())
            ));
        }
        return items;
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待处理";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

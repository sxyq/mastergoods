package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 账户余额查询工具。
 */
@Component
public class AccountBalanceLookupTool extends ToolSupport {

    private final AccountRepository accountRepository;

    public AccountBalanceLookupTool(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public String name() {
        return "account_balance_lookup";
    }

    @Override
    public String displayName() {
        return "账户余额查询";
    }

    @Override
    public String description() {
        return "查询当前账号资金账户、余额与账户类型";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Map<String, Object> input = mapOf();
        ToolAudit audit = startAudit(ctx, name(), input);

        List<AccountEntity> accounts = accountRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        List<AccountEntity> limited = limit(accounts, DEFAULT_TOOL_LIMIT);
        List<AccountEntity> topAccounts = limit(limited, 5);
        audit.markLimitedResult(limited.size(), DEFAULT_TOOL_LIMIT);
        double totalBalance = 0D;
        for (AccountEntity item : limited) {
            totalBalance += safeDouble(item.getBalance());
        }
        emitToolCompleted(ctx, name(), "命中 " + limited.size() + " 个资金账户", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "账户余额概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "账户总数", "value", String.valueOf(limited.size()), "trend_direction", limited.isEmpty() ? "flat" : "up"),
                    mapOf("label", "账户总余额", "value", money(totalBalance), "trend_direction", totalBalance > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "资金账户列表",
            toJsonNode(ctx, mapOf(
                "headers", List.of("账户名", "余额", "类型", "默认"),
                "rows", buildRows(limited),
                "row_count", limited.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        String answer = limited.isEmpty()
            ? "当前账号下还没有资金账户数据。"
            : "我查到了 " + limited.size() + " 个资金账户，账户总余额 " + money(totalBalance) + "。";
        String toolSummary = "资金账户 " + limited.size() + " 个，总余额 " + money(totalBalance);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "account_count", limited.size(),
            "total_balance", money(totalBalance),
            "query_audit", audit.facts(),
            "recent_accounts", buildSummaries(topAccounts)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildRows(List<AccountEntity> accounts) {
        List<List<Object>> rows = new ArrayList<>(accounts == null ? 0 : accounts.size());
        if (accounts == null) {
            return rows;
        }
        for (int index = 0; index < accounts.size(); index += 1) {
            AccountEntity item = accounts.get(index);
            rows.add(List.of(
                safeText(item.getName(), "-"),
                money(safeDouble(item.getBalance())),
                typeLabel(item.getType()),
                Boolean.TRUE.equals(item.getIsDefault()) ? "是" : "否"
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSummaries(List<AccountEntity> accounts) {
        List<Map<String, Object>> items = new ArrayList<>(accounts == null ? 0 : accounts.size());
        if (accounts == null) {
            return items;
        }
        for (int index = 0; index < accounts.size(); index += 1) {
            AccountEntity item = accounts.get(index);
            items.add(mapOf(
                "name", safeText(item.getName(), "-"),
                "balance", money(safeDouble(item.getBalance())),
                "type", typeLabel(item.getType()),
                "is_default", Boolean.TRUE.equals(item.getIsDefault())
            ));
        }
        return items;
    }

    private String typeLabel(Integer type) {
        return switch (type == null ? -1 : type) {
            case 0 -> "现金";
            case 1 -> "银行";
            case 2 -> "支付宝";
            case 3 -> "微信";
            default -> "其他";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

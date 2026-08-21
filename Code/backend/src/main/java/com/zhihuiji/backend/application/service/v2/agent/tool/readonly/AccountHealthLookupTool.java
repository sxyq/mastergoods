package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import com.zhihuiji.backend.infrastructure.repository.CashChangeRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 资金账户健康度工具。
 *
 * <p>聚合账户余额、转账、资金流水和现金变动，给出账户健康概览与风险建议。
 */
@Component
public class AccountHealthLookupTool extends ToolSupport {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final int INCOME_TYPE = 1;
    private static final int EXPENSE_TYPE = 2;

    private final AccountRepository accountRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final CashChangeRecordRepository cashChangeRecordRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final ObjectMapper objectMapper;

    public AccountHealthLookupTool(AccountRepository accountRepository,
                                   AccountTransferRepository accountTransferRepository,
                                   CashChangeRecordRepository cashChangeRecordRepository,
                                   FinanceRecordRepository financeRecordRepository,
                                   ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.accountTransferRepository = accountTransferRepository;
        this.cashChangeRecordRepository = cashChangeRecordRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "account_health_lookup";
    }

    @Override
    public String displayName() {
        return "资金账户健康度";
    }

    @Override
    public String description() {
        return "汇总资金账户余额、近期收支、账户转账与异常变动，评估账户健康度";
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
        keyword.put("description", "账户关键词（账户名或账户编码）");
        ObjectNode windowDays = properties.putObject("window_days");
        windowDays.put("type", "integer");
        windowDays.put("description", "观察窗口天数，默认 30");
        return schema;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        int windowDays = normalizeWindowDays(paramInt(params, "window_days", 30));
        long endAt = System.currentTimeMillis();
        long startAt = Instant.ofEpochMilli(endAt)
            .atZone(ZONE_ID)
            .minusDays(windowDays)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant()
            .toEpochMilli();

        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "window_days", windowDays
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<AccountEntity> allAccounts = accountRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId);
        List<AccountEntity> filteredAccounts = filterAccounts(allAccounts, keyword);
        List<AccountEntity> limitedAccounts = limit(filteredAccounts, DEFAULT_TOOL_LIMIT);
        Map<Long, AccountEntity> accountById = indexAccounts(allAccounts);

        List<AccountTransferEntity> allTransfers = accountTransferRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        List<AccountTransferEntity> recentTransfers = filterTransfers(allTransfers, keyword, accountById, startAt, endAt);
        List<AccountTransferEntity> limitedTransfers = limit(recentTransfers, DEFAULT_TOOL_LIMIT);

        List<CashChangeRecordEntity> allCashChanges = cashChangeRecordRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        List<CashChangeRecordEntity> recentCashChanges = filterCashChanges(allCashChanges, keyword, accountById, startAt, endAt);
        List<CashChangeRecordEntity> limitedCashChanges = limit(recentCashChanges, DEFAULT_TOOL_LIMIT);

        Object[] financeSummary = financeRecordRepository.cashflowSummary(ownerUserId, startAt, endAt, INCOME_TYPE, EXPENSE_TYPE);
        double totalIncome = safeDouble(financeSummary != null && financeSummary.length > 0 ? financeSummary[0] : null);
        double totalExpense = safeDouble(financeSummary != null && financeSummary.length > 1 ? financeSummary[1] : null);
        long financeRecordCount = safeLong(financeSummary != null && financeSummary.length > 2 ? financeSummary[2] : null);

        double totalBalance = sumBalances(filteredAccounts);
        double totalTransferAmount = sumTransferAmounts(recentTransfers);
        double totalTransferFee = sumTransferFees(recentTransfers);
        double totalCashChange = sumCashChanges(recentCashChanges);
        int activeAccountCount = countByStatus(filteredAccounts, 1);
        int inactiveAccountCount = Math.max(0, filteredAccounts.size() - activeAccountCount);
        int lowBalanceCount = countLowBalance(filteredAccounts);
        double incomeExpenseRatio = totalExpense <= 0D
            ? (totalIncome > 0D ? totalIncome : 0D)
            : totalIncome / totalExpense;
        AccountEntity topBalanceAccount = maxBalanceAccount(filteredAccounts);
        AccountEntity defaultAccount = findDefaultAccount(filteredAccounts);

        audit.markReturned(
            limitedAccounts.size() + limitedTransfers.size() + limitedCashChanges.size()
        );
        emitToolCompleted(
            ctx,
            name(),
            "账户 " + filteredAccounts.size() + " 个，窗口 " + windowDays + " 天内转账 " + recentTransfers.size() + " 条、资金变动 " + recentCashChanges.size() + " 条",
            audit
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(
            new V2AgentDtos.ResultBlockDto(
                "kpi_grid",
                "账户健康概览",
                toJsonNode(ctx, mapOf(
                    "kpis", List.of(
                        mapOf("label", "账户总余额", "value", money(totalBalance), "trend_direction", totalBalance > 0 ? "up" : "flat"),
                        mapOf("label", "收支比", "value", formatRatio(incomeExpenseRatio), "trend_direction", incomeExpenseRatio >= 1D ? "up" : "down"),
                        mapOf("label", "活跃账户", "value", String.valueOf(activeAccountCount), "trend_direction", activeAccountCount > 0 ? "up" : "flat"),
                        mapOf("label", "低余额账户", "value", String.valueOf(lowBalanceCount), "trend_direction", lowBalanceCount > 0 ? "down" : "flat")
                    )
                ))
            ),
            new V2AgentDtos.ResultBlockDto(
                "table",
                "重点账户健康明细",
                toJsonNode(ctx, mapOf(
                    "headers", List.of("账户", "余额", "类型", "状态", "默认", "备注"),
                    "rows", buildAccountRows(limitedAccounts),
                    "row_count", limitedAccounts.size()
                ))
            ),
            new V2AgentDtos.ResultBlockDto(
                "table",
                "近期账户转账",
                toJsonNode(ctx, mapOf(
                    "headers", List.of("单号", "转出账户", "转入账户", "金额", "手续费", "状态"),
                    "rows", buildTransferRows(limitedTransfers, accountById),
                    "row_count", limitedTransfers.size()
                ))
            ),
            new V2AgentDtos.ResultBlockDto(
                "table",
                "近期资金变动",
                toJsonNode(ctx, mapOf(
                    "headers", List.of("账户", "订单类型", "变动金额", "备注", "时间"),
                    "rows", buildCashChangeRows(limitedCashChanges, accountById),
                    "row_count", limitedCashChanges.size()
                ))
            ),
            new V2AgentDtos.ResultBlockDto(
                "risk_card",
                "账户健康建议",
                toJsonNode(ctx, mapOf(
                    "level", riskLevel(lowBalanceCount, incomeExpenseRatio, inactiveAccountCount),
                    "title", riskTitle(lowBalanceCount, incomeExpenseRatio),
                    "description", buildSuggestion(
                        filteredAccounts.size(),
                        totalBalance,
                        lowBalanceCount,
                        incomeExpenseRatio,
                        topBalanceAccount,
                        defaultAccount
                    ),
                    "affected_items", buildAffectedItems(lowBalanceCount, topBalanceAccount, defaultAccount),
                    "suggested_action", buildAction(lowBalanceCount, incomeExpenseRatio, defaultAccount)
                ))
            )
        );

        String toolSummary = "账户总余额 " + money(totalBalance)
            + "，收支比 " + formatRatio(incomeExpenseRatio)
            + "，低余额账户 " + lowBalanceCount + " 个";
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "account_count", filteredAccounts.size(),
            "active_account_count", activeAccountCount,
            "inactive_account_count", inactiveAccountCount,
            "low_balance_count", lowBalanceCount,
            "total_balance", money(totalBalance),
            "window_days", windowDays,
            "recent_income", money(totalIncome),
            "recent_expense", money(totalExpense),
            "income_expense_ratio", formatRatio(incomeExpenseRatio),
            "finance_record_count", financeRecordCount,
            "transfer_count", recentTransfers.size(),
            "transfer_total_amount", money(totalTransferAmount),
            "transfer_total_fee", money(totalTransferFee),
            "cash_change_count", recentCashChanges.size(),
            "cash_change_total_amount", money(totalCashChange),
            "default_account_name", defaultAccount == null ? "" : safeText(defaultAccount.getName(), ""),
            "top_balance_account_name", topBalanceAccount == null ? "" : safeText(topBalanceAccount.getName(), ""),
            "top_balance_account_balance", topBalanceAccount == null ? money(0D) : money(safeDouble(topBalanceAccount.getBalance())),
            "query_audit", audit.facts(),
            "recent_accounts", buildAccountSummaries(limit(filteredAccounts, 5)),
            "recent_transfers", buildTransferSummaries(limit(recentTransfers, 5), accountById),
            "recent_cash_changes", buildCashChangeSummaries(limit(recentCashChanges, 5), accountById)
        ));
        return ToolResult.success(blocks, toolFacts, toolSummary);
    }

    private int normalizeWindowDays(Integer value) {
        if (value == null || value <= 0) {
            return 30;
        }
        return Math.min(value, 180);
    }

    private List<AccountEntity> filterAccounts(List<AccountEntity> accounts, String keyword) {
        if (!StringUtils.hasText(keyword) || accounts == null || accounts.isEmpty()) {
            return accounts == null ? List.of() : accounts;
        }
        String normalized = keyword.trim().toLowerCase();
        List<AccountEntity> filtered = new ArrayList<>(accounts.size());
        for (AccountEntity account : accounts) {
            String name = account.getName() == null ? "" : account.getName().toLowerCase();
            String code = account.getCode() == null ? "" : account.getCode().toLowerCase();
            if (name.contains(normalized) || code.contains(normalized)) {
                filtered.add(account);
            }
        }
        return filtered;
    }

    private Map<Long, AccountEntity> indexAccounts(List<AccountEntity> accounts) {
        Map<Long, AccountEntity> items = new LinkedHashMap<>();
        if (accounts == null) {
            return items;
        }
        for (AccountEntity account : accounts) {
            items.put(account.getId(), account);
        }
        return items;
    }

    private List<AccountTransferEntity> filterTransfers(List<AccountTransferEntity> transfers,
                                                        String keyword,
                                                        Map<Long, AccountEntity> accountById,
                                                        long startAt,
                                                        long endAt) {
        if (transfers == null || transfers.isEmpty()) {
            return List.of();
        }
        String normalized = keyword == null ? null : keyword.trim().toLowerCase();
        List<AccountTransferEntity> filtered = new ArrayList<>(transfers.size());
        for (AccountTransferEntity transfer : transfers) {
            long createdAt = safeLong(transfer.getCreatedAt());
            if (createdAt < startAt || createdAt > endAt) {
                continue;
            }
            if (StringUtils.hasText(normalized) && !matchesTransferKeyword(transfer, normalized, accountById)) {
                continue;
            }
            filtered.add(transfer);
        }
        return filtered;
    }

    private boolean matchesTransferKeyword(AccountTransferEntity transfer, String keyword, Map<Long, AccountEntity> accountById) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String transferNo = transfer.getTransferNo() == null ? "" : transfer.getTransferNo().toLowerCase();
        String notes = transfer.getNotes() == null ? "" : transfer.getNotes().toLowerCase();
        String fromName = accountName(accountById.get(transfer.getFromAccountId())).toLowerCase();
        String toName = accountName(accountById.get(transfer.getToAccountId())).toLowerCase();
        return transferNo.contains(keyword) || notes.contains(keyword) || fromName.contains(keyword) || toName.contains(keyword);
    }

    private List<CashChangeRecordEntity> filterCashChanges(List<CashChangeRecordEntity> records,
                                                           String keyword,
                                                           Map<Long, AccountEntity> accountById,
                                                           long startAt,
                                                           long endAt) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        String normalized = keyword == null ? null : keyword.trim().toLowerCase();
        List<CashChangeRecordEntity> filtered = new ArrayList<>(records.size());
        for (CashChangeRecordEntity record : records) {
            long createdAt = safeLong(record.getCreatedAt());
            if (createdAt < startAt || createdAt > endAt) {
                continue;
            }
            if (StringUtils.hasText(normalized) && !matchesCashChangeKeyword(record, normalized, accountById)) {
                continue;
            }
            filtered.add(record);
        }
        return filtered;
    }

    private boolean matchesCashChangeKeyword(CashChangeRecordEntity record, String keyword, Map<Long, AccountEntity> accountById) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String orderType = record.getOrderType() == null ? "" : record.getOrderType().toLowerCase();
        String notes = record.getNotes() == null ? "" : record.getNotes().toLowerCase();
        String accountName = accountName(accountById.get(record.getAccountId())).toLowerCase();
        return orderType.contains(keyword) || notes.contains(keyword) || accountName.contains(keyword);
    }

    private double sumBalances(List<AccountEntity> accounts) {
        double total = 0D;
        if (accounts == null) {
            return total;
        }
        for (AccountEntity account : accounts) {
            total += safeDouble(account.getBalance());
        }
        return total;
    }

    private double sumTransferAmounts(List<AccountTransferEntity> transfers) {
        double total = 0D;
        if (transfers == null) {
            return total;
        }
        for (AccountTransferEntity transfer : transfers) {
            total += safeDouble(transfer.getAmount());
        }
        return total;
    }

    private double sumTransferFees(List<AccountTransferEntity> transfers) {
        double total = 0D;
        if (transfers == null) {
            return total;
        }
        for (AccountTransferEntity transfer : transfers) {
            total += safeDouble(transfer.getFee());
        }
        return total;
    }

    private double sumCashChanges(List<CashChangeRecordEntity> records) {
        double total = 0D;
        if (records == null) {
            return total;
        }
        for (CashChangeRecordEntity record : records) {
            total += safeDouble(record.getChangeAmount());
        }
        return total;
    }

    private int countByStatus(List<AccountEntity> accounts, int status) {
        int count = 0;
        if (accounts == null) {
            return count;
        }
        for (AccountEntity account : accounts) {
            if (account.getStatus() != null && account.getStatus() == status) {
                count += 1;
            }
        }
        return count;
    }

    private int countLowBalance(List<AccountEntity> accounts) {
        int count = 0;
        if (accounts == null) {
            return count;
        }
        for (AccountEntity account : accounts) {
            if (safeDouble(account.getBalance()) <= 0D) {
                count += 1;
            }
        }
        return count;
    }

    private AccountEntity maxBalanceAccount(List<AccountEntity> accounts) {
        AccountEntity candidate = null;
        if (accounts == null) {
            return null;
        }
        for (AccountEntity account : accounts) {
            if (candidate == null || safeDouble(account.getBalance()) > safeDouble(candidate.getBalance())) {
                candidate = account;
            }
        }
        return candidate;
    }

    private AccountEntity findDefaultAccount(List<AccountEntity> accounts) {
        if (accounts == null) {
            return null;
        }
        for (AccountEntity account : accounts) {
            if (Boolean.TRUE.equals(account.getIsDefault())) {
                return account;
            }
        }
        return null;
    }

    private List<List<Object>> buildAccountRows(List<AccountEntity> accounts) {
        List<List<Object>> rows = new ArrayList<>(accounts == null ? 0 : accounts.size());
        if (accounts == null) {
            return rows;
        }
        for (AccountEntity account : accounts) {
            rows.add(List.of(
                safeText(account.getName(), "-"),
                money(safeDouble(account.getBalance())),
                accountTypeLabel(account.getType()),
                accountStatusLabel(account.getStatus()),
                Boolean.TRUE.equals(account.getIsDefault()) ? "是" : "否",
                safeText(account.getNotes(), "-")
            ));
        }
        return rows;
    }

    private List<List<Object>> buildTransferRows(List<AccountTransferEntity> transfers, Map<Long, AccountEntity> accountById) {
        List<List<Object>> rows = new ArrayList<>(transfers == null ? 0 : transfers.size());
        if (transfers == null) {
            return rows;
        }
        for (AccountTransferEntity transfer : transfers) {
            rows.add(List.of(
                safeText(transfer.getTransferNo(), "-"),
                accountName(accountById.get(transfer.getFromAccountId())),
                accountName(accountById.get(transfer.getToAccountId())),
                money(safeDouble(transfer.getAmount())),
                money(safeDouble(transfer.getFee())),
                transferStatusLabel(transfer.getStatus())
            ));
        }
        return rows;
    }

    private List<List<Object>> buildCashChangeRows(List<CashChangeRecordEntity> records, Map<Long, AccountEntity> accountById) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (CashChangeRecordEntity record : records) {
            rows.add(List.of(
                accountName(accountById.get(record.getAccountId())),
                safeText(record.getOrderType(), "-"),
                money(safeDouble(record.getChangeAmount())),
                safeText(record.getNotes(), "-"),
                formatTimestamp(record.getCreatedAt())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildAccountSummaries(List<AccountEntity> accounts) {
        List<Map<String, Object>> items = new ArrayList<>(accounts == null ? 0 : accounts.size());
        if (accounts == null) {
            return items;
        }
        for (AccountEntity account : accounts) {
            items.add(mapOf(
                "name", safeText(account.getName(), ""),
                "code", safeText(account.getCode(), ""),
                "balance", money(safeDouble(account.getBalance())),
                "type", accountTypeLabel(account.getType()),
                "status", accountStatusLabel(account.getStatus()),
                "is_default", Boolean.TRUE.equals(account.getIsDefault())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildTransferSummaries(List<AccountTransferEntity> transfers, Map<Long, AccountEntity> accountById) {
        List<Map<String, Object>> items = new ArrayList<>(transfers == null ? 0 : transfers.size());
        if (transfers == null) {
            return items;
        }
        for (AccountTransferEntity transfer : transfers) {
            items.add(mapOf(
                "transfer_no", safeText(transfer.getTransferNo(), ""),
                "from_account_name", accountName(accountById.get(transfer.getFromAccountId())),
                "to_account_name", accountName(accountById.get(transfer.getToAccountId())),
                "amount", money(safeDouble(transfer.getAmount())),
                "fee", money(safeDouble(transfer.getFee())),
                "status", transferStatusLabel(transfer.getStatus()),
                "created_at", safeLong(transfer.getCreatedAt())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildCashChangeSummaries(List<CashChangeRecordEntity> records, Map<Long, AccountEntity> accountById) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (CashChangeRecordEntity record : records) {
            items.add(mapOf(
                "account_name", accountName(accountById.get(record.getAccountId())),
                "order_type", safeText(record.getOrderType(), ""),
                "change_amount", money(safeDouble(record.getChangeAmount())),
                "notes", safeText(record.getNotes(), ""),
                "created_at", safeLong(record.getCreatedAt())
            ));
        }
        return items;
    }

    private String buildSuggestion(int accountCount,
                                   double totalBalance,
                                   int lowBalanceCount,
                                   double incomeExpenseRatio,
                                   AccountEntity topBalanceAccount,
                                   AccountEntity defaultAccount) {
        StringBuilder builder = new StringBuilder();
        if (accountCount == 0) {
            return "当前还没有资金账户数据，建议先补齐基础账户后再观察健康度。";
        }
        builder.append("当前账户总余额 ").append(money(totalBalance)).append("。");
        if (lowBalanceCount > 0) {
            builder.append("有 ").append(lowBalanceCount).append(" 个账户余额偏低，建议优先核对备用金或沉淀资金。");
        } else {
            builder.append("暂未发现低余额账户。");
        }
        if (incomeExpenseRatio < 1D) {
            builder.append("近窗口支出高于收入，建议检查回款与费用节奏。");
        } else {
            builder.append("近窗口收入覆盖支出，现金流相对健康。");
        }
        if (topBalanceAccount != null) {
            builder.append("余额最高账户为「")
                .append(safeText(topBalanceAccount.getName(), "-"))
                .append("」，可复核其资金集中度。");
        }
        if (defaultAccount != null) {
            builder.append("默认账户为「")
                .append(safeText(defaultAccount.getName(), "-"))
                .append("」。");
        }
        return builder.toString();
    }

    private List<String> buildAffectedItems(int lowBalanceCount, AccountEntity topBalanceAccount, AccountEntity defaultAccount) {
        List<String> items = new ArrayList<>();
        if (lowBalanceCount > 0) {
            items.add("低余额账户 " + lowBalanceCount + " 个");
        }
        if (topBalanceAccount != null) {
            items.add("高余额账户：" + safeText(topBalanceAccount.getName(), "-"));
        }
        if (defaultAccount != null) {
            items.add("默认账户：" + safeText(defaultAccount.getName(), "-"));
        }
        return items;
    }

    private String buildAction(int lowBalanceCount, double incomeExpenseRatio, AccountEntity defaultAccount) {
        if (lowBalanceCount > 0) {
            return "优先检查低余额账户的备用金、转账补充和默认收款账户配置。";
        }
        if (incomeExpenseRatio < 1D) {
            return "优先复核近期支出分类和回款节奏，避免支出持续快于收入。";
        }
        if (defaultAccount != null) {
            return "继续关注默认账户「" + safeText(defaultAccount.getName(), "-") + "」的资金集中度与转账流向。";
        }
        return "继续按周复核账户余额、转账和资金变动趋势。";
    }

    private String riskLevel(int lowBalanceCount, double incomeExpenseRatio, int inactiveAccountCount) {
        if (lowBalanceCount > 0 || incomeExpenseRatio < 0.8D) {
            return "medium";
        }
        if (inactiveAccountCount > 0) {
            return "low";
        }
        return "info";
    }

    private String riskTitle(int lowBalanceCount, double incomeExpenseRatio) {
        if (lowBalanceCount > 0) {
            return "存在低余额账户";
        }
        if (incomeExpenseRatio < 1D) {
            return "近期支出压力偏高";
        }
        return "账户健康度相对稳定";
    }

    private String formatRatio(double ratio) {
        if (ratio <= 0D) {
            return "0.00";
        }
        return String.format(java.util.Locale.US, "%.2f", ratio);
    }

    private String accountName(AccountEntity account) {
        return account == null ? "未知账户" : safeText(account.getName(), "未知账户");
    }

    private String accountTypeLabel(Integer type) {
        return switch (type == null ? -1 : type) {
            case 0 -> "现金";
            case 1 -> "银行";
            case 2 -> "支付宝";
            case 3 -> "微信";
            default -> "其他";
        };
    }

    private String accountStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 1 -> "启用";
            case 0 -> "停用";
            default -> "未知";
        };
    }

    private String transferStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待处理";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0L) {
            return "-";
        }
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZONE_ID)
            .toLocalDateTime()
            .toString();
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

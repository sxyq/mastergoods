package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 现金流汇总查询工具，聚合收入/支出并计算净现金流。
 */
@Component
public class CashflowSummaryLookupTool extends ToolSupport {

    private static final int TYPE_INCOME = 1;
    private static final int TYPE_EXPENSE = 2;
    private static final long DEFAULT_RANGE_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private final FinanceRecordRepository financeRecordRepository;

    public CashflowSummaryLookupTool(FinanceRecordRepository financeRecordRepository) {
        this.financeRecordRepository = financeRecordRepository;
    }

    @Override
    public String name() {
        return "cashflow_summary_lookup";
    }

    @Override
    public String displayName() {
        return "现金流汇总查询";
    }

    @Override
    public String description() {
        return "查询当前账号现金流汇总、收入支出聚合与净现金流";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        Long startDate = paramLong(params, "start_date", null);
        Long endDate = paramLong(params, "end_date", null);
        long endAt = endDate == null ? System.currentTimeMillis() : endDate;
        long startAt = startDate == null ? endAt - DEFAULT_RANGE_MILLIS : startDate;
        if (startAt > endAt) {
            long tmp = startAt;
            startAt = endAt;
            endAt = tmp;
        }
        Map<String, Object> input = mapOf(
            "start_date", startAt,
            "end_date", endAt
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        Object[] row = financeRecordRepository.cashflowSummary(
            ownerUserId,
            startAt,
            endAt,
            TYPE_INCOME,
            TYPE_EXPENSE
        );
        double totalIncome = safeDouble(row == null ? null : (row.length > 0 ? row[0] : null));
        double totalExpense = safeDouble(row == null ? null : (row.length > 1 ? row[1] : null));
        long totalRecordCount = safeLong(row == null ? null : (row.length > 2 ? row[2] : null));
        double netCashflow = totalIncome - totalExpense;
        audit.markReturned(1);
        emitToolCompleted(ctx, name(), "现金流汇总完成", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "现金流汇总",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "总收入", "value", money(totalIncome), "trend_direction", totalIncome > 0 ? "up" : "flat"),
                    mapOf("label", "总支出", "value", money(totalExpense), "trend_direction", totalExpense > 0 ? "up" : "flat"),
                    mapOf("label", "净现金流", "value", money(netCashflow), "trend_direction", netCashflow > 0 ? "up" : (netCashflow < 0 ? "down" : "flat")),
                    mapOf("label", "流水笔数", "value", String.valueOf(totalRecordCount), "trend_direction", totalRecordCount > 0 ? "up" : "flat")
                )
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock);
        String answer = "区间内总收入 " + money(totalIncome) + "，总支出 " + money(totalExpense)
            + "，净现金流 " + money(netCashflow) + "，共 " + totalRecordCount + " 笔流水。";
        String toolSummary = "总收入 " + money(totalIncome) + "，总支出 " + money(totalExpense) + "，净现金流 " + money(netCashflow);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "start_date", startAt,
            "end_date", endAt,
            "total_income", money(totalIncome),
            "total_expense", money(totalExpense),
            "net_cashflow", money(netCashflow),
            "total_record_count", totalRecordCount,
            "query_audit", audit.facts()
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }
}

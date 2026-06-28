package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 资金流水查询工具，迁移自 V2AgentAiService.buildFinanceRecordResponse。
 */
@Component
public class FinanceRecordLookupTool extends ToolSupport {

    private final FinanceRecordRepository financeRecordRepository;

    public FinanceRecordLookupTool(FinanceRecordRepository financeRecordRepository) {
        this.financeRecordRepository = financeRecordRepository;
    }

    @Override
    public String name() {
        return "finance_record_lookup";
    }

    @Override
    public String displayName() {
        return "资金流水查询";
    }

    @Override
    public String description() {
        return "查询当前账号收入支出流水、分类与近期开支";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        Long ownerUserId = ctx.ownerUserId();
        String keyword = paramString(params, "keyword");
        Integer type = paramInt(params, "type", null);
        Long createdAfter = paramLong(params, "created_after", null);
        Long createdBefore = paramLong(params, "created_before", null);
        Map<String, Object> input = mapOf(
            "keyword", keyword == null ? "" : keyword,
            "type", type,
            "created_after", createdAfter,
            "created_before", createdBefore
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        List<FinanceRecordEntity> recentRecords = financeRecordRepository.search(
            ownerUserId,
            keyword,
            type,
            createdAfter,
            createdBefore,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        List<FinanceRecordEntity> topRecords = limit(recentRecords, 5);
        audit.markLimitedResult(recentRecords.size(), DEFAULT_TOOL_LIMIT);
        double income = 0D;
        double expense = 0D;
        for (FinanceRecordEntity item : recentRecords) {
            double amount = safeDouble(item.getAmount());
            if (item.getType() != null && item.getType() == 1) {
                income += amount;
            } else if (item.getType() != null && item.getType() == 2) {
                expense += amount;
            }
        }
        emitToolCompleted(ctx, name(), "命中 " + recentRecords.size() + " 条资金流水", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "资金流水概览",
            toJsonNode(ctx, mapOf(
                "kpis", List.of(
                    mapOf("label", "最近流水", "value", String.valueOf(recentRecords.size()), "trend_direction", recentRecords.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询收入", "value", money(income), "trend_direction", income > 0 ? "up" : "flat"),
                    mapOf("label", "查询支出", "value", money(expense), "trend_direction", expense > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近流水",
            toJsonNode(ctx, mapOf(
                "headers", List.of("单号", "类型", "分类", "金额", "往来方"),
                "rows", buildFinanceRecordRows(recentRecords),
                "row_count", recentRecords.size()
            ))
        );
        V2AgentDtos.ResultBlockDto donutBlock = new V2AgentDtos.ResultBlockDto(
            "donut_chart",
            "收入支出占比",
            toJsonNode(ctx, mapOf(
                "title", "收入支出占比",
                "segments", List.of(
                    mapOf("name", "收入", "value", income, "color", "#34A853"),
                    mapOf("name", "支出", "value", expense, "color", "#FB8C00")
                )
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, donutBlock, tableBlock);
        if (recentRecords.isEmpty() && hasMeaningfulFilter(keyword, type, createdAfter, createdBefore)) {
            return ToolResult.emptyInsufficient("按当前条件未匹配到资金流水，建议放宽筛选后重试");
        }
        String answer = recentRecords.isEmpty()
            ? "当前账号下还没有资金流水数据。"
            : "我查到了最近 " + recentRecords.size() + " 条资金流水，查询收入 "
                + money(income) + "，查询支出 " + money(expense) + "。";
        String toolSummary = "最近流水 " + recentRecords.size() + " 条，收入 " + money(income) + "，支出 " + money(expense);
        JsonNode toolFacts = toJsonNode(ctx, mapOf(
            "record_count", recentRecords.size(),
            "recent_income", money(income),
            "recent_expense", money(expense),
            "query_audit", audit.facts(),
            "recent_records", buildFinanceRecordSummaries(topRecords)
        ));
        return ToolResult.success(answer, blocks, toolFacts, toolSummary);
    }

    private List<List<Object>> buildFinanceRecordRows(List<FinanceRecordEntity> records) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (int index = 0; index < records.size(); index += 1) {
            FinanceRecordEntity item = records.get(index);
            rows.add(List.of(
                safeText(item.getRecordNo(), "-"),
                financeTypeLabel(item.getType()),
                safeText(item.getCategory(), "-"),
                money(safeDouble(item.getAmount())),
                safeText(item.getPartnerName(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildFinanceRecordSummaries(List<FinanceRecordEntity> records) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (int index = 0; index < records.size(); index += 1) {
            FinanceRecordEntity item = records.get(index);
            items.add(mapOf(
                "record_no", safeText(item.getRecordNo(), "-"),
                "type", financeTypeLabel(item.getType()),
                "category", safeText(item.getCategory(), "-"),
                "amount", money(safeDouble(item.getAmount())),
                "partner_name", safeText(item.getPartnerName(), "-")
            ));
        }
        return items;
    }

    private String financeTypeLabel(Integer type) {
        return switch (type == null ? -1 : type) {
            case 1 -> "收入";
            case 2 -> "支出";
            default -> "未知";
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean hasMeaningfulFilter(String keyword, Integer type, Long createdAfter, Long createdBefore) {
        return StringUtils.hasText(keyword) || type != null || createdAfter != null || createdBefore != null;
    }
}

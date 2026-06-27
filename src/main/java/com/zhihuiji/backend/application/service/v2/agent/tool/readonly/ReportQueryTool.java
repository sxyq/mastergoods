package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.ReportService;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolSupport;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 报表查询工具，根据报表类型调用 ReportService 获取经营报表数据。
 */
@Component
public class ReportQueryTool extends ToolSupport {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})-Q([1-4])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})$");
    private static final int REPORT_LIST_LIMIT = 10;

    private final ReportService reportService;

    public ReportQueryTool(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public String name() {
        return "report_query";
    }

    @Override
    public String displayName() {
        return "报表查询";
    }

    @Override
    public String description() {
        return "查询当前账号经营报表、销售/采购/库存/财务报表数据";
    }

    @Override
    public ToolType type() {
        return ToolType.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolContext ctx, JsonNode params) {
        String reportType = paramString(params, "report_type");
        String period = paramString(params, "period");
        Map<String, Object> input = mapOf(
            "report_type", reportType == null ? "" : reportType,
            "period", period == null ? "" : period
        );
        ToolAudit audit = startAudit(ctx, name(), input);

        if (!StringUtils.hasText(reportType)) {
            emitToolFailed(ctx, name(), "report_type 参数为空");
            String answer = "请提供 report_type 参数后再查询报表，支持 sales_summary / profit_summary / cashflow_summary / reconciliation_summary / top_products / customer_sales 等。";
            V2AgentDtos.ResultBlockDto hintBlock = new V2AgentDtos.ResultBlockDto(
                "table",
                "支持的报表类型",
                toJsonNode(ctx, mapOf(
                    "headers", List.of("报表类型", "说明"),
                    "rows", List.of(
                        List.of("sales_summary", "销售汇总报表"),
                        List.of("profit_summary", "利润汇总报表"),
                        List.of("cashflow_summary", "现金流汇总报表"),
                        List.of("reconciliation_summary", "往来对账汇总报表"),
                        List.of("top_products", "热销商品报表"),
                        List.of("customer_sales", "客户销售报表"),
                        List.of("profit_by_products", "按商品利润报表"),
                        List.of("profit_by_customers", "按客户利润报表")
                    ),
                    "row_count", 8
                ))
            );
            return ToolResult.success(answer, List.of(hintBlock),
                toJsonNode(ctx, mapOf("query_audit", audit.facts(), "error", "missing_report_type")),
                "报表类型未指定");
        }

        TimeRange range = parsePeriod(period);
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        String answer;
        String toolSummary;
        Map<String, Object> facts;
        try {
            switch (reportType.toLowerCase()) {
                case "sales_summary" -> {
                    ReportDto.SalesSummaryReportDto dto = reportService.salesSummary(range.startAt, range.endAt);
                    blocks.add(buildKpiBlock(ctx, "销售汇总报表", List.of(
                        mapOf("label", "销售额", "value", money(dto.totalSalesAmount()), "trend_direction", dto.totalSalesAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "已收", "value", money(dto.totalPaidAmount()), "trend_direction", "flat"),
                        mapOf("label", "退款", "value", money(dto.totalRefundAmount()), "trend_direction", "flat"),
                        mapOf("label", "未收", "value", money(dto.totalUnpaidAmount()), "trend_direction", dto.totalUnpaidAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "订单数", "value", String.valueOf(dto.totalOrderCount()), "trend_direction", dto.totalOrderCount() > 0 ? "up" : "flat")
                    )));
                    answer = "销售汇总：销售额 " + money(dto.totalSalesAmount()) + "，已收 " + money(dto.totalPaidAmount())
                        + "，未收 " + money(dto.totalUnpaidAmount()) + "，订单数 " + dto.totalOrderCount() + "。";
                    toolSummary = "销售汇总 销售额 " + money(dto.totalSalesAmount());
                    facts = mapOf("report_type", "sales_summary", "total_sales", money(dto.totalSalesAmount()),
                        "total_paid", money(dto.totalPaidAmount()), "total_unpaid", money(dto.totalUnpaidAmount()),
                        "order_count", dto.totalOrderCount(), "query_audit", audit.facts());
                }
                case "profit_summary" -> {
                    ReportDto.ProfitSummaryReportDto dto = reportService.profitSummary(range.startAt, range.endAt);
                    blocks.add(buildKpiBlock(ctx, "利润汇总报表", List.of(
                        mapOf("label", "估算成本", "value", money(dto.estimatedCostAmount()), "trend_direction", "flat"),
                        mapOf("label", "估算利润", "value", money(dto.estimatedProfitAmount()), "trend_direction", dto.estimatedProfitAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "利润率", "value", formatNumber(dto.estimatedProfitRate()) + "%", "trend_direction", dto.estimatedProfitRate() > 0 ? "up" : "flat")
                    )));
                    answer = "利润汇总：估算成本 " + money(dto.estimatedCostAmount()) + "，估算利润 "
                        + money(dto.estimatedProfitAmount()) + "，利润率 " + formatNumber(dto.estimatedProfitRate()) + "%。";
                    toolSummary = "利润汇总 利润 " + money(dto.estimatedProfitAmount());
                    facts = mapOf("report_type", "profit_summary", "estimated_cost", money(dto.estimatedCostAmount()),
                        "estimated_profit", money(dto.estimatedProfitAmount()), "profit_rate", dto.estimatedProfitRate(),
                        "query_audit", audit.facts());
                }
                case "cashflow_summary" -> {
                    ReportDto.CashflowSummaryReportDto dto = reportService.cashflowSummary(range.startAt, range.endAt);
                    blocks.add(buildKpiBlock(ctx, "现金流汇总报表", List.of(
                        mapOf("label", "总收入", "value", money(dto.totalIncomeAmount()), "trend_direction", dto.totalIncomeAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "总支出", "value", money(dto.totalExpenseAmount()), "trend_direction", dto.totalExpenseAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "净现金流", "value", money(dto.netCashFlow()), "trend_direction", dto.netCashFlow() > 0 ? "up" : "flat"),
                        mapOf("label", "流水笔数", "value", String.valueOf(dto.totalRecordCount()), "trend_direction", dto.totalRecordCount() > 0 ? "up" : "flat")
                    )));
                    answer = "现金流汇总：总收入 " + money(dto.totalIncomeAmount()) + "，总支出 "
                        + money(dto.totalExpenseAmount()) + "，净现金流 " + money(dto.netCashFlow()) + "。";
                    toolSummary = "现金流汇总 净现金流 " + money(dto.netCashFlow());
                    facts = mapOf("report_type", "cashflow_summary", "total_income", money(dto.totalIncomeAmount()),
                        "total_expense", money(dto.totalExpenseAmount()), "net_cashflow", money(dto.netCashFlow()),
                        "record_count", dto.totalRecordCount(), "query_audit", audit.facts());
                }
                case "reconciliation_summary" -> {
                    ReportDto.ReconciliationSummaryReportDto dto = reportService.reconciliationSummary(range.startAt, range.endAt);
                    blocks.add(buildKpiBlock(ctx, "往来对账汇总报表", List.of(
                        mapOf("label", "应收款", "value", money(dto.totalReceivableAmount()), "trend_direction", dto.totalReceivableAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "应付款", "value", money(dto.totalPayableAmount()), "trend_direction", dto.totalPayableAmount() > 0 ? "up" : "flat"),
                        mapOf("label", "已收", "value", money(dto.totalReceivedAmount()), "trend_direction", "flat"),
                        mapOf("label", "已付", "value", money(dto.totalPaidAmount()), "trend_direction", "flat"),
                        mapOf("label", "净额", "value", money(dto.netCashFlow()), "trend_direction", dto.netCashFlow() > 0 ? "up" : "flat")
                    )));
                    answer = "往来对账汇总：应收 " + money(dto.totalReceivableAmount()) + "，应付 "
                        + money(dto.totalPayableAmount()) + "，已收 " + money(dto.totalReceivedAmount())
                        + "，已付 " + money(dto.totalPaidAmount()) + "。";
                    toolSummary = "往来对账 应收 " + money(dto.totalReceivableAmount()) + "，应付 " + money(dto.totalPayableAmount());
                    facts = mapOf("report_type", "reconciliation_summary",
                        "total_receivable", money(dto.totalReceivableAmount()),
                        "total_payable", money(dto.totalPayableAmount()),
                        "total_received", money(dto.totalReceivedAmount()),
                        "total_paid", money(dto.totalPaidAmount()),
                        "query_audit", audit.facts());
                }
                case "top_products" -> {
                    List<ReportDto.TopSellingProductReportDto> rows = reportService.topProducts(range.startAt, range.endAt, REPORT_LIST_LIMIT);
                    blocks.add(buildProductSalesTable(ctx, "热销商品报表", rows));
                    answer = rows.isEmpty() ? "该区间内没有热销商品数据。" : "查到 " + rows.size() + " 个热销商品。";
                    toolSummary = "热销商品 " + rows.size() + " 个";
                    facts = mapOf("report_type", "top_products", "row_count", rows.size(), "query_audit", audit.facts());
                }
                case "customer_sales" -> {
                    List<ReportDto.CustomerSalesReportDto> rows = reportService.customerSales(range.startAt, range.endAt, REPORT_LIST_LIMIT);
                    blocks.add(buildCustomerSalesTable(ctx, "客户销售报表", rows));
                    answer = rows.isEmpty() ? "该区间内没有客户销售数据。" : "查到 " + rows.size() + " 个客户销售记录。";
                    toolSummary = "客户销售 " + rows.size() + " 条";
                    facts = mapOf("report_type", "customer_sales", "row_count", rows.size(), "query_audit", audit.facts());
                }
                case "profit_by_products" -> {
                    List<ReportDto.ProfitByProductReportDto> rows = reportService.profitByProducts(range.startAt, range.endAt, REPORT_LIST_LIMIT);
                    blocks.add(buildProfitByProductTable(ctx, "按商品利润报表", rows));
                    answer = rows.isEmpty() ? "该区间内没有按商品利润数据。" : "查到 " + rows.size() + " 个商品利润记录。";
                    toolSummary = "按商品利润 " + rows.size() + " 条";
                    facts = mapOf("report_type", "profit_by_products", "row_count", rows.size(), "query_audit", audit.facts());
                }
                case "profit_by_customers" -> {
                    List<ReportDto.ProfitByCustomerReportDto> rows = reportService.profitByCustomers(range.startAt, range.endAt, REPORT_LIST_LIMIT);
                    blocks.add(buildProfitByCustomerTable(ctx, "按客户利润报表", rows));
                    answer = rows.isEmpty() ? "该区间内没有按客户利润数据。" : "查到 " + rows.size() + " 个客户利润记录。";
                    toolSummary = "按客户利润 " + rows.size() + " 条";
                    facts = mapOf("report_type", "profit_by_customers", "row_count", rows.size(), "query_audit", audit.facts());
                }
                default -> {
                    V2AgentDtos.ResultBlockDto hintBlock = new V2AgentDtos.ResultBlockDto(
                        "table", "支持的报表类型",
                        toJsonNode(ctx, mapOf(
                            "headers", List.of("报表类型", "说明"),
                            "rows", List.of(
                                List.of("sales_summary", "销售汇总报表"),
                                List.of("profit_summary", "利润汇总报表"),
                                List.of("cashflow_summary", "现金流汇总报表"),
                                List.of("reconciliation_summary", "往来对账汇总报表"),
                                List.of("top_products", "热销商品报表"),
                                List.of("customer_sales", "客户销售报表"),
                                List.of("profit_by_products", "按商品利润报表"),
                                List.of("profit_by_customers", "按客户利润报表")
                            ),
                            "row_count", 8
                        ))
                    );
                    answer = "不支持的报表类型 " + reportType + "，请参考支持的报表类型列表。";
                    toolSummary = "报表类型不支持: " + reportType;
                    facts = mapOf("query_audit", audit.facts(), "error", "unsupported_report_type", "report_type", reportType);
                    blocks.add(hintBlock);
                }
            }
        } catch (Exception ex) {
            answer = "报表服务暂不可用，请稍后重试或直接在报表页面查看。";
            toolSummary = "报表服务调用失败";
            facts = mapOf("query_audit", audit.facts(), "error", safeMessage(ex), "report_type", reportType);
        }
        emitToolCompleted(ctx, name(), toolSummary, audit);
        return ToolResult.success(answer, blocks, toJsonNode(ctx, facts), toolSummary);
    }

    private V2AgentDtos.ResultBlockDto buildKpiBlock(ToolContext ctx, String title, List<Map<String, Object>> kpis) {
        return new V2AgentDtos.ResultBlockDto("kpi_grid", title, toJsonNode(ctx, mapOf("kpis", kpis)));
    }

    private V2AgentDtos.ResultBlockDto buildProductSalesTable(ToolContext ctx, String title, List<ReportDto.TopSellingProductReportDto> rows) {
        List<List<Object>> tableRows = new ArrayList<>(rows.size());
        for (ReportDto.TopSellingProductReportDto row : rows) {
            tableRows.add(List.of(
                safeText(row.productName(), "-"),
                safeText(row.productCode(), "-"),
                formatNumber(row.totalQuantity()),
                money(row.totalAmount())
            ));
        }
        return new V2AgentDtos.ResultBlockDto("table", title, toJsonNode(ctx, mapOf(
            "headers", List.of("商品", "编码", "销量", "销售额"),
            "rows", tableRows,
            "row_count", rows.size()
        )));
    }

    private V2AgentDtos.ResultBlockDto buildCustomerSalesTable(ToolContext ctx, String title, List<ReportDto.CustomerSalesReportDto> rows) {
        List<List<Object>> tableRows = new ArrayList<>(rows.size());
        for (ReportDto.CustomerSalesReportDto row : rows) {
            tableRows.add(List.of(
                safeText(row.customerName(), "散客"),
                String.valueOf(row.totalOrders()),
                money(row.totalAmount())
            ));
        }
        return new V2AgentDtos.ResultBlockDto("table", title, toJsonNode(ctx, mapOf(
            "headers", List.of("客户", "订单数", "销售额"),
            "rows", tableRows,
            "row_count", rows.size()
        )));
    }

    private V2AgentDtos.ResultBlockDto buildProfitByProductTable(ToolContext ctx, String title, List<ReportDto.ProfitByProductReportDto> rows) {
        List<List<Object>> tableRows = new ArrayList<>(rows.size());
        for (ReportDto.ProfitByProductReportDto row : rows) {
            tableRows.add(List.of(
                safeText(row.productName(), "-"),
                money(row.totalSalesAmount()),
                money(row.totalCostAmount()),
                money(row.totalProfitAmount()),
                formatNumber(row.profitRate()) + "%"
            ));
        }
        return new V2AgentDtos.ResultBlockDto("table", title, toJsonNode(ctx, mapOf(
            "headers", List.of("商品", "销售额", "成本", "利润", "利润率"),
            "rows", tableRows,
            "row_count", rows.size()
        )));
    }

    private V2AgentDtos.ResultBlockDto buildProfitByCustomerTable(ToolContext ctx, String title, List<ReportDto.ProfitByCustomerReportDto> rows) {
        List<List<Object>> tableRows = new ArrayList<>(rows.size());
        for (ReportDto.ProfitByCustomerReportDto row : rows) {
            tableRows.add(List.of(
                safeText(row.customerName(), "散客"),
                money(row.totalSalesAmount()),
                money(row.totalCostAmount()),
                money(row.totalProfitAmount()),
                formatNumber(row.profitRate()) + "%"
            ));
        }
        return new V2AgentDtos.ResultBlockDto("table", title, toJsonNode(ctx, mapOf(
            "headers", List.of("客户", "销售额", "成本", "利润", "利润率"),
            "rows", tableRows,
            "row_count", rows.size()
        )));
    }

    private TimeRange parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            YearMonth now = YearMonth.now(REPORT_ZONE);
            return monthRange(now.getYear(), now.getMonthValue());
        }
        Matcher monthMatcher = MONTH_PATTERN.matcher(period);
        if (monthMatcher.matches()) {
            return monthRange(Integer.parseInt(monthMatcher.group(1)), Integer.parseInt(monthMatcher.group(2)));
        }
        Matcher quarterMatcher = QUARTER_PATTERN.matcher(period);
        if (quarterMatcher.matches()) {
            int year = Integer.parseInt(quarterMatcher.group(1));
            int quarter = Integer.parseInt(quarterMatcher.group(2));
            int startMonth = (quarter - 1) * 3 + 1;
            LocalDate start = LocalDate.of(year, startMonth, 1);
            LocalDate end = start.plusMonths(3).minusDays(1);
            return new TimeRange(toMillis(start), toMillis(end));
        }
        Matcher yearMatcher = YEAR_PATTERN.matcher(period);
        if (yearMatcher.matches()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            return new TimeRange(toMillis(start), toMillis(end));
        }
        YearMonth now = YearMonth.now(REPORT_ZONE);
        return monthRange(now.getYear(), now.getMonthValue());
    }

    private TimeRange monthRange(int year, int month) {
        YearMonth ym = YearMonth.of(year, Math.max(1, Math.min(12, month)));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return new TimeRange(toMillis(start), toMillis(end));
    }

    private long toMillis(LocalDate date) {
        return date.atStartOfDay(REPORT_ZONE).toInstant().toEpochMilli();
    }

    private String safeMessage(Throwable ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record TimeRange(long startAt, long endAt) {}
}

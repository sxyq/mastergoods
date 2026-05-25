package com.zhihuiji.backend.api.controller;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.application.service.ReportService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-summary")
    public ApiResponse<ReportDto.SalesSummaryReportDto> salesSummary(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt
    ) {
        return ApiResponse.success(reportService.salesSummary(startAt, endAt));
    }

    @GetMapping("/profit-summary")
    public ApiResponse<ReportDto.ProfitSummaryReportDto> profitSummary(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt
    ) {
        return ApiResponse.success(reportService.profitSummary(startAt, endAt));
    }

    @GetMapping("/refund-records")
    public ApiResponse<List<ReportDto.RefundRecordReportDto>> refundRecords(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.refundRecords(startAt, endAt, limit));
    }

    @GetMapping("/stock-out-records")
    public ApiResponse<List<ReportDto.StockOutRecordReportDto>> stockOutRecords(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.stockOutRecords(startAt, endAt, limit));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<ReportDto.TopSellingProductReportDto>> topProducts(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.topProducts(startAt, endAt, limit));
    }

    @GetMapping("/profit-by-products")
    public ApiResponse<List<ReportDto.ProfitByProductReportDto>> profitByProducts(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.profitByProducts(startAt, endAt, limit));
    }

    @GetMapping("/profit-by-customers")
    public ApiResponse<List<ReportDto.ProfitByCustomerReportDto>> profitByCustomers(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.profitByCustomers(startAt, endAt, limit));
    }

    @GetMapping("/inventory-flow")
    public ApiResponse<List<ReportDto.InventoryFlowRecordDto>> inventoryFlow(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.inventoryFlow(startAt, endAt, limit));
    }

    @GetMapping("/customer-sales")
    public ApiResponse<List<ReportDto.CustomerSalesReportDto>> customerSales(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt,
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.customerSales(startAt, endAt, limit));
    }

    @GetMapping("/top-receivable-customers")
    public ApiResponse<List<ReportDto.CustomerReceivableReportDto>> topReceivableCustomers(
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.receivables(limit));
    }

    @GetMapping("/low-stock-products")
    public ApiResponse<List<ReportDto.LowStockProductReportDto>> lowStockProducts(
        @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(reportService.lowStockProducts(limit));
    }

    @GetMapping("/reconciliation-summary")
    public ApiResponse<ReportDto.ReconciliationSummaryReportDto> reconciliationSummary(
        @RequestParam("start_at") Long startAt,
        @RequestParam("end_at") Long endAt
    ) {
        return ApiResponse.success(reportService.reconciliationSummary(startAt, endAt));
    }
}

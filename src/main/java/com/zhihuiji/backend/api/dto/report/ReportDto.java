package com.zhihuiji.backend.api.dto.report;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public final class ReportDto {
    private ReportDto() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SalesSummaryReportDto(
        long startAt,
        long endAt,
        double totalSalesAmount,
        double totalPaidAmount,
        double totalRefundAmount,
        double totalUnpaidAmount,
        int totalOrderCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SalesTrendPointReportDto(
        long startAt,
        long endAt,
        double totalSalesAmount,
        int totalOrderCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProfitSummaryReportDto(
        long startAt,
        long endAt,
        double estimatedCostAmount,
        double estimatedProfitAmount,
        double estimatedProfitRate
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RefundRecordReportDto(
        long paymentId,
        long orderId,
        String orderNo,
        String customerName,
        double refundAmount,
        int method,
        String referenceNo,
        long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StockOutRecordReportDto(
        long orderId,
        String orderNo,
        Long customerId,
        String customerName,
        long productId,
        String productCode,
        String productName,
        double quantity,
        double unitPrice,
        double amount,
        long itemCreatedAt,
        long orderCreatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TopSellingProductReportDto(
        long productId,
        String productCode,
        String productName,
        double totalQuantity,
        double totalAmount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProfitByProductReportDto(
        long productId,
        String productCode,
        String productName,
        double totalSalesAmount,
        double totalCostAmount,
        double totalProfitAmount,
        double profitRate
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProfitByCustomerReportDto(
        Long customerId,
        String customerName,
        double totalSalesAmount,
        double totalCostAmount,
        double totalProfitAmount,
        double profitRate
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InventoryFlowRecordDto(
        long orderId,
        String orderNo,
        long productId,
        String productCode,
        String productName,
        double quantity,
        int flowType,
        long flowTime,
        String customerName,
        int sourceType,
        String sourceLabel,
        String adjustReason,
        String operatorName
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CustomerSalesReportDto(
        Long customerId,
        String customerName,
        int totalOrders,
        double totalAmount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CustomerReceivableReportDto(
        long customerId,
        String customerName,
        String phone,
        double balance
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LowStockProductReportDto(
        long productId,
        String productCode,
        String productName,
        double stock,
        double safeStock
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ReconciliationSummaryReportDto(
        long startAt,
        long endAt,
        double totalReceivableAmount,
        double totalPayableAmount,
        long totalReceivableCustomerCount,
        long totalPayableSupplierCount,
        double totalReceivedAmount,
        double totalPaidAmount,
        double netCashFlow
    ) {}
}

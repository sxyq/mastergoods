package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private static final int INVENTORY_FLOW_OUT = 0;
    private static final int INVENTORY_FLOW_IN = 1;
    private static final int INVENTORY_SOURCE_SALE = 0;
    private static final int INVENTORY_SOURCE_ADJUSTMENT = 1;
    private static final long DAY_BUCKET_MILLIS = 86_400_000L;
    private static final long SIX_HOUR_BUCKET_MILLIS = 21_600_000L;
    private static final int MAX_TREND_BUCKETS = 370;

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PayOrderRepository payOrderRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final CurrentOwnerService currentOwnerService;

    public ReportService(
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        PayOrderRepository payOrderRepository,
        FinanceRecordRepository financeRecordRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.payOrderRepository = payOrderRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReportDto.SalesSummaryReportDto salesSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        Object[] salesRow = normalizeAggregateRow(saleOrderRepository.salesSummaryAggregate(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code()
        ));
        double totalSales = safeDouble(salesRow[0]);
        double totalPaid = safeDouble(salesRow[1]);
        long orderCount = safeLong(salesRow[2]);
        double totalRefund = safeDouble(paymentRepository.sumAbsoluteAmountBetweenByType(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            PaymentType.REFUND.code()
        ));
        double totalUnpaid = Math.max(0.0, totalSales - totalPaid);
        return new ReportDto.SalesSummaryReportDto(
            range.startAt(),
            range.endAt(),
            totalSales,
            totalPaid,
            totalRefund,
            totalUnpaid,
            (int) orderCount
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReportDto.SalesTrendPointReportDto> salesTrend(Long startAt, Long endAt, String bucket) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        long bucketMillis = normalizeSalesTrendBucket(bucket);
        int bucketCount = trendBucketCount(range, bucketMillis);
        Map<Long, Object[]> rowsByBucket = new HashMap<>();
        List<Object[]> bucketRows = saleOrderRepository.salesTrendBuckets(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            bucketMillis,
            OrderStatus.CANCELLED.code()
        );
        for (Object[] row : bucketRows) {
            long bucketIndex = safeLong(row[0]);
            rowsByBucket.putIfAbsent(bucketIndex, row);
        }

        List<ReportDto.SalesTrendPointReportDto> points = new ArrayList<>(bucketCount);
        for (int index = 0; index < bucketCount; index++) {
            long pointStart = range.startAt() + (bucketMillis * index);
            long pointEnd = Math.min(range.endAt(), pointStart + bucketMillis - 1L);
            Object[] row = rowsByBucket.get((long) index);
            points.add(new ReportDto.SalesTrendPointReportDto(
                pointStart,
                pointEnd,
                row == null ? 0.0 : safeDouble(row[1]),
                row == null ? 0 : safeInt(row[2])
            ));
        }
        return points;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReportDto.ProfitSummaryReportDto profitSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        Object[] row = normalizeAggregateRow(saleOrderItemRepository.profitSummary(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code()
        ));
        double totalSales = safeDouble(row[0]);
        double estimatedCost = safeDouble(row[1]);
        double estimatedProfit = totalSales - estimatedCost;
        double estimatedProfitRate = totalSales <= 0.0 ? 0.0 : (estimatedProfit / totalSales) * 100.0;
        return new ReportDto.ProfitSummaryReportDto(
            range.startAt(),
            range.endAt(),
            estimatedCost,
            estimatedProfit,
            estimatedProfitRate
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReportDto.RefundRecordReportDto> refundRecords(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<PaymentEntity> refundPayments = paymentRepository.findByOwnerUserIdAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            ownerUserId,
            PaymentType.REFUND.code(),
            range.startAt(),
            range.endAt(),
            PageRequest.of(0, safeLimit)
        );
        if (refundPayments.isEmpty()) {
            return List.of();
        }

        Set<Long> orderIds = new HashSet<>(refundPayments.size());
        for (PaymentEntity payment : refundPayments) {
            Long orderId = payment.getOrderId();
            if (orderId != null && orderId > 0L) {
                orderIds.add(orderId);
            }
        }
        Map<Long, SaleOrderEntity> orderMap = orderIds.isEmpty()
            ? Map.of()
            : buildSaleOrderMap(ownerUserId, orderIds);

        List<ReportDto.RefundRecordReportDto> rows = new ArrayList<>();
        for (PaymentEntity payment : refundPayments) {
            long orderId = safeLong(payment.getOrderId());
            SaleOrderEntity order = orderMap.get(orderId);
            rows.add(new ReportDto.RefundRecordReportDto(
                safeLong(payment.getId()),
                orderId,
                order == null ? "-" : safeString(order.getOrderNo(), "-"),
                order == null ? "散客" : safeString(order.getCustomerName(), "散客"),
                Math.abs(safeDouble(payment.getAmount())),
                payment.getMethod() == null ? 0 : payment.getMethod(),
                payment.getReferenceNo(),
                safeLong(payment.getCreatedAt())
            ));
        }
        return rows;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReportDto.StockOutRecordReportDto> stockOutRecords(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<Object[]> stockOutRows = saleOrderItemRepository.recentStockOutRows(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.StockOutRecordReportDto> rows = new ArrayList<>(stockOutRows.size());
        for (Object[] row : stockOutRows) {
            rows.add(toStockOutRecord(row));
        }
        return rows;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReportDto.TopSellingProductReportDto> topProducts(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<Object[]> topRows = saleOrderItemRepository.topProducts(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.TopSellingProductReportDto> rows = new ArrayList<>(topRows.size());
        for (Object[] row : topRows) {
            rows.add(new ReportDto.TopSellingProductReportDto(
                safeLong(row[0]),
                safeString((String) row[1], ""),
                safeString((String) row[2], ""),
                safeDouble(row[3]),
                safeDouble(row[4])
            ));
        }
        return rows;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReportDto.ProfitByProductReportDto> profitByProducts(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<Object[]> profitRows = saleOrderItemRepository.profitByProducts(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.ProfitByProductReportDto> rows = new ArrayList<>(profitRows.size());
        for (Object[] row : profitRows) {
            double totalSalesAmount = safeDouble(row[3]);
            double totalCostAmount = safeDouble(row[4]);
            double totalProfit = totalSalesAmount - totalCostAmount;
            double profitRate = totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / totalSalesAmount) * 100.0;
            rows.add(new ReportDto.ProfitByProductReportDto(
                safeLong(row[0]),
                safeString((String) row[1], ""),
                safeString((String) row[2], ""),
                totalSalesAmount,
                totalCostAmount,
                totalProfit,
                profitRate
            ));
        }
        return rows;
    }

    public List<ReportDto.ProfitByCustomerReportDto> profitByCustomers(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<Object[]> profitRows = saleOrderItemRepository.profitByCustomers(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.ProfitByCustomerReportDto> rows = new ArrayList<>(profitRows.size());
        for (Object[] row : profitRows) {
            double totalSalesAmount = safeDouble(row[2]);
            double totalCostAmount = safeDouble(row[3]);
            double totalProfit = totalSalesAmount - totalCostAmount;
            double profitRate = totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / totalSalesAmount) * 100.0;
            rows.add(new ReportDto.ProfitByCustomerReportDto(
                row[0] == null ? null : safeLong(row[0]),
                safeString((String) row[1], "散客"),
                totalSalesAmount,
                totalCostAmount,
                totalProfit,
                profitRate
            ));
        }
        return rows;
    }

    public List<ReportDto.InventoryFlowRecordDto> inventoryFlow(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<ReportDto.InventoryFlowRecordDto> rows = new ArrayList<>(safeLimit * 3);

        PageRequest page = PageRequest.of(0, safeLimit);
        List<Object[]> saleRows = saleOrderItemRepository.recentSaleInventoryFlowRows(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                page
            );
        for (Object[] row : saleRows) {
            rows.add(toSaleInventoryFlowRecord(row, INVENTORY_FLOW_OUT, false));
        }
        List<Object[]> cancelledRows = saleOrderItemRepository.recentCancelledSaleInventoryFlowRows(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                page
            );
        for (Object[] row : cancelledRows) {
            rows.add(toSaleInventoryFlowRecord(row, INVENTORY_FLOW_IN, true));
        }
        List<InventoryAdjustmentEntity> adjustments = inventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                page
            );
        for (InventoryAdjustmentEntity adjustment : adjustments) {
            rows.add(toAdjustmentInventoryFlowRecord(adjustment));
        }
        rows.sort(Comparator.comparingLong(ReportDto.InventoryFlowRecordDto::flowTime).reversed());
        return List.copyOf(rows.subList(0, Math.min(rows.size(), safeLimit)));
    }

    private ReportDto.StockOutRecordReportDto toStockOutRecord(Object[] row) {
        SaleOrderItemEntity item = (SaleOrderItemEntity) row[0];
        SaleOrderEntity order = (SaleOrderEntity) row[1];
        Long customerId = order.getCustomerId() != null ? order.getCustomerId() : item.getCustomerId();
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : item.getCustomerName();
        return new ReportDto.StockOutRecordReportDto(
            safeLong(order.getId()),
            safeString(order.getOrderNo(), "-"),
            customerId,
            customerName,
            safeLong(item.getProductId()),
            safeString(item.getProductCode(), ""),
            safeString(item.getProductName(), ""),
            safeDouble(item.getQuantity()),
            safeDouble(item.getUnitPrice()),
            safeDouble(item.getAmount()),
            safeLong(item.getCreatedAt()),
            safeLong(order.getCreatedAt())
        );
    }

    private ReportDto.InventoryFlowRecordDto toSaleInventoryFlowRecord(Object[] row, int flowType, boolean useUpdatedAt) {
        SaleOrderItemEntity item = (SaleOrderItemEntity) row[0];
        SaleOrderEntity order = (SaleOrderEntity) row[1];
        return new ReportDto.InventoryFlowRecordDto(
            safeLong(order.getId()),
            safeString(order.getOrderNo(), "-"),
            safeLong(item.getProductId()),
            safeString(item.getProductCode(), ""),
            safeString(item.getProductName(), ""),
            safeDouble(item.getQuantity()),
            flowType,
            useUpdatedAt ? safeLong(order.getUpdatedAt()) : safeLong(order.getCreatedAt()),
            order.getCustomerName(),
            INVENTORY_SOURCE_SALE,
            "销售",
            null,
            null
        );
    }

    private ReportDto.InventoryFlowRecordDto toAdjustmentInventoryFlowRecord(InventoryAdjustmentEntity adjustment) {
        long adjustmentId = safeLong(adjustment.getId());
        if (adjustmentId <= 0L) {
            adjustmentId = Math.max(1L, safeLong(adjustment.getCreatedAt()));
        }
        return new ReportDto.InventoryFlowRecordDto(
            -adjustmentId,
            "ADJ-" + adjustmentId,
            safeLong(adjustment.getProductId()),
            safeString(adjustment.getProductCode(), ""),
            safeString(adjustment.getProductName(), ""),
            safeDouble(adjustment.getQuantity()),
            adjustment.getFlowType() == null ? INVENTORY_FLOW_OUT : adjustment.getFlowType(),
            safeLong(adjustment.getCreatedAt()),
            null,
            INVENTORY_SOURCE_ADJUSTMENT,
            "库存调整",
            adjustment.getReason(),
            adjustment.getOperatorName()
        );
    }

    public List<ReportDto.CustomerSalesReportDto> customerSales(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<Object[]> customerRows = saleOrderRepository.customerSales(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.CustomerSalesReportDto> rows = new ArrayList<>(customerRows.size());
        for (Object[] row : customerRows) {
            rows.add(new ReportDto.CustomerSalesReportDto(
                row[0] == null ? null : safeLong(row[0]),
                safeString((String) row[1], "散客"),
                safeInt(row[2]),
                safeDouble(row[3])
            ));
        }
        return rows;
    }

    public List<ReportDto.CustomerReceivableReportDto> receivables(int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int safeLimit = normalizeLimit(limit);
        List<CustomerEntity> customers = customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(
            ownerUserId,
            0.0,
            PageRequest.of(0, safeLimit)
        );
        List<ReportDto.CustomerReceivableReportDto> rows = new ArrayList<>(customers.size());
        for (CustomerEntity customer : customers) {
            rows.add(new ReportDto.CustomerReceivableReportDto(
                safeLong(customer.getId()),
                safeString(customer.getName(), ""),
                safeString(customer.getPhone(), ""),
                safeDouble(customer.getBalance())
            ));
        }
        return rows;
    }

    public List<ReportDto.LowStockProductReportDto> lowStockProducts(int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int safeLimit = normalizeLimit(limit);
        List<ProductEntity> products = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, safeLimit));
        List<ReportDto.LowStockProductReportDto> rows = new ArrayList<>(products.size());
        for (ProductEntity product : products) {
            rows.add(new ReportDto.LowStockProductReportDto(
                safeLong(product.getId()),
                safeString(product.getCode(), ""),
                safeString(product.getName(), ""),
                safeDouble(product.getStock()),
                safeDouble(product.getSafeStock())
            ));
        }
        return rows;
    }

    public ReportDto.ReconciliationSummaryReportDto reconciliationSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        double totalReceivable = safeDouble(customerRepository.sumPositiveBalance(ownerUserId));
        double totalPayable = safeDouble(supplierRepository.sumPositiveBalance(ownerUserId));
        long totalReceivableCustomerCount = customerRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0);
        long totalPayableSupplierCount = supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(ownerUserId, 0.0);
        double totalReceived = safeDouble(paymentRepository.sumReceivedAmountBetween(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            PaymentType.REFUND.code()
        ));
        double totalPaid = safeDouble(payOrderRepository.sumAmountBetweenByStatus(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            PayOrderStatus.PAID.code()
        ));

        return new ReportDto.ReconciliationSummaryReportDto(
            range.startAt(),
            range.endAt(),
            totalReceivable,
            totalPayable,
            totalReceivableCustomerCount,
            totalPayableSupplierCount,
            totalReceived,
            totalPaid,
            totalReceived - totalPaid
        );
    }

    public ReportDto.CashflowSummaryReportDto cashflowSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        Object[] row = normalizeAggregateRow(financeRecordRepository.cashflowSummary(
            ownerUserId,
            range.startAt(),
            range.endAt(),
            FinanceRecordService.TYPE_INCOME,
            FinanceRecordService.TYPE_EXPENSE
        ));
        double totalIncome = safeDouble(row[0]);
        double totalExpense = safeDouble(row[1]);
        long totalRecordCount = safeLong(row[2]);
        return new ReportDto.CashflowSummaryReportDto(
            range.startAt(),
            range.endAt(),
            totalIncome,
            totalExpense,
            totalIncome - totalExpense,
            totalRecordCount
        );
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 200);
    }

    private TimeRange normalizeRange(Long startAt, Long endAt) {
        long safeStart = startAt == null ? 0L : startAt;
        long safeEnd = endAt == null ? System.currentTimeMillis() : endAt;
        if (safeStart <= safeEnd) {
            return new TimeRange(safeStart, safeEnd);
        }
        return new TimeRange(safeEnd, safeStart);
    }

    private Object[] normalizeAggregateRow(Object raw) {
        if (raw instanceof Object[] row && row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        if (raw instanceof Object[] row) {
            return row;
        }
        return new Object[] {0.0, 0.0, 0L};
    }

    private Map<Long, SaleOrderEntity> buildSaleOrderMap(Long ownerUserId, Set<Long> orderIds) {
        List<SaleOrderEntity> orders = saleOrderRepository.findAllByOwnerUserIdAndIdIn(ownerUserId, orderIds);
        Map<Long, SaleOrderEntity> orderMap = new HashMap<>(orders.size());
        for (SaleOrderEntity order : orders) {
            orderMap.put(order.getId(), order);
        }
        return orderMap;
    }

    private long normalizeSalesTrendBucket(String bucket) {
        if ("hour6".equalsIgnoreCase(bucket) || "6h".equalsIgnoreCase(bucket)) {
            return SIX_HOUR_BUCKET_MILLIS;
        }
        return DAY_BUCKET_MILLIS;
    }

    private int trendBucketCount(TimeRange range, long bucketMillis) {
        long duration = Math.max(0L, range.endAt() - range.startAt());
        long count = (duration / bucketMillis) + 1L;
        return (int) Math.max(1L, Math.min(count, MAX_TREND_BUCKETS));
    }

    private static double safeDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static int safeInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static long safeLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static String safeString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TimeRange(long startAt, long endAt) {}
}

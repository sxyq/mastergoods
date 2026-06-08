package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public ReportDto.SalesSummaryReportDto salesSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        double totalSales = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, range.startAt(), range.endAt()));
        double totalPaid = safeDouble(saleOrderRepository.sumPaidAmountBetween(ownerUserId, range.startAt(), range.endAt()));
        long orderCount = saleOrderRepository.countNonCancelledBetween(ownerUserId, range.startAt(), range.endAt());
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

    public List<ReportDto.SalesTrendPointReportDto> salesTrend(Long startAt, Long endAt, String bucket) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        long bucketMillis = normalizeSalesTrendBucket(bucket);
        int bucketCount = trendBucketCount(range, bucketMillis);
        Map<Long, Object[]> rowsByBucket = saleOrderRepository.salesTrendBuckets(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                bucketMillis,
                OrderStatus.CANCELLED.code()
            ).stream()
            .collect(Collectors.toMap(
                row -> safeLong(row[0]),
                row -> row,
                (left, ignored) -> left
            ));

        List<ReportDto.SalesTrendPointReportDto> points = new ArrayList<>();
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

    public ReportDto.ProfitSummaryReportDto profitSummary(Long startAt, Long endAt) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        List<SaleOrderEntity> orders = saleOrderRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, range.startAt(), range.endAt())
            .stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        List<SaleOrderItemEntity> items = collectOrderItems(ownerUserId, orders);
        Map<Long, Double> purchasePriceByProductId = productRepository.findAllByOwnerUserId(ownerUserId).stream()
            .filter(product -> safeLong(product.getId()) > 0L)
            .collect(Collectors.toMap(
                product -> safeLong(product.getId()),
                product -> safeDouble(product.getPurchasePrice()),
                (left, ignored) -> left
            ));
        double totalSales = items.stream().mapToDouble(item -> safeDouble(item.getAmount())).sum();
        double estimatedCost = items.stream()
            .mapToDouble(item -> safeDouble(item.getQuantity()) * purchasePriceByProductId.getOrDefault(safeLong(item.getProductId()), 0.0))
            .sum();
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

        Set<Long> orderIds = refundPayments.stream()
            .map(PaymentEntity::getOrderId)
            .filter(id -> id != null && id > 0L)
            .collect(Collectors.toSet());
        Map<Long, SaleOrderEntity> orderMap = orderIds.isEmpty()
            ? Map.of()
            : saleOrderRepository.findAllByOwnerUserId(ownerUserId).stream()
                .filter(order -> orderIds.contains(order.getId()))
                .collect(Collectors.toMap(SaleOrderEntity::getId, o -> o));

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

    public List<ReportDto.StockOutRecordReportDto> stockOutRecords(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        Map<Long, List<SaleOrderItemEntity>> itemsByOrderId = groupItemsByOrderId(collectOrderItems(ownerUserId, orders));

        List<ReportDto.StockOutRecordReportDto> rows = new ArrayList<>();
        for (SaleOrderEntity order : orders) {
            for (SaleOrderItemEntity item : itemsByOrderId.getOrDefault(safeLong(order.getId()), List.of())) {
                Long customerId = order.getCustomerId() != null ? order.getCustomerId() : item.getCustomerId();
                String customerName = order.getCustomerName() != null ? order.getCustomerName() : item.getCustomerName();
                rows.add(new ReportDto.StockOutRecordReportDto(
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
                ));
            }
        }
        return rows.stream()
            .sorted(Comparator.comparingLong(ReportDto.StockOutRecordReportDto::itemCreatedAt).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.TopSellingProductReportDto> topProducts(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        return saleOrderItemRepository.topProducts(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                PageRequest.of(0, safeLimit)
            ).stream()
            .map(row -> new ReportDto.TopSellingProductReportDto(
                safeLong(row[0]),
                safeString((String) row[1], ""),
                safeString((String) row[2], ""),
                safeDouble(row[3]),
                safeDouble(row[4])
            ))
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.ProfitByProductReportDto> profitByProducts(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        return saleOrderItemRepository.profitByProducts(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                PageRequest.of(0, safeLimit)
            ).stream()
            .map(row -> {
                double totalSalesAmount = safeDouble(row[3]);
                double totalCostAmount = safeDouble(row[4]);
                double totalProfit = totalSalesAmount - totalCostAmount;
                double profitRate = totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / totalSalesAmount) * 100.0;
                return new ReportDto.ProfitByProductReportDto(
                    safeLong(row[0]),
                    safeString((String) row[1], ""),
                    safeString((String) row[2], ""),
                    totalSalesAmount,
                    totalCostAmount,
                    totalProfit,
                    profitRate
                );
            })
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.ProfitByCustomerReportDto> profitByCustomers(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        return saleOrderItemRepository.profitByCustomers(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                PageRequest.of(0, safeLimit)
            ).stream()
            .map(row -> {
                double totalSalesAmount = safeDouble(row[2]);
                double totalCostAmount = safeDouble(row[3]);
                double totalProfit = totalSalesAmount - totalCostAmount;
                double profitRate = totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / totalSalesAmount) * 100.0;
                return new ReportDto.ProfitByCustomerReportDto(
                    row[0] == null ? null : safeLong(row[0]),
                    safeString((String) row[1], "散客"),
                    totalSalesAmount,
                    totalCostAmount,
                    totalProfit,
                    profitRate
                );
            })
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.InventoryFlowRecordDto> inventoryFlow(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<ReportDto.InventoryFlowRecordDto> rows = new ArrayList<>();

        List<SaleOrderEntity> createdOrders = saleOrderRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, range.startAt(), range.endAt());
        Map<Long, List<SaleOrderItemEntity>> createdItemsByOrderId = groupItemsByOrderId(collectOrderItems(ownerUserId, createdOrders));
        for (SaleOrderEntity order : createdOrders) {
            for (SaleOrderItemEntity item : createdItemsByOrderId.getOrDefault(safeLong(order.getId()), List.of())) {
                rows.add(new ReportDto.InventoryFlowRecordDto(
                    safeLong(order.getId()),
                    safeString(order.getOrderNo(), "-"),
                    safeLong(item.getProductId()),
                    safeString(item.getProductCode(), ""),
                    safeString(item.getProductName(), ""),
                    safeDouble(item.getQuantity()),
                    INVENTORY_FLOW_OUT,
                    safeLong(order.getCreatedAt()),
                    order.getCustomerName(),
                    INVENTORY_SOURCE_SALE,
                    "销售",
                    null,
                    null
                ));
            }
        }

        List<SaleOrderEntity> cancelledOrders = saleOrderRepository.findByOwnerUserIdAndStatusAndUpdatedAtBetween(
            ownerUserId,
            OrderStatus.CANCELLED.code(),
            range.startAt(),
            range.endAt()
        );
        Map<Long, List<SaleOrderItemEntity>> cancelledItemsByOrderId = groupItemsByOrderId(collectOrderItems(ownerUserId, cancelledOrders));
        for (SaleOrderEntity order : cancelledOrders) {
            for (SaleOrderItemEntity item : cancelledItemsByOrderId.getOrDefault(safeLong(order.getId()), List.of())) {
                rows.add(new ReportDto.InventoryFlowRecordDto(
                    safeLong(order.getId()),
                    safeString(order.getOrderNo(), "-"),
                    safeLong(item.getProductId()),
                    safeString(item.getProductCode(), ""),
                    safeString(item.getProductName(), ""),
                    safeDouble(item.getQuantity()),
                    INVENTORY_FLOW_IN,
                    safeLong(order.getUpdatedAt()),
                    order.getCustomerName(),
                    INVENTORY_SOURCE_SALE,
                    "销售",
                    null,
                    null
                ));
            }
        }

        for (InventoryAdjustmentEntity adjustment : inventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetween(ownerUserId, range.startAt(), range.endAt())) {
            long adjustmentId = safeLong(adjustment.getId());
            if (adjustmentId <= 0L) {
                adjustmentId = Math.max(1L, safeLong(adjustment.getCreatedAt()));
            }
            rows.add(new ReportDto.InventoryFlowRecordDto(
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
            ));
        }

        return rows.stream()
            .sorted(Comparator.comparingLong(ReportDto.InventoryFlowRecordDto::flowTime).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.CustomerSalesReportDto> customerSales(Long startAt, Long endAt, int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        return saleOrderRepository.customerSales(
                ownerUserId,
                range.startAt(),
                range.endAt(),
                OrderStatus.CANCELLED.code(),
                PageRequest.of(0, safeLimit)
            ).stream()
            .map(row -> new ReportDto.CustomerSalesReportDto(
                row[0] == null ? null : safeLong(row[0]),
                safeString((String) row[1], "散客"),
                safeInt(row[2]),
                safeDouble(row[3])
            ))
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.CustomerReceivableReportDto> receivables(int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int safeLimit = normalizeLimit(limit);
        return customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(ownerUserId, 0.0, PageRequest.of(0, safeLimit)).stream()
            .map(c -> new ReportDto.CustomerReceivableReportDto(
                safeLong(c.getId()),
                safeString(c.getName(), ""),
                safeString(c.getPhone(), ""),
                safeDouble(c.getBalance())
            ))
            .toList();
    }

    public List<ReportDto.LowStockProductReportDto> lowStockProducts(int limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        int safeLimit = normalizeLimit(limit);
        return productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, safeLimit)).stream()
            .map(p -> new ReportDto.LowStockProductReportDto(
                safeLong(p.getId()),
                safeString(p.getCode(), ""),
                safeString(p.getName(), ""),
                safeDouble(p.getStock()),
                safeDouble(p.getSafeStock())
            ))
            .toList();
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

    private List<SaleOrderItemEntity> collectOrderItems(Long ownerUserId, List<SaleOrderEntity> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> seenOrderIds = new HashSet<>();
        for (SaleOrderEntity order : orders) {
            long orderId = safeLong(order.getId());
            if (orderId <= 0L || seenOrderIds.contains(orderId)) {
                continue;
            }
            seenOrderIds.add(orderId);
        }
        if (seenOrderIds.isEmpty()) {
            return List.of();
        }
        return saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn(ownerUserId, seenOrderIds);
    }

    private Map<Long, List<SaleOrderItemEntity>> groupItemsByOrderId(List<SaleOrderItemEntity> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        return items.stream().collect(Collectors.groupingBy(item -> safeLong(item.getOrderId())));
    }

    private boolean isNonCancelledOrder(SaleOrderEntity order) {
        return order.getStatus() == null || order.getStatus() != OrderStatus.CANCELLED.code();
    }

    private boolean isCancelledOrder(SaleOrderEntity order) {
        return order.getStatus() != null && order.getStatus() == OrderStatus.CANCELLED.code();
    }

    private boolean isRefundPayment(PaymentEntity payment) {
        return (payment.getType() != null && payment.getType() == PaymentType.REFUND.code())
            || safeDouble(payment.getAmount()) < 0.0;
    }

    private boolean isReceivePayment(PaymentEntity payment) {
        return !isRefundPayment(payment) && safeDouble(payment.getAmount()) > 0.0;
    }

    private boolean isCompletedPayOrder(PayOrderEntity payOrder) {
        return payOrder.getStatus() != null && payOrder.getStatus() == PayOrderStatus.PAID.code();
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

    private boolean between(long value, long startAt, long endAt) {
        return value >= startAt && value <= endAt;
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

package com.zhihuiji.backend.application.service;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private static final int STATUS_CANCELLED = 2;
    private static final int PAYMENT_TYPE_REFUND = 2;
    private static final int INVENTORY_FLOW_OUT = 0;
    private static final int INVENTORY_FLOW_IN = 1;
    private static final int INVENTORY_SOURCE_SALE = 0;
    private static final int INVENTORY_SOURCE_ADJUSTMENT = 1;

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PayOrderRepository payOrderRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    public ReportService(
        SaleOrderRepository saleOrderRepository,
        SaleOrderItemRepository saleOrderItemRepository,
        PaymentRepository paymentRepository,
        PayOrderRepository payOrderRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.payOrderRepository = payOrderRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
    }

    public ReportDto.SalesSummaryReportDto salesSummary(Long startAt, Long endAt) {
        TimeRange range = normalizeRange(startAt, endAt);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt())
            .stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        double totalSales = orders.stream().mapToDouble(o -> safeDouble(o.getTotalAmount())).sum();
        double totalPaid = orders.stream().mapToDouble(o -> safeDouble(o.getPaidAmount())).sum();
        double totalUnpaid = orders.stream()
            .mapToDouble(o -> Math.max(0.0, safeDouble(o.getTotalAmount()) - safeDouble(o.getPaidAmount())))
            .sum();
        double totalRefund = paymentRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isRefundPayment)
            .mapToDouble(p -> Math.abs(safeDouble(p.getAmount())))
            .sum();
        return new ReportDto.SalesSummaryReportDto(
            range.startAt(),
            range.endAt(),
            totalSales,
            totalPaid,
            totalRefund,
            totalUnpaid,
            orders.size()
        );
    }

    public ReportDto.ProfitSummaryReportDto profitSummary(Long startAt, Long endAt) {
        TimeRange range = normalizeRange(startAt, endAt);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt())
            .stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        Map<Long, Double> purchasePriceByProductId = productRepository.findAll().stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> safeDouble(p.getPurchasePrice()), (left, right) -> left));

        double totalSales = 0.0;
        double estimatedCost = 0.0;
        for (SaleOrderItemEntity item : collectOrderItems(orders)) {
            totalSales += safeDouble(item.getAmount());
            estimatedCost += safeDouble(item.getQuantity()) * purchasePriceByProductId.getOrDefault(safeLong(item.getProductId()), 0.0);
        }
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
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<PaymentEntity> refundPayments = paymentRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isRefundPayment)
            .sorted(Comparator.comparingLong((PaymentEntity p) -> safeLong(p.getCreatedAt())).reversed())
            .limit(safeLimit)
            .toList();
        if (refundPayments.isEmpty()) {
            return List.of();
        }

        Set<Long> orderIds = refundPayments.stream()
            .map(PaymentEntity::getOrderId)
            .filter(id -> id != null && id > 0L)
            .collect(Collectors.toSet());
        Map<Long, SaleOrderEntity> orderMap = orderIds.isEmpty()
            ? Map.of()
            : saleOrderRepository.findAllById(orderIds).stream().collect(Collectors.toMap(SaleOrderEntity::getId, o -> o));

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
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();

        List<ReportDto.StockOutRecordReportDto> rows = new ArrayList<>();
        for (SaleOrderEntity order : orders) {
            List<SaleOrderItemEntity> items = saleOrderItemRepository.findByOrderId(safeLong(order.getId()));
            for (SaleOrderItemEntity item : items) {
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
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();

        Map<Long, TopProductAccumulator> agg = new HashMap<>();
        for (SaleOrderItemEntity row : collectOrderItems(orders)) {
            long productId = safeLong(row.getProductId());
            TopProductAccumulator acc = agg.computeIfAbsent(
                productId,
                key -> new TopProductAccumulator(productId, safeString(row.getProductCode(), ""), safeString(row.getProductName(), ""))
            );
            acc.totalQuantity += safeDouble(row.getQuantity());
            acc.totalAmount += safeDouble(row.getAmount());
            if (acc.productCode.isBlank() && row.getProductCode() != null) {
                acc.productCode = row.getProductCode();
            }
            if (acc.productName.isBlank() && row.getProductName() != null) {
                acc.productName = row.getProductName();
            }
        }
        return agg.values().stream()
            .map(v -> new ReportDto.TopSellingProductReportDto(v.productId, v.productCode, v.productName, v.totalQuantity, v.totalAmount))
            .sorted(Comparator.comparingDouble(ReportDto.TopSellingProductReportDto::totalAmount).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.ProfitByProductReportDto> profitByProducts(Long startAt, Long endAt, int limit) {
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        Map<Long, Double> purchasePriceByProductId = productRepository.findAll().stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> safeDouble(p.getPurchasePrice()), (left, right) -> left));

        Map<Long, ProfitByProductAccumulator> agg = new HashMap<>();
        for (SaleOrderItemEntity item : collectOrderItems(orders)) {
            long productId = safeLong(item.getProductId());
            ProfitByProductAccumulator acc = agg.computeIfAbsent(
                productId,
                key -> new ProfitByProductAccumulator(productId, safeString(item.getProductCode(), ""), safeString(item.getProductName(), ""))
            );
            acc.totalSalesAmount += safeDouble(item.getAmount());
            acc.totalCostAmount += safeDouble(item.getQuantity()) * purchasePriceByProductId.getOrDefault(productId, 0.0);
            if (acc.productCode.isBlank() && item.getProductCode() != null) {
                acc.productCode = item.getProductCode();
            }
            if (acc.productName.isBlank() && item.getProductName() != null) {
                acc.productName = item.getProductName();
            }
        }
        return agg.values().stream()
            .map(v -> {
                double totalProfit = v.totalSalesAmount - v.totalCostAmount;
                double profitRate = v.totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / v.totalSalesAmount) * 100.0;
                return new ReportDto.ProfitByProductReportDto(
                    v.productId,
                    v.productCode,
                    v.productName,
                    v.totalSalesAmount,
                    v.totalCostAmount,
                    totalProfit,
                    profitRate
                );
            })
            .sorted(Comparator.comparingDouble(ReportDto.ProfitByProductReportDto::totalProfitAmount).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.ProfitByCustomerReportDto> profitByCustomers(Long startAt, Long endAt, int limit) {
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();
        Map<Long, Double> purchasePriceByProductId = productRepository.findAll().stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> safeDouble(p.getPurchasePrice()), (left, right) -> left));

        Map<String, ProfitByCustomerAccumulator> agg = new HashMap<>();
        for (SaleOrderEntity order : orders) {
            String key = order.getCustomerId() == null ? "guest" : String.valueOf(order.getCustomerId());
            ProfitByCustomerAccumulator acc = agg.computeIfAbsent(
                key,
                k -> new ProfitByCustomerAccumulator(order.getCustomerId(), safeString(order.getCustomerName(), "散客"))
            );
            for (SaleOrderItemEntity item : saleOrderItemRepository.findByOrderId(safeLong(order.getId()))) {
                acc.totalSalesAmount += safeDouble(item.getAmount());
                acc.totalCostAmount += safeDouble(item.getQuantity()) * purchasePriceByProductId.getOrDefault(safeLong(item.getProductId()), 0.0);
            }
        }
        return agg.values().stream()
            .map(v -> {
                double totalProfit = v.totalSalesAmount - v.totalCostAmount;
                double profitRate = v.totalSalesAmount <= 0.0 ? 0.0 : (totalProfit / v.totalSalesAmount) * 100.0;
                return new ReportDto.ProfitByCustomerReportDto(
                    v.customerId,
                    v.customerName,
                    v.totalSalesAmount,
                    v.totalCostAmount,
                    totalProfit,
                    profitRate
                );
            })
            .sorted(Comparator.comparingDouble(ReportDto.ProfitByCustomerReportDto::totalProfitAmount).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.InventoryFlowRecordDto> inventoryFlow(Long startAt, Long endAt, int limit) {
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<ReportDto.InventoryFlowRecordDto> rows = new ArrayList<>();

        List<SaleOrderEntity> createdOrders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt());
        for (SaleOrderEntity order : createdOrders) {
            for (SaleOrderItemEntity item : saleOrderItemRepository.findByOrderId(safeLong(order.getId()))) {
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

        List<SaleOrderEntity> cancelledOrders = saleOrderRepository.findAll().stream()
            .filter(this::isCancelledOrder)
            .filter(order -> between(safeLong(order.getUpdatedAt()), range.startAt(), range.endAt()))
            .toList();
        for (SaleOrderEntity order : cancelledOrders) {
            for (SaleOrderItemEntity item : saleOrderItemRepository.findByOrderId(safeLong(order.getId()))) {
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

        for (InventoryAdjustmentEntity adjustment : inventoryAdjustmentRepository.findByCreatedAtBetween(range.startAt(), range.endAt())) {
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
        TimeRange range = normalizeRange(startAt, endAt);
        int safeLimit = normalizeLimit(limit);
        List<SaleOrderEntity> orders = saleOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isNonCancelledOrder)
            .toList();

        Map<String, CustomerSalesAccumulator> agg = new HashMap<>();
        for (SaleOrderEntity row : orders) {
            String key = row.getCustomerId() == null ? "guest" : String.valueOf(row.getCustomerId());
            CustomerSalesAccumulator acc = agg.computeIfAbsent(
                key,
                k -> new CustomerSalesAccumulator(row.getCustomerId(), safeString(row.getCustomerName(), "散客"))
            );
            acc.totalOrders += 1;
            acc.totalAmount += safeDouble(row.getTotalAmount());
        }
        return agg.values().stream()
            .map(v -> new ReportDto.CustomerSalesReportDto(v.customerId, v.customerName, v.totalOrders, v.totalAmount))
            .sorted(Comparator.comparingDouble(ReportDto.CustomerSalesReportDto::totalAmount).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.CustomerReceivableReportDto> receivables(int limit) {
        int safeLimit = normalizeLimit(limit);
        return customerRepository.findAll().stream()
            .filter(c -> safeDouble(c.getBalance()) > 0.0)
            .map(c -> new ReportDto.CustomerReceivableReportDto(
                safeLong(c.getId()),
                safeString(c.getName(), ""),
                safeString(c.getPhone(), ""),
                safeDouble(c.getBalance())
            ))
            .sorted(Comparator.comparingDouble(ReportDto.CustomerReceivableReportDto::balance).reversed())
            .limit(safeLimit)
            .toList();
    }

    public List<ReportDto.LowStockProductReportDto> lowStockProducts(int limit) {
        int safeLimit = normalizeLimit(limit);
        return productRepository.findAll().stream()
            .filter(p -> safeDouble(p.getStock()) <= safeDouble(p.getSafeStock()))
            .sorted(
                Comparator.comparingDouble((ProductEntity p) -> safeDouble(p.getStock()))
                    .thenComparingLong(p -> -safeLong(p.getUpdatedAt()))
            )
            .limit(safeLimit)
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
        TimeRange range = normalizeRange(startAt, endAt);
        double totalReceivable = customerRepository.findAll().stream()
            .mapToDouble(c -> Math.max(0.0, safeDouble(c.getBalance())))
            .sum();
        double totalPayable = supplierRepository.findAll().stream()
            .mapToDouble(s -> Math.max(0.0, safeDouble(s.getBalance())))
            .sum();

        double totalReceived = paymentRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isReceivePayment)
            .mapToDouble(p -> Math.max(0.0, safeDouble(p.getAmount())))
            .sum();

        double totalPaid = payOrderRepository.findByCreatedAtBetween(range.startAt(), range.endAt()).stream()
            .filter(this::isCompletedPayOrder)
            .mapToDouble(p -> Math.max(0.0, safeDouble(p.getAmount())))
            .sum();

        return new ReportDto.ReconciliationSummaryReportDto(
            range.startAt(),
            range.endAt(),
            totalReceivable,
            totalPayable,
            totalReceived,
            totalPaid,
            totalReceived - totalPaid
        );
    }

    private static final class TopProductAccumulator {
        private final long productId;
        private String productCode;
        private String productName;
        private double totalQuantity;
        private double totalAmount;

        private TopProductAccumulator(long productId, String productCode, String productName) {
            this.productId = productId;
            this.productCode = productCode;
            this.productName = productName;
        }
    }

    private static final class ProfitByProductAccumulator {
        private final long productId;
        private String productCode;
        private String productName;
        private double totalSalesAmount;
        private double totalCostAmount;

        private ProfitByProductAccumulator(long productId, String productCode, String productName) {
            this.productId = productId;
            this.productCode = productCode;
            this.productName = productName;
        }
    }

    private static final class ProfitByCustomerAccumulator {
        private final Long customerId;
        private final String customerName;
        private double totalSalesAmount;
        private double totalCostAmount;

        private ProfitByCustomerAccumulator(Long customerId, String customerName) {
            this.customerId = customerId;
            this.customerName = customerName;
        }
    }

    private static final class CustomerSalesAccumulator {
        private final Long customerId;
        private final String customerName;
        private int totalOrders;
        private double totalAmount;

        private CustomerSalesAccumulator(Long customerId, String customerName) {
            this.customerId = customerId;
            this.customerName = customerName;
        }
    }

    private List<SaleOrderItemEntity> collectOrderItems(List<SaleOrderEntity> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> seenOrderIds = new HashSet<>();
        List<SaleOrderItemEntity> rows = new ArrayList<>();
        for (SaleOrderEntity order : orders) {
            long orderId = safeLong(order.getId());
            if (orderId <= 0L || seenOrderIds.contains(orderId)) {
                continue;
            }
            seenOrderIds.add(orderId);
            rows.addAll(saleOrderItemRepository.findByOrderId(orderId));
        }
        return rows;
    }

    private boolean isNonCancelledOrder(SaleOrderEntity order) {
        return order.getStatus() == null || order.getStatus() != STATUS_CANCELLED;
    }

    private boolean isCancelledOrder(SaleOrderEntity order) {
        return order.getStatus() != null && order.getStatus() == STATUS_CANCELLED;
    }

    private boolean isRefundPayment(PaymentEntity payment) {
        return (payment.getType() != null && payment.getType() == PAYMENT_TYPE_REFUND)
            || safeDouble(payment.getAmount()) < 0.0;
    }

    private boolean isReceivePayment(PaymentEntity payment) {
        return !isRefundPayment(payment) && safeDouble(payment.getAmount()) > 0.0;
    }

    private boolean isCompletedPayOrder(PayOrderEntity payOrder) {
        return payOrder.getStatus() != null && payOrder.getStatus() == PayOrderService.STATUS_PAID;
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

    private boolean between(long value, long startAt, long endAt) {
        return value >= startAt && value <= endAt;
    }

    private static double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String safeString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TimeRange(long startAt, long endAt) {}
}

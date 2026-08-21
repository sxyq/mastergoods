package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.InventoryAdjustmentEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class ReportServiceTest {
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private SaleOrderItemRepository saleOrderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PayOrderRepository payOrderRepository;
    @Mock
    private FinanceRecordRepository financeRecordRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reportService = new ReportService(
            saleOrderRepository,
            saleOrderItemRepository,
            paymentRepository,
            payOrderRepository,
            financeRecordRepository,
            customerRepository,
            supplierRepository,
            productRepository,
            inventoryAdjustmentRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void profitSummaryIgnoresInvalidProductIdsInsteadOfFailingReport() {
        when(saleOrderItemRepository.profitSummary(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code()
        )).thenReturn(new Object[] {100.0, 24.0});

        ReportDto.ProfitSummaryReportDto result = reportService.profitSummary(0L, 2_000L);

        assertEquals(24.0, result.estimatedCostAmount());
        assertEquals(76.0, result.estimatedProfitAmount());
        assertEquals(76.0, result.estimatedProfitRate());
        verify(saleOrderItemRepository).profitSummary(1L, 0L, 2_000L, OrderStatus.CANCELLED.code());
        verify(saleOrderRepository, never()).findByOwnerUserIdAndCreatedAtBetween(1L, 0L, 2_000L);
        verify(saleOrderItemRepository, never()).findByOwnerUserIdAndOrderIdIn(1L, Set.of(10L));
        verify(productRepository, never()).findAllByOwnerUserId(1L);
    }

    @Test
    void salesSummaryUsesSingleSalesAggregateQuery() {
        when(saleOrderRepository.salesSummaryAggregate(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code()
        )).thenReturn(new Object[] {150.0, 120.0, 2L});
        when(paymentRepository.sumAbsoluteAmountBetweenByType(
            1L,
            0L,
            2_000L,
            PaymentType.REFUND.code()
        )).thenReturn(20.0);

        ReportDto.SalesSummaryReportDto result = reportService.salesSummary(0L, 2_000L);

        assertEquals(150.0, result.totalSalesAmount());
        assertEquals(120.0, result.totalPaidAmount());
        assertEquals(20.0, result.totalRefundAmount());
        assertEquals(30.0, result.totalUnpaidAmount());
        assertEquals(2, result.totalOrderCount());
        verify(saleOrderRepository).salesSummaryAggregate(1L, 0L, 2_000L, OrderStatus.CANCELLED.code());
        verify(saleOrderRepository, never()).sumTotalAmountBetween(1L, 0L, 2_000L);
        verify(saleOrderRepository, never()).sumPaidAmountBetween(1L, 0L, 2_000L);
        verify(saleOrderRepository, never()).countNonCancelledBetween(1L, 0L, 2_000L);
    }

    @Test
    void reconciliationSummaryIncludesPartnerCountsFromDatabaseAggregates() {
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(300.0);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(120.0);
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(4L);
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(2L);
        when(paymentRepository.sumReceivedAmountBetween(1L, 0L, 2_000L, PaymentType.REFUND.code()))
            .thenReturn(80.0);
        when(payOrderRepository.sumAmountBetweenByStatus(1L, 0L, 2_000L, PayOrderStatus.PAID.code()))
            .thenReturn(30.0);

        ReportDto.ReconciliationSummaryReportDto result = reportService.reconciliationSummary(0L, 2_000L);

        assertEquals(300.0, result.totalReceivableAmount());
        assertEquals(120.0, result.totalPayableAmount());
        assertEquals(4L, result.totalReceivableCustomerCount());
        assertEquals(2L, result.totalPayableSupplierCount());
        assertEquals(50.0, result.netCashFlow());
    }

    @Test
    void cashflowSummaryUsesFinanceRecordAggregatesWithoutChangingPaymentLedgerSemantics() {
        when(financeRecordRepository.cashflowSummary(
            1L,
            0L,
            2_000L,
            FinanceRecordService.TYPE_INCOME,
            FinanceRecordService.TYPE_EXPENSE
        )).thenReturn(new Object[] {200.0, 75.0, 3L});

        ReportDto.CashflowSummaryReportDto result = reportService.cashflowSummary(0L, 2_000L);

        assertEquals(0L, result.startAt());
        assertEquals(2_000L, result.endAt());
        assertEquals(200.0, result.totalIncomeAmount());
        assertEquals(75.0, result.totalExpenseAmount());
        assertEquals(125.0, result.netCashFlow());
        assertEquals(3L, result.totalRecordCount());
    }

    @Test
    void stockOutRecordsUsesPagedItemJoinInsteadOfLoadingFullOrderRange() {
        SaleOrderEntity order = saleOrder(10L, 1_000L, 1_100L, OrderStatus.COMPLETED.code());
        SaleOrderItemEntity item = saleOrderItem(1L, 10L, 20L, 2.0, 100.0, 1_200L);
        when(saleOrderItemRepository.recentStockOutRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 5)
        )).thenReturn(List.<Object[]>of(new Object[] {item, order}));

        List<ReportDto.StockOutRecordReportDto> result = reportService.stockOutRecords(0L, 2_000L, 5);

        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().orderId());
        assertEquals(20L, result.getFirst().productId());
        assertEquals(1_200L, result.getFirst().itemCreatedAt());
        verify(saleOrderItemRepository).recentStockOutRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 5)
        );
        verify(saleOrderRepository, never()).findByOwnerUserIdAndCreatedAtBetween(1L, 0L, 2_000L);
        verify(saleOrderItemRepository, never()).findByOwnerUserIdAndOrderIdIn(1L, Set.of(10L));
    }

    @Test
    void refundRecordsLoadsOnlyReferencedOrders() {
        PaymentEntity payment = refundPayment(100L, 10L, -25.0, 1_500L);
        PaymentEntity guestPayment = refundPayment(101L, null, -5.0, 1_600L);
        SaleOrderEntity order = saleOrder(10L, 1_000L, 1_100L, OrderStatus.COMPLETED.code());
        when(paymentRepository.findByOwnerUserIdAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            1L,
            PaymentType.REFUND.code(),
            0L,
            2_000L,
            PageRequest.of(0, 5)
        )).thenReturn(List.of(payment, guestPayment));
        when(saleOrderRepository.findAllByOwnerUserIdAndIdIn(1L, Set.of(10L))).thenReturn(List.of(order));

        List<ReportDto.RefundRecordReportDto> result = reportService.refundRecords(0L, 2_000L, 5);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).orderId());
        assertEquals("SO-10", result.get(0).orderNo());
        assertEquals(25.0, result.get(0).refundAmount());
        assertEquals(0L, result.get(1).orderId());
        assertEquals("-", result.get(1).orderNo());
        verify(saleOrderRepository).findAllByOwnerUserIdAndIdIn(1L, Set.of(10L));
        verify(saleOrderRepository, never()).findAllByOwnerUserId(1L);
    }

    @Test
    void inventoryFlowLoadsOnlyPagedSourcesThenMergesByFlowTime() {
        SaleOrderEntity saleOrder = saleOrder(10L, 1_000L, 1_800L, OrderStatus.COMPLETED.code());
        SaleOrderItemEntity saleItem = saleOrderItem(1L, 10L, 20L, 2.0, 100.0, 1_000L);
        SaleOrderEntity cancelledOrder = saleOrder(11L, 1_100L, 1_900L, OrderStatus.CANCELLED.code());
        SaleOrderItemEntity cancelledItem = saleOrderItem(2L, 11L, 21L, 1.0, 50.0, 1_100L);
        InventoryAdjustmentEntity adjustment = inventoryAdjustment(30L, 22L, 3.0, 1, 1_500L);
        when(saleOrderItemRepository.recentSaleInventoryFlowRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 2)
        )).thenReturn(List.<Object[]>of(new Object[] {saleItem, saleOrder}));
        when(saleOrderItemRepository.recentCancelledSaleInventoryFlowRows(
            1L,
            0L,
            2_000L,
            OrderStatus.CANCELLED.code(),
            PageRequest.of(0, 2)
        )).thenReturn(List.<Object[]>of(new Object[] {cancelledItem, cancelledOrder}));
        when(inventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            1L,
            0L,
            2_000L,
            PageRequest.of(0, 2)
        )).thenReturn(List.of(adjustment));

        List<ReportDto.InventoryFlowRecordDto> result = reportService.inventoryFlow(0L, 2_000L, 2);

        assertEquals(2, result.size());
        assertEquals(1_900L, result.get(0).flowTime());
        assertEquals(1, result.get(0).flowType());
        assertEquals(1_500L, result.get(1).flowTime());
        assertEquals(1, result.get(1).sourceType());
        verify(saleOrderRepository, never()).findByOwnerUserIdAndCreatedAtBetween(1L, 0L, 2_000L);
        verify(saleOrderRepository, never()).findByOwnerUserIdAndStatusAndUpdatedAtBetween(
            1L,
            OrderStatus.CANCELLED.code(),
            0L,
            2_000L
        );
        verify(saleOrderItemRepository, never()).findByOwnerUserIdAndOrderIdIn(any(), any());
    }

    @Test
    void salesTrendReturnsFilledSixHourBucketsFromDatabaseAggregates() {
        when(saleOrderRepository.salesTrendBuckets(
            1L,
            0L,
            86_399_999L,
            21_600_000L,
            OrderStatus.CANCELLED.code()
        )).thenReturn(List.of(
            new Object[] {0L, 120.0, 2L},
            new Object[] {2L, 80.0, 1L}
        ));

        List<ReportDto.SalesTrendPointReportDto> result = reportService.salesTrend(0L, 86_399_999L, "hour6");

        assertEquals(4, result.size());
        assertEquals(0L, result.get(0).startAt());
        assertEquals(21_599_999L, result.get(0).endAt());
        assertEquals(120.0, result.get(0).totalSalesAmount());
        assertEquals(2, result.get(0).totalOrderCount());
        assertEquals(0.0, result.get(1).totalSalesAmount());
        assertEquals(43_200_000L, result.get(2).startAt());
        assertEquals(80.0, result.get(2).totalSalesAmount());
        assertEquals(64_800_000L, result.get(3).startAt());
    }

    @Test
    void salesTrendNormalizesRangeAndUsesCancelledStatusInAggregateQuery() {
        when(saleOrderRepository.salesTrendBuckets(
            1L,
            1_000L,
            3_000L,
            86_400_000L,
            OrderStatus.CANCELLED.code()
        )).thenReturn(List.<Object[]>of(new Object[] {0L, 55.0, 1L}));

        List<ReportDto.SalesTrendPointReportDto> result = reportService.salesTrend(3_000L, 1_000L, "day");

        assertEquals(1, result.size());
        assertEquals(1_000L, result.get(0).startAt());
        assertEquals(3_000L, result.get(0).endAt());
        assertEquals(55.0, result.get(0).totalSalesAmount());
        verify(saleOrderRepository).salesTrendBuckets(
            1L,
            1_000L,
            3_000L,
            86_400_000L,
            OrderStatus.CANCELLED.code()
        );
    }

    private static SaleOrderEntity saleOrder(Long id, Long createdAt, Long updatedAt, Integer status) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("SO-" + id);
        entity.setCustomerId(100L);
        entity.setCustomerName("客户" + id);
        entity.setSubtotalAmount(100.0);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(100.0);
        entity.setStatus(status);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private static SaleOrderItemEntity saleOrderItem(
        Long id,
        Long orderId,
        Long productId,
        Double quantity,
        Double amount,
        Long createdAt
    ) {
        SaleOrderItemEntity entity = new SaleOrderItemEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderId(orderId);
        entity.setProductId(productId);
        entity.setProductCode("P-" + productId);
        entity.setProductName("商品" + productId);
        entity.setCustomerId(100L);
        entity.setCustomerName("客户");
        entity.setQuantity(quantity);
        entity.setUnitPrice(amount / quantity);
        entity.setAmount(amount);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private static PaymentEntity refundPayment(Long id, Long orderId, Double amount, Long createdAt) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderId(orderId);
        entity.setAmount(amount);
        entity.setMethod(1);
        entity.setReferenceNo("R-" + id);
        entity.setType(PaymentType.REFUND.code());
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private static InventoryAdjustmentEntity inventoryAdjustment(
        Long id,
        Long productId,
        Double quantity,
        Integer flowType,
        Long createdAt
    ) {
        InventoryAdjustmentEntity entity = new InventoryAdjustmentEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setProductId(productId);
        entity.setProductCode("P-" + productId);
        entity.setProductName("商品" + productId);
        entity.setQuantity(quantity);
        entity.setFlowType(flowType);
        entity.setReason("盘点");
        entity.setOperatorName("老板");
        entity.setCreatedAt(createdAt);
        return entity;
    }
}

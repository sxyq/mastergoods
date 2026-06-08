package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.common.PayOrderStatus;
import com.zhihuiji.backend.api.common.PaymentType;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryAdjustmentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
}

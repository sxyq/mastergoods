package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.dto.report.ReportDto;
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
        SaleOrderEntity order = saleOrder(10L, 1_000L);
        SaleOrderItemEntity item = saleOrderItem(10L, 20L, 2.0, 50.0);
        ProductEntity invalidProduct = product(null, 999.0);
        ProductEntity product = product(20L, 12.0);

        when(saleOrderRepository.findByOwnerUserIdAndCreatedAtBetween(1L, 0L, 2_000L))
            .thenReturn(List.of(order));
        when(saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn(1L, Set.of(10L)))
            .thenReturn(List.of(item));
        when(productRepository.findAllByOwnerUserId(1L))
            .thenReturn(List.of(invalidProduct, product));

        ReportDto.ProfitSummaryReportDto result = reportService.profitSummary(0L, 2_000L);

        assertEquals(24.0, result.estimatedCostAmount());
        assertEquals(76.0, result.estimatedProfitAmount());
        assertEquals(76.0, result.estimatedProfitRate());
    }

    private static SaleOrderEntity saleOrder(Long id, Long createdAt) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("SO-" + id);
        entity.setStatus(OrderStatus.COMPLETED.code());
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(100.0);
        return entity;
    }

    private static SaleOrderItemEntity saleOrderItem(
        Long orderId,
        Long productId,
        Double quantity,
        Double unitPrice
    ) {
        SaleOrderItemEntity entity = new SaleOrderItemEntity();
        entity.setId(1L);
        entity.setOwnerUserId(1L);
        entity.setOrderId(orderId);
        entity.setProductId(productId);
        entity.setProductCode("P-" + productId);
        entity.setProductName("商品" + productId);
        entity.setQuantity(quantity);
        entity.setUnitPrice(unitPrice);
        entity.setAmount(quantity * unitPrice);
        entity.setCreatedAt(1_000L);
        return entity;
    }

    private static ProductEntity product(Long id, Double purchasePrice) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(id == null ? "BAD" : "P-" + id);
        entity.setName("商品");
        entity.setCategory("默认");
        entity.setUnit("件");
        entity.setSalePrice(50.0);
        entity.setPurchasePrice(purchasePrice);
        entity.setStock(1.0);
        entity.setSafeStock(1.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}

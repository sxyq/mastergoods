package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2SaleReceiptPdfServiceTest {
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CurrentOwnerService currentOwnerService;

    private V2SaleReceiptPdfService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2SaleReceiptPdfService(
            saleOrderRepository,
            saleOrderItemRepository,
            paymentRepository,
            storeRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(7L);
    }

    @Test
    void exportBuildsChineseReceiptFromOwnerScopedOrderData() {
        SaleOrderEntity order = order();
        SaleOrderItemEntity item = item();
        PaymentEntity payment = payment();
        StoreEntity store = new StoreEntity();
        store.setStoreName("演示门店");
        when(saleOrderRepository.findByIdAndOwnerUserId(12L, 7L)).thenReturn(Optional.of(order));
        when(saleOrderItemRepository.findByOwnerUserIdAndOrderId(7L, 12L)).thenReturn(List.of(item));
        when(paymentRepository.findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(7L, 12L)).thenReturn(List.of(payment));
        when(storeRepository.findByOwnerUserId(7L)).thenReturn(Optional.of(store));

        byte[] pdf = service.export(12L);

        String rawPdf = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(rawPdf.startsWith("%PDF-"));
        assertTrue(pdf.length > 1_000);
        assertTrue(rawPdf.contains("STSong-Light"));
        assertTrue(rawPdf.contains("/Count 1"));
        verify(saleOrderRepository).findByIdAndOwnerUserId(12L, 7L);
        verify(saleOrderItemRepository).findByOwnerUserIdAndOrderId(7L, 12L);
        verify(paymentRepository).findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc(7L, 12L);
        verify(storeRepository).findByOwnerUserId(7L);
    }

    @Test
    void exportRejectsOrderOutsideCurrentOwnerScope() {
        when(saleOrderRepository.findByIdAndOwnerUserId(12L, 7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.export(12L));

        verify(saleOrderRepository).findByIdAndOwnerUserId(12L, 7L);
    }

    private static SaleOrderEntity order() {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(12L);
        entity.setOwnerUserId(7L);
        entity.setOrderNo("SO-001");
        entity.setCustomerName("客户甲");
        entity.setSubtotalAmount(108.0);
        entity.setDiscountAmount(8.0);
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(60.0);
        entity.setNotes("测试备注");
        entity.setStatus(OrderStatus.CONFIRMED.code());
        entity.setCreatedAt(1_720_000_000_000L);
        return entity;
    }

    private static SaleOrderItemEntity item() {
        SaleOrderItemEntity entity = new SaleOrderItemEntity();
        entity.setId(100L);
        entity.setOrderId(12L);
        entity.setProductCode("SKU-001");
        entity.setProductName("苹果");
        entity.setQuantity(2.0);
        entity.setUnitPrice(54.0);
        entity.setAmount(108.0);
        entity.setCreatedAt(1_720_000_000_000L);
        return entity;
    }

    private static PaymentEntity payment() {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(200L);
        entity.setOrderId(12L);
        entity.setAmount(60.0);
        entity.setMethod(1);
        entity.setType(1);
        entity.setCreatedAt(1_720_000_001_000L);
        return entity;
    }
}

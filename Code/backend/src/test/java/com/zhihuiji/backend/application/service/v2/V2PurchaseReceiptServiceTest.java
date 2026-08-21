package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReceiptDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PurchaseReceiptServiceTest {
    @Mock
    private PurchaseReceiptRepository purchaseReceiptRepository;
    @Mock
    private PurchaseReceiptItemRepository purchaseReceiptItemRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2PurchaseReceiptService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2PurchaseReceiptService(
            purchaseReceiptRepository,
            purchaseReceiptItemRepository,
            purchaseOrderRepository,
            productRepository,
            supplierRepository,
            currentOwnerService,
            new IdGenerator()
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(productRepository.findByIdForUpdate(1L, 11L)).thenReturn(Optional.of(product(11L, "P001", "矿泉水")));
        when(purchaseReceiptRepository.save(any(PurchaseReceiptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseReceiptItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void invalidSupplierReferenceIsRejected() {
        when(supplierRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(9L, null))
        );

        assertEquals("供应商不存在", error.getMessage());
        verify(purchaseReceiptRepository, never()).save(any(PurchaseReceiptEntity.class));
    }

    @Test
    void purchaseOrderSupplierMismatchIsRejected() {
        when(purchaseOrderRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(purchaseOrder(6L, 4L, "供应商B")));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(3L, 6L))
        );

        assertEquals("收货供应商与采购订单不一致", error.getMessage());
        verify(purchaseReceiptRepository, never()).save(any(PurchaseReceiptEntity.class));
    }

    @Test
    void purchaseOrderSupplierIsUsedAsTrustedSource() {
        when(purchaseOrderRepository.findByIdAndOwnerUserId(6L, 1L)).thenReturn(Optional.of(purchaseOrder(6L, 4L, "供应商B")));
        when(supplierRepository.findByIdAndOwnerUserId(4L, 1L)).thenReturn(Optional.of(supplier(4L, "供应商B")));

        V2PurchaseReceiptDtos.PurchaseReceiptResponse response = service.create(createRequest(null, 6L));

        assertEquals(4L, response.supplierId());
        assertEquals("供应商B", response.supplierName());
        assertEquals(8.0, response.totalAmount());
    }

    @Test
    void listWithoutKeywordAvoidsSearchQuery() {
        PurchaseReceiptEntity receipt = receipt(5L, 0);
        when(purchaseReceiptRepository.findByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(receipt));
        when(purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(1L, 5L)).thenReturn(List.of());

        List<V2PurchaseReceiptDtos.PurchaseReceiptResponse> responses = service.list(null, null);

        assertEquals(1, responses.size());
        assertEquals("RC-5", responses.get(0).receiptNo());
        verify(purchaseReceiptRepository, never()).search(any(), any(), any());
    }

    @Test
    void listWithBlankKeywordAndStatusAvoidsSearchQuery() {
        PurchaseReceiptEntity receipt = receipt(6L, 1);
        when(purchaseReceiptRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(1L, 1)).thenReturn(List.of(receipt));
        when(purchaseReceiptItemRepository.findByOwnerUserIdAndReceiptIdOrderByCreatedAtAsc(1L, 6L)).thenReturn(List.of());

        List<V2PurchaseReceiptDtos.PurchaseReceiptResponse> responses = service.list("   ", 1);

        assertEquals(1, responses.size());
        assertEquals(1, responses.get(0).status());
        verify(purchaseReceiptRepository, never()).search(any(), any(), any());
    }

    private static V2PurchaseReceiptDtos.CreateRequest createRequest(Long supplierId, Long purchaseOrderId) {
        return new V2PurchaseReceiptDtos.CreateRequest(
            purchaseOrderId,
            supplierId,
            "不可信供应商名",
            List.of(new V2PurchaseReceiptDtos.CreateItemRequest(11L, null, "矿泉水", 2.0, 4.0)),
            "收货备注"
        );
    }

    private static PurchaseReceiptEntity receipt(Long id, Integer status) {
        PurchaseReceiptEntity entity = new PurchaseReceiptEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setReceiptNo("RC-" + id);
        entity.setSupplierId(4L);
        entity.setSupplierName("供应商B");
        entity.setTotalAmount(8.0);
        entity.setStatus(status);
        entity.setCreatedAt(1000L);
        entity.setUpdatedAt(2000L);
        return entity;
    }

    private static ProductEntity product(Long id, String code, String name) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setStock(5.0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static SupplierEntity supplier(Long id, String name) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setBalance(0.0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static PurchaseOrderEntity purchaseOrder(Long id, Long supplierId, String supplierName) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("PO-" + id);
        entity.setSupplierId(supplierId);
        entity.setSupplierName(supplierName);
        entity.setReceivedAmount(0.0);
        entity.setSyncVersion(1L);
        return entity;
    }
}

package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.common.PurchaseReturnStatus;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReturnDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRefundRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PurchaseReturnServiceTest {
    @Mock
    private PurchaseReturnRepository purchaseReturnRepository;
    @Mock
    private PurchaseReturnItemRepository purchaseReturnItemRepository;
    @Mock
    private PurchaseReturnRefundRepository purchaseReturnRefundRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2PurchaseReturnService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2PurchaseReturnService(
            purchaseReturnRepository,
            purchaseReturnItemRepository,
            purchaseReturnRefundRepository,
            purchaseOrderRepository,
            purchaseOrderItemRepository,
            productRepository,
            supplierRepository,
            currentOwnerService,
            new IdGenerator()
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(productRepository.findByIdForUpdate(1L, 11L)).thenReturn(Optional.of(product(11L, "P001", "矿泉水")));
        when(purchaseReturnRepository.save(any(PurchaseReturnEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseReturnItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPersistsReturnAndItems() {
        when(supplierRepository.findByIdAndOwnerUserId(4L, 1L)).thenReturn(Optional.of(supplier(4L, "供应商A")));

        V2PurchaseReturnDtos.PurchaseReturnResponse response = service.create(createRequest(null, 4L));

        assertEquals(4L, response.supplierId());
        assertEquals("供应商A", response.supplierName());
        assertEquals(8.0, response.totalAmount());
        verify(purchaseReturnRepository).save(any(PurchaseReturnEntity.class));
        verify(purchaseReturnItemRepository).saveAll(any());
    }

    @Test
    void listUsesBatchQueryForItemsAndRefunds() {
        PurchaseReturnEntity return1 = purchaseReturn(5L, PurchaseReturnStatus.DRAFT.code());
        PurchaseReturnEntity return2 = purchaseReturn(6L, PurchaseReturnStatus.DRAFT.code());
        when(purchaseReturnRepository.findByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(return1, return2));
        when(purchaseReturnItemRepository.findAllByOwnerUserIdAndReturnIdIn(1L, List.of(5L, 6L))).thenReturn(List.of());
        when(purchaseReturnRefundRepository.findAllByOwnerUserIdAndReturnIdIn(1L, List.of(5L, 6L))).thenReturn(List.of());

        List<V2PurchaseReturnDtos.PurchaseReturnResponse> responses = service.list(null, null);

        assertEquals(2, responses.size());
        verify(purchaseReturnItemRepository).findAllByOwnerUserIdAndReturnIdIn(1L, List.of(5L, 6L));
        verify(purchaseReturnRefundRepository).findAllByOwnerUserIdAndReturnIdIn(1L, List.of(5L, 6L));
        verify(purchaseReturnItemRepository, never()).findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(any(), any());
        verify(purchaseReturnRefundRepository, never()).findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void listByOrderUsesBatchQueryForItemsAndRefunds() {
        PurchaseReturnEntity returnEntity = purchaseReturn(7L, PurchaseReturnStatus.DRAFT.code());
        when(purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(1L, 10L)).thenReturn(List.of(returnEntity));
        when(purchaseReturnItemRepository.findAllByOwnerUserIdAndReturnIdIn(1L, List.of(7L))).thenReturn(List.of());
        when(purchaseReturnRefundRepository.findAllByOwnerUserIdAndReturnIdIn(1L, List.of(7L))).thenReturn(List.of());

        List<V2PurchaseReturnDtos.PurchaseReturnResponse> responses = service.listByOrder(10L);

        assertEquals(1, responses.size());
        assertEquals("PR-7", responses.get(0).returnNo());
        verify(purchaseReturnItemRepository).findAllByOwnerUserIdAndReturnIdIn(1L, List.of(7L));
        verify(purchaseReturnRefundRepository).findAllByOwnerUserIdAndReturnIdIn(1L, List.of(7L));
    }

    @Test
    void getReturnsSingleReturn() {
        PurchaseReturnEntity entity = purchaseReturn(8L, PurchaseReturnStatus.DRAFT.code());
        when(purchaseReturnRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(entity));
        when(purchaseReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 8L)).thenReturn(List.of());
        when(purchaseReturnRefundRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 8L)).thenReturn(List.of());

        V2PurchaseReturnDtos.PurchaseReturnResponse response = service.get(8L);

        assertEquals(8L, response.id());
        assertEquals("PR-8", response.returnNo());
    }

    @Test
    void cancelDraftReturnUpdatesStatus() {
        PurchaseReturnEntity entity = purchaseReturn(9L, PurchaseReturnStatus.DRAFT.code());
        when(purchaseReturnRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(entity));
        when(purchaseReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 9L)).thenReturn(List.of());
        when(purchaseReturnRefundRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 9L)).thenReturn(List.of());

        V2PurchaseReturnDtos.PurchaseReturnResponse response = service.cancel(9L);

        assertEquals(PurchaseReturnStatus.CANCELLED.code(), response.status());
        verify(purchaseReturnRepository).save(any(PurchaseReturnEntity.class));
    }

    private static V2PurchaseReturnDtos.CreateRequest createRequest(Long purchaseOrderId, Long supplierId) {
        return new V2PurchaseReturnDtos.CreateRequest(
            purchaseOrderId,
            supplierId,
            "不可信供应商名",
            List.of(new V2PurchaseReturnDtos.CreateItemRequest(11L, null, "矿泉水", 2.0, 4.0)),
            "退货备注"
        );
    }

    private static PurchaseReturnEntity purchaseReturn(Long id, Integer status) {
        PurchaseReturnEntity entity = new PurchaseReturnEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setReturnNo("PR-" + id);
        entity.setSupplierId(4L);
        entity.setSupplierName("供应商A");
        entity.setTotalAmount(8.0);
        entity.setRefundAmount(0.0);
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
}

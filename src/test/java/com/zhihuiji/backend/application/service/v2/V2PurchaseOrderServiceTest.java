package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseOrderDtos;
import com.zhihuiji.backend.application.service.PurchaseOrderService;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PurchaseOrderServiceTest {
    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    private V2PurchaseOrderService v2PurchaseOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        v2PurchaseOrderService = new V2PurchaseOrderService(purchaseOrderService, purchaseOrderItemRepository);
    }

    @Test
    void listUsesBatchItemLookup() {
        PurchaseOrderEntity first = purchaseOrder(11L, 99L, "PO-1");
        PurchaseOrderEntity second = purchaseOrder(12L, 99L, "PO-2");
        PurchaseOrderItemEntity firstItem = purchaseItem(1001L, 11L, "PRD-1", "商品1");
        PurchaseOrderItemEntity secondItem = purchaseItem(1002L, 12L, "PRD-2", "商品2");

        when(purchaseOrderService.list("采购", 1)).thenReturn(List.of(first, second));
        when(purchaseOrderItemRepository.findByOwnerUserIdAndOrderIdIn(eq(99L), anyCollection()))
            .thenReturn(List.of(firstItem, secondItem));

        List<V2PurchaseOrderDtos.PurchaseOrderResponse> responses = v2PurchaseOrderService.list("采购", 1);

        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).items().size());
        assertEquals(1, responses.get(1).items().size());
        assertEquals("PRD-1", responses.get(0).items().get(0).productCode());
        assertEquals("PRD-2", responses.get(1).items().get(0).productCode());
        verify(purchaseOrderService, never()).listItems(anyLong());
        ArgumentCaptor<Collection<Long>> orderIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(purchaseOrderItemRepository).findByOwnerUserIdAndOrderIdIn(eq(99L), orderIdsCaptor.capture());
        assertTrue(orderIdsCaptor.getValue().containsAll(List.of(11L, 12L)));
    }

    private static PurchaseOrderEntity purchaseOrder(Long id, Long ownerUserId, String orderNo) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(ownerUserId);
        entity.setOrderNo(orderNo);
        entity.setSupplierName("供应商");
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(0.0);
        entity.setReceivedAmount(0.0);
        entity.setSettlementMethod(1);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static PurchaseOrderItemEntity purchaseItem(Long id, Long orderId, String productCode, String productName) {
        PurchaseOrderItemEntity entity = new PurchaseOrderItemEntity();
        entity.setId(id);
        entity.setOwnerUserId(99L);
        entity.setOrderId(orderId);
        entity.setProductId(100L + id);
        entity.setProductCode(productCode);
        entity.setProductName(productName);
        entity.setQuantity(1.0);
        entity.setUnitCost(10.0);
        entity.setAmount(10.0);
        entity.setCreatedAt(1L);
        return entity;
    }
}

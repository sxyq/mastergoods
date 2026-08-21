package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PurchaseOrderLookupToolTest {
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;

    private ObjectMapper objectMapper;
    private PurchaseOrderLookupTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new PurchaseOrderLookupTool(purchaseOrderRepository, purchaseOrderItemRepository);
    }

    @Test
    void exposesOrderSupplierAndItemIdsForDraftFollowUp() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setId(31L);
        order.setOwnerUserId(1L);
        order.setOrderNo("PO-31");
        order.setSupplierId(41L);
        order.setSupplierName("真实供应商");
        order.setTotalAmount(120.0);
        order.setPaidAmount(20.0);
        order.setReceivedAmount(0.0);
        order.setStatus(1);
        PurchaseOrderItemEntity item = new PurchaseOrderItemEntity();
        item.setOrderId(31L);
        item.setProductId(51L);
        item.setProductCode("P-51");
        item.setProductName("真实商品");
        item.setQuantity(2.0);
        item.setUnitCost(60.0);
        when(purchaseOrderRepository.search(eq(1L), any(), any(), any())).thenReturn(List.of(order));
        when(purchaseOrderItemRepository.findByOwnerUserIdAndOrderIdIn(eq(1L), any()))
            .thenReturn(List.of(item));

        var result = tool.execute(
            new ToolContext(1L, null, 100L, "run-purchase", null, objectMapper),
            objectMapper.createObjectNode()
        );
        var facts = result.toolFacts().path("recent_orders").get(0);

        assertEquals(31L, facts.path("order_id").asLong());
        assertEquals(41L, facts.path("supplier_id").asLong());
        assertEquals(51L, facts.path("items").get(0).path("product_id").asLong());
    }
}

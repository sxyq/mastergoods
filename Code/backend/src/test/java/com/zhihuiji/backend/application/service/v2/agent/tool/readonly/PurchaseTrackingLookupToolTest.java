package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class PurchaseTrackingLookupToolTest {
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseReceiptRepository purchaseReceiptRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;

    private ObjectMapper objectMapper;
    private PurchaseTrackingLookupTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new PurchaseTrackingLookupTool(
            purchaseOrderRepository,
            purchaseReceiptRepository,
            purchaseReturnRepository,
            objectMapper
        );
    }

    @Test
    void usesLatestOwnerScopedPurchaseOrderWhenModelProvidesNoFilter() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setId(31L);
        order.setOwnerUserId(7L);
        order.setOrderNo("PO-31");
        order.setSupplierName("真实供应商");
        order.setTotalAmount(120D);
        order.setPaidAmount(20D);
        order.setReceivedAmount(60D);
        order.setStatus(1);
        when(purchaseOrderRepository.search(7L, null, null, PageRequest.of(0, 1)))
            .thenReturn(List.of(order));
        when(purchaseReceiptRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(7L, 31L))
            .thenReturn(List.of());
        when(purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(7L, 31L))
            .thenReturn(List.of());

        var result = tool.execute(
            new ToolContext(7L, null, 100L, "run-tracking", null, objectMapper),
            objectMapper.createObjectNode()
        );

        assertEquals("PO-31", result.toolFacts().path("order_no").asText());
        assertEquals(0, result.toolFacts().path("receipt_count").asInt());
        verify(purchaseOrderRepository).search(7L, null, null, PageRequest.of(0, 1));
    }
}

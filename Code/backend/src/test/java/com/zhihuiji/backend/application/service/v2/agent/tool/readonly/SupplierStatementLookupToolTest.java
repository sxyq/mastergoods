package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SupplierStatementLookupToolTest {
    @Mock private SupplierRepository supplierRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;

    private SupplierStatementLookupTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new SupplierStatementLookupTool(
            supplierRepository,
            purchaseOrderRepository,
            purchaseReturnRepository,
            objectMapper
        );
    }

    @Test
    void buildsStatementFromOwnerScopedSupplierAndPurchaseData() {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(21L);
        supplier.setOwnerUserId(1L);
        supplier.setName("真实供应商");
        supplier.setPhone("13900000000");
        supplier.setBalance(80D);

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setId(31L);
        order.setOwnerUserId(1L);
        order.setOrderNo("PO-001");
        order.setSupplierId(21L);
        order.setSupplierName("真实供应商");
        order.setTotalAmount(200D);
        order.setPaidAmount(120D);
        order.setStatus(1);
        order.setCreatedAt(1_720_000_000_000L);

        when(supplierRepository.search(1L, "真实供应商", null, null))
            .thenReturn(List.of(supplier));
        when(purchaseOrderRepository.search(eq(1L), eq("真实供应商"), any()))
            .thenReturn(List.of(order));
        when(purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(1L, 31L))
            .thenReturn(List.of());

        var result = tool.execute(
            new ToolContext(1L, null, 100L, "run-supplier", null, objectMapper),
            objectMapper.createObjectNode().put("keyword", "真实供应商")
        );

        assertEquals("真实供应商", result.toolFacts().path("supplier_name").asText());
        assertEquals(1, result.toolFacts().path("order_count").asInt());
        assertEquals("¥80.00", result.toolFacts().path("balance").asText());
        assertEquals("PO-001", result.toolFacts().path("latest_order_no").asText());
    }
}

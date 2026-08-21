package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SaleOrderLookupToolTest {
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;

    private ObjectMapper objectMapper;
    private SaleOrderLookupTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new SaleOrderLookupTool(saleOrderRepository, saleOrderItemRepository);
    }

    @Test
    void exposesOrderCustomerAndItemIdsForDraftFollowUp() {
        SaleOrderEntity order = new SaleOrderEntity();
        order.setId(61L);
        order.setOwnerUserId(1L);
        order.setOrderNo("SO-61");
        order.setCustomerId(71L);
        order.setCustomerName("真实客户");
        order.setTotalAmount(88.0);
        order.setPaidAmount(18.0);
        order.setStatus(1);
        SaleOrderItemEntity item = new SaleOrderItemEntity();
        item.setOrderId(61L);
        item.setProductId(81L);
        item.setProductCode("P-81");
        item.setProductName("真实商品");
        item.setQuantity(2.0);
        item.setUnitPrice(44.0);
        when(saleOrderRepository.search(eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(order));
        when(saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn(eq(1L), any()))
            .thenReturn(List.of(item));

        var result = tool.execute(
            new ToolContext(1L, null, 100L, "run-sale", null, objectMapper),
            objectMapper.createObjectNode()
        );
        var facts = result.toolFacts().path("recent_orders").get(0);

        assertEquals(61L, facts.path("order_id").asLong());
        assertEquals(71L, facts.path("customer_id").asLong());
        assertEquals(81L, facts.path("items").get(0).path("product_id").asLong());
    }
}

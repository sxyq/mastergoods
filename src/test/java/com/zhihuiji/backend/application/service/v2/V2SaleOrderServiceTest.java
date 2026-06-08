package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.OrderStatus;
import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.SaleOrderService;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderItemEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class V2SaleOrderServiceTest {
    @Mock
    private SaleOrderService saleOrderService;
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private SaleOrderItemRepository saleOrderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2SaleOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2SaleOrderService(
            saleOrderService,
            saleOrderRepository,
            saleOrderItemRepository,
            productRepository,
            customerRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void listUsesRepositoryPaginationAndBatchLoadsItems() {
        SaleOrderEntity first = order(10L, "SO-10", 2_000L);
        SaleOrderEntity second = order(11L, "SO-11", 1_000L);
        SaleOrderItemEntity firstItem = item(100L, 10L, 20L);
        SaleOrderItemEntity secondItem = item(101L, 11L, 21L);
        when(saleOrderRepository.search(
            1L,
            "SO",
            OrderStatus.COMPLETED.code(),
            10.0,
            200.0,
            0L,
            3_000L,
            "商品",
            1,
            PageRequest.of(1, 20)
        )).thenReturn(List.of(first, second));
        when(saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn(1L, Set.of(10L, 11L)))
            .thenReturn(List.of(firstItem, secondItem));

        List<V2SaleOrderDtos.SaleOrderResponse> result = service.list(
            "SO",
            OrderStatus.COMPLETED.code(),
            10.0,
            200.0,
            0L,
            3_000L,
            "商品",
            1,
            1,
            20
        );

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(1, result.get(0).items().size());
        assertEquals(20L, result.get(0).items().getFirst().productId());
        assertEquals(11L, result.get(1).id());
        assertEquals(21L, result.get(1).items().getFirst().productId());
        verify(saleOrderRepository).search(
            1L,
            "SO",
            OrderStatus.COMPLETED.code(),
            10.0,
            200.0,
            0L,
            3_000L,
            "商品",
            1,
            PageRequest.of(1, 20)
        );
        verify(saleOrderItemRepository).findByOwnerUserIdAndOrderIdIn(1L, Set.of(10L, 11L));
        verify(saleOrderService, never()).list("SO", OrderStatus.COMPLETED.code(), 10.0, 200.0, 0L, 3_000L, "商品", 1);
        verify(saleOrderService, never()).listItems(10L);
        verify(saleOrderService, never()).listItems(11L);
    }

    @Test
    void listNormalizesInvalidPageAndCapsSize() {
        when(saleOrderRepository.search(
            1L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 200)
        )).thenReturn(List.of());

        List<V2SaleOrderDtos.SaleOrderResponse> result = service.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            -1,
            999
        );

        assertEquals(0, result.size());
        verify(saleOrderRepository).search(
            1L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 200)
        );
        verify(saleOrderItemRepository, never()).findByOwnerUserIdAndOrderIdIn(1L, Set.of());
    }

    private static SaleOrderEntity order(Long id, String orderNo, Long createdAt) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo(orderNo);
        entity.setCustomerId(2L);
        entity.setCustomerName("客户");
        entity.setSubtotalAmount(100.0);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(100.0);
        entity.setPaidAmount(100.0);
        entity.setStatus(OrderStatus.COMPLETED.code());
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static SaleOrderItemEntity item(Long id, Long orderId, Long productId) {
        SaleOrderItemEntity entity = new SaleOrderItemEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderId(orderId);
        entity.setProductId(productId);
        entity.setProductCode("P-" + productId);
        entity.setProductName("商品" + productId);
        entity.setCustomerId(2L);
        entity.setCustomerName("客户");
        entity.setQuantity(1.0);
        entity.setUnitPrice(100.0);
        entity.setAmount(100.0);
        entity.setCreatedAt(1_000L);
        return entity;
    }
}

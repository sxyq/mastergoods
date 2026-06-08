package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.sales.V2SalesReturnDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2SalesReturnServiceTest {
    @Mock
    private SalesReturnRepository salesReturnRepository;
    @Mock
    private SalesReturnItemRepository salesReturnItemRepository;
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2SalesReturnService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2SalesReturnService(
            salesReturnRepository,
            salesReturnItemRepository,
            saleOrderRepository,
            productRepository,
            customerRepository,
            paymentRepository,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(productRepository.findByIdForUpdate(1L, 11L)).thenReturn(Optional.of(product(11L, "P001", "矿泉水", 3.0)));
        when(salesReturnRepository.save(any(SalesReturnEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(salesReturnItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void negativeQuantityIsRejected() {
        when(customerRepository.findByIdAndOwnerUserId(2L, 1L)).thenReturn(Optional.of(customer(2L, "客户A")));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(2L, -1.0, null))
        );

        assertEquals("退货数量必须大于0", error.getMessage());
        verify(salesReturnRepository, never()).save(any(SalesReturnEntity.class));
    }

    @Test
    void zeroQuantityIsRejected() {
        when(customerRepository.findByIdAndOwnerUserId(2L, 1L)).thenReturn(Optional.of(customer(2L, "客户A")));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(2L, 0.0, null))
        );

        assertEquals("退货数量必须大于0", error.getMessage());
        verify(salesReturnRepository, never()).save(any(SalesReturnEntity.class));
    }

    @Test
    void invalidCustomerReferenceIsRejected() {
        when(customerRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(9L, 2.0, null))
        );

        assertEquals("客户不存在", error.getMessage());
        verify(salesReturnRepository, never()).save(any(SalesReturnEntity.class));
    }

    @Test
    void originalOrderCustomerMismatchIsRejected() {
        when(saleOrderRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(saleOrder(5L, 3L, "客户B")));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(createRequest(2L, 2.0, 5L))
        );

        assertEquals("退货客户与原销售订单不一致", error.getMessage());
        verify(salesReturnRepository, never()).save(any(SalesReturnEntity.class));
    }

    @Test
    void originalOrderCustomerIsUsedAsTrustedSource() {
        when(saleOrderRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(saleOrder(5L, 3L, "客户B")));
        when(customerRepository.findByIdAndOwnerUserId(3L, 1L)).thenReturn(Optional.of(customer(3L, "客户B")));

        V2SalesReturnDtos.SalesReturnResponse response = service.create(createRequest(null, 2.0, 5L));

        assertEquals(3L, response.customerId());
        assertEquals("客户B", response.customerName());
        assertEquals(6.0, response.totalAmount());
    }

    @Test
    void listWithoutKeywordAvoidsSearchQuery() {
        SalesReturnEntity salesReturn = salesReturn(7L, 0);
        when(salesReturnRepository.findByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(salesReturn));
        when(salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 7L)).thenReturn(List.of());

        List<V2SalesReturnDtos.SalesReturnResponse> responses = service.list(null, null);

        assertEquals(1, responses.size());
        assertEquals("SR-7", responses.get(0).returnNo());
        verify(salesReturnRepository, never()).search(any(), any(), any());
    }

    @Test
    void listWithBlankKeywordAndStatusAvoidsSearchQuery() {
        SalesReturnEntity salesReturn = salesReturn(8L, 1);
        when(salesReturnRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(1L, 1)).thenReturn(List.of(salesReturn));
        when(salesReturnItemRepository.findByOwnerUserIdAndReturnIdOrderByCreatedAtAsc(1L, 8L)).thenReturn(List.of());

        List<V2SalesReturnDtos.SalesReturnResponse> responses = service.list("   ", 1);

        assertEquals(1, responses.size());
        assertEquals(1, responses.get(0).status());
        verify(salesReturnRepository, never()).search(any(), any(), any());
    }

    private static V2SalesReturnDtos.CreateRequest createRequest(Long customerId, Double quantity, Long originalOrderId) {
        return new V2SalesReturnDtos.CreateRequest(
            originalOrderId,
            customerId,
            "不可信名称",
            List.of(new V2SalesReturnDtos.CreateItemRequest(11L, quantity, null)),
            "退货备注"
        );
    }

    private static SalesReturnEntity salesReturn(Long id, Integer status) {
        SalesReturnEntity entity = new SalesReturnEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setReturnNo("SR-" + id);
        entity.setCustomerId(2L);
        entity.setCustomerName("客户A");
        entity.setTotalAmount(6.0);
        entity.setRefundAmount(0.0);
        entity.setStatus(status);
        entity.setCreatedAt(1000L);
        entity.setUpdatedAt(2000L);
        return entity;
    }

    private static ProductEntity product(Long id, String code, String name, Double salePrice) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setSalePrice(salePrice);
        entity.setStock(8.0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static CustomerEntity customer(Long id, String name) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setBalance(0.0);
        entity.setSyncVersion(1L);
        return entity;
    }

    private static SaleOrderEntity saleOrder(Long id, Long customerId, String customerName) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderNo("SO-" + id);
        entity.setCustomerId(customerId);
        entity.setCustomerName(customerName);
        return entity;
    }
}

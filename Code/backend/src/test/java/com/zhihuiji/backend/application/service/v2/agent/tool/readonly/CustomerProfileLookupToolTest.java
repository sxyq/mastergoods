package com.zhihuiji.backend.application.service.v2.agent.tool.readonly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class CustomerProfileLookupToolTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SalesReturnRepository salesReturnRepository;

    private ObjectMapper objectMapper;
    private CustomerProfileLookupTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        tool = new CustomerProfileLookupTool(
            customerRepository,
            saleOrderRepository,
            paymentRepository,
            salesReturnRepository,
            objectMapper
        );
    }

    @Test
    void selectsTheOnlyOwnerScopedCustomerWhenTheModelOmitsFilters() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(17L);
        customer.setOwnerUserId(1L);
        customer.setName("真实客户");
        customer.setPhone("13800000000");
        customer.setBalance(25.0);
        when(customerRepository.findAllByOwnerUserIdOrderByNameAsc(1L, PageRequest.of(0, 2)))
            .thenReturn(List.of(customer));
        when(saleOrderRepository.search(eq(1L), eq("真实客户"), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        var params = objectMapper.createObjectNode()
            .put("customer_id", 0)
            .put("keyword", "null");
        var result = tool.execute(
            new ToolContext(1L, null, 100L, "run-customer", null, objectMapper),
            params
        );

        assertEquals(17L, result.toolFacts().path("customer_id").asLong());
        assertEquals("真实客户", result.toolFacts().path("customer_name").asText());
    }
}

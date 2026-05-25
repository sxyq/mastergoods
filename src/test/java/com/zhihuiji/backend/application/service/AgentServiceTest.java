package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AgentServiceTest {
    @Mock
    private ReportService reportService;
    @Mock
    private SaleOrderService saleOrderService;
    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private SaleOrderItemRepository saleOrderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PayOrderRepository payOrderRepository;
    @Mock
    private AgentLlmService agentLlmService;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agentService = new AgentService(
            reportService,
            saleOrderService,
            purchaseOrderService,
            customerRepository,
            supplierRepository,
            productRepository,
            saleOrderRepository,
            saleOrderItemRepository,
            paymentRepository,
            payOrderRepository
        );
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(saleOrderRepository.findAll()).thenReturn(List.of());
        when(payOrderRepository.findAll()).thenReturn(List.of());
        when(reportService.lowStockProducts(6)).thenReturn(List.of());
    }

    @Test
    void answerQuestionReturnsReceivableInsight() {
        when(reportService.receivables(5)).thenReturn(List.of(
            new ReportDto.CustomerReceivableReportDto(1L, "客户甲", "13800138000", 2800.0)
        ));

        AgentDto.AgentAnswerDto answer = agentService.answerQuestion("哪些客户欠款最多");

        assertEquals("receivables", answer.intent());
        assertTrue(answer.answer().contains("客户甲"));
        assertEquals(1, answer.rows().size());
    }

    @Test
    void draftOperationBuildsPurchaseDraft() {
        ProductEntity product = new ProductEntity();
        product.setCode("S7");
        product.setName("工业传感器 S7");
        product.setPurchasePrice(35.0);
        product.setSalePrice(50.0);
        product.setStock(12.0);
        product.setSafeStock(20.0);
        product.setStatus(1);
        product.setSyncStatus(0);
        product.setSyncVersion(1L);
        product.setCreatedAt(1L);
        product.setUpdatedAt(1L);

        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("供应商A");
        supplier.setPhone("13900000000");
        supplier.setBalance(5000.0);
        supplier.setStatus(1);
        supplier.setSyncStatus(0);
        supplier.setSyncVersion(1L);
        supplier.setCreatedAt(1L);
        supplier.setUpdatedAt(1L);

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));
        when(customerRepository.findAll()).thenReturn(List.of());

        AgentDto.OperationDraftDto draft = agentService.draftOperation("给供应商A入库 20 个工业传感器 S7，单价 35");

        assertEquals("purchase", draft.operationType());
        assertTrue(draft.canSubmit());
        assertEquals("供应商A", draft.partnerName());
        assertEquals(1, draft.items().size());
        assertEquals(20.0, draft.items().get(0).quantity());
        assertFalse(draft.suggestedActions().isEmpty());
    }

    @Test
    void draftOperationBlocksShortStockSale() {
        ProductEntity product = new ProductEntity();
        product.setCode("A12");
        product.setName("绝缘手套 A12");
        product.setPurchasePrice(18.0);
        product.setSalePrice(48.0);
        product.setStock(2.0);
        product.setSafeStock(10.0);
        product.setStatus(1);
        product.setSyncStatus(0);
        product.setSyncVersion(1L);
        product.setCreatedAt(1L);
        product.setUpdatedAt(1L);

        CustomerEntity customer = new CustomerEntity();
        customer.setName("客户B");
        customer.setPhone("13700000000");
        customer.setBalance(0.0);
        customer.setStatus(1);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        customer.setCreatedAt(1L);
        customer.setUpdatedAt(1L);

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(supplierRepository.findAll()).thenReturn(List.of());

        AgentDto.OperationDraftDto draft = agentService.draftOperation("给客户B销售 5 个绝缘手套 A12，单价 48");

        assertNotNull(draft);
        assertEquals("sale", draft.operationType());
        assertFalse(draft.canSubmit());
        assertTrue(draft.warnings().stream().anyMatch(text -> text.contains("库存")));
    }
}

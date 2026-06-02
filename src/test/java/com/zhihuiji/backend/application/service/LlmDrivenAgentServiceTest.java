package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AlertDtos;
import com.zhihuiji.backend.api.dto.agent.AnswerDtos;
import com.zhihuiji.backend.api.dto.agent.OperationDraftDtos;
import com.zhihuiji.backend.api.dto.agent.ReconciliationDtos;
import com.zhihuiji.backend.api.dto.agent.WorkbenchDtos;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LlmDrivenAgentServiceTest {
    @Mock
    private AgentService agentService;
    @Mock
    private AgentLlmService agentLlmService;
    @Mock
    private ReportService reportService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private ObjectMapper objectMapper;
    private LlmDrivenAgentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        service = new LlmDrivenAgentService(
            agentService,
            agentLlmService,
            reportService,
            productRepository,
            customerRepository,
            supplierRepository,
            objectMapper,
            currentOwnerService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void answerQuestionUsesLlmStructuredResponse() throws Exception {
        when(agentLlmService.isEnabled()).thenReturn(true);
        when(reportService.topProducts(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(List.of(new ReportDto.TopSellingProductReportDto(1L, "S7", "sensor", 5, 200)));
        when(reportService.receivables(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(List.of(new ReportDto.CustomerReceivableReportDto(1L, "customer-a", "13800138000", 2800)));
        when(reportService.lowStockProducts(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(List.of(new ReportDto.LowStockProductReportDto(2L, "A12", "glove", 3, 10)));
        when(agentService.getReportInsight(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new ReconciliationDtos.ReportInsightDto("7d", 1000, 900, 11.1, "fallback", "sensor", 200, "customer-a", 300, List.of("h1"), List.of("a1")));
        when(agentService.getReconciliationFollowup(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new ReconciliationDtos.ReconciliationFollowupDto(2800, 1200, 300, 100, 200, List.of(), List.of(), List.of()));
        when(agentService.getAlerts(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new AlertDtos.AlertDashboardDto(List.of()));
        when(agentService.answerQuestion(anyString()))
            .thenReturn(new AnswerDtos.AgentAnswerDto("q", "fallback", "fallback", List.of(), List.of(), List.of(), List.of()));
        when(agentLlmService.requestStructuredJson(anyString(), anyString()))
            .thenReturn(Optional.of(objectMapper.readTree("""
                {
                  "intent": "receivables",
                  "answer": "customer-a owes the most.",
                  "highlights": ["balance 2800"],
                  "columns": ["customer", "balance"],
                  "rows": [["customer-a", "2800.00"]],
                  "suggestedActions": ["call customer-a"]
                }
                """)));

        AnswerDtos.AgentAnswerDto answer = service.answerQuestion("who owes the most");

        assertEquals("receivables", answer.intent());
        assertEquals("customer-a owes the most.", answer.answer());
        assertEquals(1, answer.rows().size());
        assertEquals("customer-a", answer.rows().get(0).get(0));
    }

    @Test
    void draftOperationUsesLlmToResolveStructuredDraft() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setId(7L);
        product.setCode("S7");
        product.setName("sensor");
        product.setPurchasePrice(35.0);
        product.setSalePrice(50.0);
        product.setStock(12.0);
        product.setSafeStock(20.0);

        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(11L);
        supplier.setName("supplier-a");
        supplier.setPhone("13900000000");
        supplier.setBalance(0.0);

        when(agentLlmService.isEnabled()).thenReturn(true);
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(product));
        when(productRepository.findByOwnerUserIdAndCode(1L, "S7")).thenReturn(Optional.of(product));
        when(productRepository.findAllByOwnerUserId(1L)).thenReturn(List.of(product));
        when(customerRepository.findAllByOwnerUserIdOrderByNameAsc(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(supplier));
        when(supplierRepository.findAllByOwnerUserId(1L)).thenReturn(List.of(supplier));
        when(agentService.draftOperation(anyString()))
            .thenReturn(new OperationDraftDtos.OperationDraftDto("purchase", "fallback", "supplier", null, null, List.of(), "note", false, List.of("warn"), List.of("action")));
        when(agentLlmService.requestStructuredJson(anyString(), anyString()))
            .thenReturn(Optional.of(objectMapper.readTree("""
                {
                  "operationType": "purchase",
                  "partnerRole": "supplier",
                  "partnerId": 11,
                  "partnerName": "supplier-a",
                  "notes": "purchase sensor",
                  "items": [
                    {
                      "productId": 7,
                      "productCode": "S7",
                      "productName": "sensor",
                      "quantity": 20,
                      "unitPrice": 35
                    }
                  ]
                }
                """)));

        OperationDraftDtos.OperationDraftDto draft = service.draftOperation("purchase 20 sensor S7 from supplier-a at 35");

        assertEquals("purchase", draft.operationType());
        assertEquals("supplier-a", draft.partnerName());
        assertEquals(11L, draft.partnerId());
        assertEquals(1, draft.items().size());
        assertEquals("S7", draft.items().get(0).productCode());
        assertEquals(20.0, draft.items().get(0).quantity());
        assertTrue(draft.canSubmit());
        assertFalse(draft.suggestedActions().isEmpty());
    }

    @Test
    void workbenchIncludesProactiveAnswerAndDraft() {
        when(agentLlmService.isEnabled()).thenReturn(false);
        when(agentService.getWorkbench(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new WorkbenchDtos.AgentWorkbenchDto(
                new ReconciliationDtos.ReconciliationFollowupDto(10, 20, 30, 40, -10, List.of(), List.of(), List.of()),
                new ReconciliationDtos.ReportInsightDto("7d", 100, 90, 11.1, "narrative", "sensor", 60, "customer-a", 40, List.of("h1"), List.of("a1")),
                new AlertDtos.AlertDashboardDto(List.of()),
                List.of("who owes the most"),
                List.of("purchase 20 sensor S7 from supplier-a at 35")
            ));
        when(agentService.getReportInsight(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new ReconciliationDtos.ReportInsightDto("7d", 100, 90, 11.1, "narrative", "sensor", 60, "customer-a", 40, List.of("h1"), List.of("a1")));
        when(agentService.answerQuestion("who owes the most"))
            .thenReturn(new AnswerDtos.AgentAnswerDto("who owes the most", "receivables", "customer-a", List.of(), List.of(), List.of(), List.of()));
        when(agentService.draftOperation("purchase 20 sensor S7 from supplier-a at 35"))
            .thenReturn(new OperationDraftDtos.OperationDraftDto("purchase", "draft", "supplier", 1L, "supplier-a", List.of(), "note", false, List.of("warn"), List.of("act")));

        WorkbenchDtos.AgentWorkbenchDto workbench = service.getWorkbench(7, 6, 15);

        assertEquals(1, workbench.proactiveAnswers().size());
        assertEquals(1, workbench.proactiveDrafts().size());
        assertEquals("who owes the most", workbench.proactiveAnswers().get(0).query());
        assertEquals("supplier-a", workbench.proactiveDrafts().get(0).partnerName());
    }
}

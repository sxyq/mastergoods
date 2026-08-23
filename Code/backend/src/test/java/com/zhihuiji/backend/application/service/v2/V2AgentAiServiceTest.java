package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AccountEntity;
import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import com.zhihuiji.backend.domain.entity.CashChangeRecordEntity;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.domain.entity.InventoryMonthlyStatsEntity;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.domain.entity.PaymentEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReceiptEntity;
import com.zhihuiji.backend.domain.entity.PurchaseReturnEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.domain.entity.SalesReturnEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.application.service.v2.agent.component.AnswerSynthesizer;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTerminalStatus;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes;
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyDecision;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyGuard;
import com.zhihuiji.backend.application.service.v2.agent.component.SseStreamEmitter;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilder;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionService;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextWindowResolver;
import com.zhihuiji.backend.application.service.v2.agent.context.TokenEstimator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolExecutor;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.AccountHealthLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.CustomerProfileLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.CustomerReceivableLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.FinanceRecordLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.InventoryLowStockLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.InventoryPanoramaLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.PayOrderLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.ProductCatalogLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.PurchaseOrderLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.PurchaseTrackingLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.ReceivablePayableLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.ResultVisualizationTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.SaleOrderLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.SalesOverviewLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.SupplierPayableLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateCustomerTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateFinanceRecordTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePayOrderTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateProductTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePurchaseOrderTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateSaleOrderTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateSupplierTool;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentConversationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountRepository;
import com.zhihuiji.backend.infrastructure.repository.AccountTransferRepository;
import com.zhihuiji.backend.infrastructure.repository.CashChangeRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.InventoryMonthlyStatsRepository;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.access.AccessDeniedException;

class V2AgentAiServiceTest {
    @Mock private CurrentOwnerService currentOwnerService;
    @Mock private AgentConversationRepository agentConversationRepository;
    @Mock private AgentMessageRepository agentMessageRepository;
    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private AgentTaskRepository agentTaskRepository;
    @Mock private AgentNotificationRepository agentNotificationRepository;
    @Mock private AgentRunAuditRepository agentRunAuditRepository;
    @Mock private AgentRunAuditEventRepository agentRunAuditEventRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountTransferRepository accountTransferRepository;
    @Mock private CashChangeRecordRepository cashChangeRecordRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private MediaStorageService mediaStorageService;
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PayOrderRepository payOrderRepository;
    @Mock private FinanceRecordRepository financeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SalesReturnRepository salesReturnRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;
    @Mock private InventoryMonthlyStatsRepository inventoryMonthlyStatsRepository;
    @Mock private PurchaseReceiptRepository purchaseReceiptRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;
    @Mock private com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private LongCatAnthropicClient longCatAnthropicClient;
    @Mock private AgentContextCheckpointRepository agentContextCheckpointRepository;

    private V2AgentAiService service;
    private List<AgentMessageEntity> agentMessages;
    private Map<String, AgentRunAuditEntity> runAudits;
    private List<AgentRunAuditEventEntity> runAuditEvents;
    private ObjectMapper objectMapper;
    private RunAuditService runAuditService;
    private SseStreamEmitter sseStreamEmitter;
    private SafetyGuard safetyGuard;
    private ToolRegistry toolRegistry;
    private ToolPlanner toolPlanner;
    private AnswerSynthesizer answerSynthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agentMessages = new CopyOnWriteArrayList<>();
        runAudits = new HashMap<>();
        runAuditEvents = new CopyOnWriteArrayList<>();
        objectMapper = new ObjectMapper();
        runAuditService = new RunAuditService(
            agentRunAuditRepository,
            agentRunAuditEventRepository,
            objectMapper
        );
        sseStreamEmitter = new SseStreamEmitter(objectMapper, runAuditService);
        safetyGuard = new SafetyGuard(longCatAnthropicClient, objectMapper, currentOwnerService);
        toolRegistry = new ToolRegistry(java.util.List.of(
            new CustomerReceivableLookupTool(customerRepository),
            new CustomerProfileLookupTool(customerRepository, saleOrderRepository, paymentRepository, salesReturnRepository, objectMapper),
            new SupplierPayableLookupTool(supplierRepository),
            new AccountHealthLookupTool(accountRepository, accountTransferRepository, cashChangeRecordRepository, financeRecordRepository, objectMapper),
            new ProductCatalogLookupTool(productRepository),
            new InventoryPanoramaLookupTool(productRepository, inventoryMonthlyStatsRepository, saleOrderItemRepository, objectMapper),
            new SaleOrderLookupTool(saleOrderRepository, saleOrderItemRepository),
            new PurchaseOrderLookupTool(purchaseOrderRepository, purchaseOrderItemRepository),
            new PurchaseTrackingLookupTool(purchaseOrderRepository, purchaseReceiptRepository, purchaseReturnRepository, objectMapper),
            new PayOrderLookupTool(payOrderRepository),
            new FinanceRecordLookupTool(financeRecordRepository),
            new ReceivablePayableLookupTool(customerRepository, supplierRepository),
            new SalesOverviewLookupTool(saleOrderRepository, productRepository, customerRepository),
            new InventoryLowStockLookupTool(productRepository),
            new ResultVisualizationTool(objectMapper),
            new CreateCustomerTool(agentDraftRepository),
            new CreateSupplierTool(agentDraftRepository),
            new CreateProductTool(agentDraftRepository),
            new CreateSaleOrderTool(agentDraftRepository),
            new CreatePurchaseOrderTool(agentDraftRepository),
            new CreatePayOrderTool(agentDraftRepository),
            new CreateFinanceRecordTool(agentDraftRepository)
        ));
        toolPlanner = new ToolPlanner(longCatAnthropicClient, toolRegistry, objectMapper);
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry, currentOwnerService);
        answerSynthesizer = spy(new AnswerSynthesizer(
            longCatAnthropicClient,
            sseStreamEmitter,
            runAuditService,
            agentMessageRepository,
            objectMapper,
            toolPlanner
        ));
        AgentLlmProperties llmProperties = new AgentLlmProperties();
        llmProperties.setModel("test-model");
        llmProperties.setWireApi("anthropic");
        TokenEstimator tokenEstimator = new TokenEstimator();
        ContextWindowResolver windowResolver = new ContextWindowResolver(llmProperties);
        ContextBuilder contextBuilder = new ContextBuilder(
            agentMessageRepository,
            agentContextCheckpointRepository,
            windowResolver,
            tokenEstimator,
            llmProperties
        );
        ContextCompactionService contextCompactionService = new ContextCompactionService(
            agentContextCheckpointRepository,
            longCatAnthropicClient,
            llmProperties,
            objectMapper,
            tokenEstimator
        );
        service = new V2AgentAiService(
            currentOwnerService,
            agentConversationRepository,
            agentMessageRepository,
            agentDraftRepository,
            agentTaskRepository,
            agentNotificationRepository,
            agentRunAuditRepository,
            agentRunAuditEventRepository,
            mediaAssetRepository,
            mediaStorageService,
            objectMapper,
            longCatAnthropicClient,
            toolRegistry,
            toolExecutor,
            runAuditService,
            sseStreamEmitter,
            safetyGuard,
            toolPlanner,
            answerSynthesizer,
            contextBuilder,
            contextCompactionService
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("disabled");
        when(longCatAnthropicClient.streamingUnavailableStatus()).thenReturn("disabled");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(false);
        when(agentConversationRepository.save(any(AgentConversationEntity.class))).thenAnswer(invocation -> {
            AgentConversationEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 101L);
            }
            return entity;
        });
        when(agentMessageRepository.save(any(AgentMessageEntity.class))).thenAnswer(invocation -> {
            AgentMessageEntity entity = invocation.getArgument(0);
            agentMessages.add(entity);
            return entity;
        });
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 501L);
            }
            return entity;
        });
        when(agentRunAuditRepository.save(any(AgentRunAuditEntity.class))).thenAnswer(invocation -> {
            AgentRunAuditEntity entity = invocation.getArgument(0);
            runAudits.put(entity.getRunId(), entity);
            return entity;
        });
        when(agentRunAuditRepository.findByRunId(anyString())).thenAnswer(invocation ->
            Optional.ofNullable(runAudits.get(invocation.getArgument(0, String.class)))
        );
        when(agentRunAuditRepository.findByRunIdAndOwnerUserId(anyString(), any())).thenAnswer(invocation -> {
            AgentRunAuditEntity entity = runAudits.get(invocation.getArgument(0, String.class));
            Long ownerUserId = invocation.getArgument(1, Long.class);
            return entity != null && entity.getOwnerUserId().equals(ownerUserId) ? Optional.of(entity) : Optional.empty();
        });
        when(agentRunAuditEventRepository.save(any(AgentRunAuditEventEntity.class))).thenAnswer(invocation -> {
            AgentRunAuditEventEntity entity = invocation.getArgument(0);
            runAuditEvents.add(entity);
            return entity;
        });
        when(agentRunAuditEventRepository.findAllByRunIdAndOwnerUserIdOrderBySeqAsc(anyString(), any())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0, String.class);
            return runAuditEvents.stream()
                .filter(event -> runId.equals(event.getRunId()))
                .sorted((left, right) -> Integer.compare(left.getSeq(), right.getSeq()))
                .toList();
        });
        when(agentRunAuditEventRepository.countByRunIdAndOwnerUserId(anyString(), any())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0, String.class);
            return runAuditEvents.stream()
                .filter(event -> runId.equals(event.getRunId()))
                .count();
        });
    }

    @Test
    void blockedRequestPersistsSafeTerminalMessageWithoutSyntheticAnswer() {
        AgentConversationEntity conversation = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));
        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(7L, "DROP TABLE products", false)
        );

        assertNull(response.answer());
        assertEquals(false, response.safetyPassed());
        assertEquals("blocked", response.mode());
        assertMessagesForRun(response.runId(), "blocked");
        AgentMessageEntity blockedMessage = agentMessages.stream()
            .filter(message -> "assistant".equals(message.getRole()))
            .findFirst()
            .orElseThrow();
        assertEquals("本次请求未执行：安全策略已拦截该请求。", blockedMessage.getContent());
        assertEquals(blockedMessage.getContent(), conversation.getLatestSummary());
        assertEquals(conversation.getLastMessageAt(), conversation.getUpdatedAt());
        verify(agentMessageRepository, times(2)).save(any(AgentMessageEntity.class));
        verifyNoInteractions(longCatAnthropicClient);
    }

    @Test
    void emptyReceivableResultDoesNotEmitInvalidBarChart() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(0L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(0.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前没有欠款客户，应收总额为 0。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况，用图表展示", false)
        );

        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertFalse(hasBlock(response, "bar_chart"));
        assertEquals("tool_query_llm", response.mode());
        assertEquals("completed", response.llmStatus());
    }

    @Test
    void nativePlanExecutesDistinctCallsToTheSameReadToolOnceEach() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "模型选择商品查询",
            "native_tool_use",
            Map.of(),
            "response-products",
            List.of(
                new AgentTypes.NativeToolCallBlock("call-products-1", "product_catalog_lookup", "{}"),
                new AgentTypes.NativeToolCallBlock("call-products-2", "product_catalog_lookup", "{}")
            )
        );
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();
        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);
        executeToolPlan.invoke(
            service,
            1L,
            9L,
            null,
            "run-duplicate-tool-calls",
            plan,
            blocks,
            results,
            failures
        );

        assertEquals(1, results.size());
        assertEquals("call-products-1", results.get(0).toolCallId());
        verify(productRepository, times(1)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void nativePlanSkipsAnExactRepeatedCallIdAndArguments() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "模型重复返回同一调用",
            "native_tool_use",
            Map.of(),
                "response-duplicate",
            List.of(
                new AgentTypes.NativeToolCallBlock("call-same", "product_catalog_lookup", "{}"),
                new AgentTypes.NativeToolCallBlock("call-other", "product_catalog_lookup", "{}")
            )
        );
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();
        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);

        executeToolPlan.invoke(
            service,
            1L,
            9L,
            null,
            "run-duplicate-exact-call",
            plan,
            new ArrayList<>(),
            results,
            failures
        );

        assertEquals(1, results.size());
        verify(productRepository, times(1)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void laterPlanSkipsSameSemanticReadWithDifferentCallId() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        Class<?> budgetType = Class.forName(
            "com.zhihuiji.backend.application.service.v2.V2AgentAiService$ToolExecutionBudget"
        );
        Constructor<?> budgetConstructor = budgetType.getDeclaredConstructor(int.class);
        budgetConstructor.setAccessible(true);
        Object budget = budgetConstructor.newInstance(5);
        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class,
            java.util.Set.class,
            budgetType,
            com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState.class
        );
        executeToolPlan.setAccessible(true);

        AgentTypes.AgentToolPlan firstPlan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "第一轮查询",
            "native_tool_use",
            Map.of(),
            "response-first",
            List.of(new AgentTypes.NativeToolCallBlock("call-first", "product_catalog_lookup", "{\"keyword\":\"a\"}"))
        );
        AgentTypes.AgentToolPlan laterPlan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "第二轮重复查询",
            "native_tool_use_react_continuation",
            Map.of(),
            "response-later",
            List.of(new AgentTypes.NativeToolCallBlock("call-later", "product_catalog_lookup", "{\"keyword\":\"a\"}"))
        );
        CapturingEmitter emitter = new CapturingEmitter();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        java.util.Set<String> invocationKeys = new LinkedHashSet<>();

        executeToolPlan.invoke(service, 1L, 9L, emitter, "run-semantic-dedupe",
            firstPlan, blocks, results, failures, invocationKeys, budget, null);
        executeToolPlan.invoke(service, 1L, 9L, emitter, "run-semantic-dedupe",
            laterPlan, blocks, results, failures, invocationKeys, budget, null);

        assertEquals(1, results.size());
        assertTrue(emitter.containsPayload("duplicate_tool_semantic_key"));
        assertTrue(emitter.containsPayload("call-later"));
        verify(productRepository, times(1)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void missingCreateParametersAreRecordedForDependencyContinuation() throws Exception {
        JsonNode invalidSaleOrder = objectMapper.createObjectNode()
            .put("customer_id", 0L)
            .putArray("items")
            .addObject()
            .put("product_id", 0L)
            .put("quantity", 1)
            .put("price", 1.23);
        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("create_sale_order"),
            "销售草稿",
            "native_tool_use",
            Map.of("create_sale_order", invalidSaleOrder),
            "response-sale-missing-ids",
            List.of(new AgentTypes.NativeToolCallBlock(
                "call-sale-missing-ids",
                "create_sale_order",
                invalidSaleOrder.toString()
            ))
        );
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();

        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);
        executeToolPlan.invoke(
            service,
            1L,
            9L,
            null,
            "run-sale-missing-ids",
            plan,
            new ArrayList<>(),
            new ArrayList<>(),
            failures
        );

        assertEquals(1, failures.size());
        assertEquals("create_sale_order", failures.get(0).toolName());
        assertEquals("call-sale-missing-ids", failures.get(0).toolCallId());
        assertTrue(failures.get(0).safeMessage().contains("必填参数缺失"));
        verifyNoInteractions(agentDraftRepository);
    }

    @Test
    void nativePlanAllowsDifferentSemanticArgumentsEvenWhenCallIdRepeats() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "模型重复使用同一个 provider call id",
            "native_tool_use",
            Map.of(),
            "response-duplicate-arguments",
            List.of(
                new AgentTypes.NativeToolCallBlock("call-same", "product_catalog_lookup", "{\"keyword\":\"a\"}"),
                new AgentTypes.NativeToolCallBlock("call-same", "product_catalog_lookup", "{\"keyword\":\"b\"}")
            )
        );
        CapturingEmitter emitter = new CapturingEmitter();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();

        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);
        executeToolPlan.invoke(
            service,
            1L,
            9L,
            emitter,
            "run-duplicate-arguments",
            plan,
            new ArrayList<>(),
            results,
            failures
        );

        assertEquals(2, results.size());
        assertEquals(0, emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_skipped\""))
            .count());
        verify(productRepository, times(2)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void continuationPlanSkipsRepeatedReadWithSameSemanticArguments() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "模型续轮重复选择同一查询工具",
            "native_tool_use_react_continuation",
            Map.of(),
            "response-repeat-read-tool",
            List.of(
                new AgentTypes.NativeToolCallBlock("call-read-1", "product_catalog_lookup", "{}"),
                new AgentTypes.NativeToolCallBlock("call-read-2", "product_catalog_lookup", "{}")
            )
        );
        CapturingEmitter emitter = new CapturingEmitter();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();
        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);

        executeToolPlan.invoke(
            service,
            1L,
            9L,
            emitter,
            "run-repeat-read-tool",
            plan,
            new ArrayList<>(),
            results,
            failures
        );

        assertEquals(1, results.size());
        assertTrue(emitter.containsPayload("duplicate_tool_semantic_key"));
        verify(productRepository, times(1)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void continuationPlanExecutesSameReadToolWithDifferentArguments() throws Exception {
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(productRepository.countByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(0D);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("product_catalog_lookup"),
            "模型续轮以不同参数查询同一工具",
            "native_tool_use_react_continuation",
            Map.of(),
            "response-distinct-read-args",
            List.of(
                new AgentTypes.NativeToolCallBlock("call-read-keyword-a", "product_catalog_lookup", "{\"keyword\":\"a\"}"),
                new AgentTypes.NativeToolCallBlock("call-read-keyword-b", "product_catalog_lookup", "{\"keyword\":\"b\"}")
            )
        );
        CapturingEmitter emitter = new CapturingEmitter();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();
        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);

        executeToolPlan.invoke(
            service,
            1L,
            9L,
            emitter,
            "run-distinct-read-args",
            plan,
            new ArrayList<>(),
            results,
            failures
        );

        assertEquals(2, results.size());
        assertEquals(0, emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_skipped\""))
            .count());
        verify(productRepository, times(2)).findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any());
    }

    @Test
    void selectionFailedPlanOnlyEmitsSkippedEventsAndNeverExecutesNativeCalls() throws Exception {
        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of(),
            "模型工具选择失败：澄清轮仍选择多个互斥只读工具",
            "model_tool_selection_failed",
            Map.of(),
            "response-selection-failed",
            List.of(
                new AgentTypes.NativeToolCallBlock(
                    "call-original-health",
                    "account_health_lookup",
                    "{\"limit\":5}"
                ),
                new AgentTypes.NativeToolCallBlock(
                    "call-clarified-balance",
                    "account_balance_lookup",
                    "{\"limit\":5}"
                )
            )
        );
        CapturingEmitter emitter = new CapturingEmitter();
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        List<AgentTypes.ToolExecutionResult> results = new ArrayList<>();
        List<AgentTypes.ToolFailureResult> failures = new ArrayList<>();

        Method executeToolPlan = V2AgentAiService.class.getDeclaredMethod(
            "executeToolPlan",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            AgentTypes.AgentToolPlan.class,
            List.class,
            List.class,
            List.class
        );
        executeToolPlan.setAccessible(true);
        executeToolPlan.invoke(
            service,
            1L,
            9L,
            emitter,
            "run-selection-failed",
            plan,
            blocks,
            results,
            failures
        );

        assertTrue(results.isEmpty());
        assertTrue(failures.isEmpty());
        assertTrue(blocks.isEmpty());
        assertEquals(2, emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_skipped\""))
            .count());
        assertTrue(emitter.containsPayload("model_tool_selection_failed"));
        assertTrue(emitter.containsPayload("call-original-health"));
        assertTrue(emitter.containsPayload("call-clarified-balance"));
        assertTrue(emitter.containsPayload("account_health_lookup"));
        assertTrue(emitter.containsPayload("account_balance_lookup"));
        assertTrue(emitter.containsPayload("\"trace_id\""));
        assertTrue(emitter.containsPayload("\"audit_id\""));
        verifyNoInteractions(accountRepository, accountTransferRepository, cashChangeRecordRepository,
            financeRecordRepository, productRepository, customerRepository, supplierRepository,
            saleOrderRepository, purchaseOrderRepository, payOrderRepository, paymentRepository);
    }

    @Test
    void selectionFailedPlanStopsReactLoopAndReturnsEmptyResponsePayload() throws Exception {
        AgentTypes.AgentToolPlan failedPlan = new AgentTypes.AgentToolPlan(
            List.of(),
            "模型工具选择失败：澄清轮仍选择多个互斥只读工具",
            "model_tool_selection_failed",
            Map.of(),
            "response-selection-failed-react",
            List.of(
                new AgentTypes.NativeToolCallBlock(
                    "call-original-health-react",
                    "account_health_lookup",
                    "{\"limit\":5}"
                ),
                new AgentTypes.NativeToolCallBlock(
                    "call-clarified-balance-react",
                    "account_balance_lookup",
                    "{\"limit\":5}"
                )
            )
        );
        ToolPlanner plannerSpy = spy(toolPlanner);
        doReturn(failedPlan).when(plannerSpy).planTools(anyString(), anyList(), any());
        ToolRegistry registrySpy = spy(toolRegistry);
        Field plannerField = V2AgentAiService.class.getDeclaredField("toolPlanner");
        plannerField.setAccessible(true);
        plannerField.set(service, plannerSpy);
        Field registryField = V2AgentAiService.class.getDeclaredField("toolRegistry");
        registryField.setAccessible(true);
        registryField.set(service, registrySpy);

        CapturingEmitter emitter = new CapturingEmitter();
        Method buildResponse = V2AgentAiService.class.getDeclaredMethod(
            "buildResponse",
            Long.class,
            Long.class,
            String.class,
            List.class,
            String.class,
            SseEmitter.class,
            String.class
        );
        buildResponse.setAccessible(true);
        AgentTypes.AgentRunOutcome outcome = (AgentTypes.AgentRunOutcome) buildResponse.invoke(
            service,
            1L,
            9L,
            "帮我看看资金账户状态。",
            List.of(),
            null,
            emitter,
            "run-selection-failed-react"
        );
        AgentTypes.ResponsePayload payload = outcome.payload();

        assertTrue(payload.blocks().isEmpty());
        assertTrue(payload.toolResults().isEmpty());
        assertTrue(payload.toolFailures().isEmpty());
        assertEquals("model_tool_selection_failed", payload.plan().source());
        assertEquals(AgentTerminalStatus.FAILED, outcome.terminalStatus());
        assertEquals(2, emitter.payloads.stream()
            .filter(value -> value.contains("\"event_type\":\"tool_skipped\""))
            .count());
        verify(plannerSpy, never()).planNextIteration(
            anyString(), any(AgentTypes.AgentToolPlan.class), anyList(), anyList(), eq(2)
        );
        verify(longCatAnthropicClient, never()).continueWithToolOutputs(
            any(), anyString(), anyString(), anyList(), anyList(), anyList()
        );
        verify(registrySpy, never()).executeTool(anyString(), any(), any());
        verifyNoInteractions(accountRepository, accountTransferRepository, cashChangeRecordRepository,
            financeRecordRepository, productRepository, customerRepository, supplierRepository,
            saleOrderRepository, purchaseOrderRepository, payOrderRepository, paymentRepository);
    }

    @Test
    void registeredToolUsesCurrentCallerPermissionAndOwnerStoreContext() throws Exception {
        AtomicReference<ToolContext> captured = new AtomicReference<>();
        AgentTool probe = permissionProbe("agent:view", captured);
        ToolRegistry registry = spy(new ToolRegistry(List.of(probe)));
        replaceToolRegistry(registry);
        when(currentOwnerService.requireCurrentUserId()).thenReturn(9L);
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.of(4L));

        Optional<ToolResult> result = invokeRegisteredTool(
            2L, 3L, null, "run-permission-positive", "permission_probe", objectMapper.createObjectNode()
        );

        assertTrue(result.isPresent() && result.get().success());
        assertEquals(2L, captured.get().ownerUserId());
        assertEquals(9L, captured.get().userId());
        assertEquals(4L, captured.get().storeId());
        verify(currentOwnerService).requirePermissions("agent:view");
        verify(registry).executeTool(eq("permission_probe"), any(ToolContext.class), any(JsonNode.class));
    }

    @Test
    void deniedCallerPermissionStopsToolBeforeBusinessExecution() throws Exception {
        AtomicReference<ToolContext> captured = new AtomicReference<>();
        AgentTool probe = permissionProbe("agent:write", captured);
        ToolRegistry registry = spy(new ToolRegistry(List.of(probe)));
        replaceToolRegistry(registry);
        doThrow(new AccessDeniedException("denied"))
            .when(currentOwnerService).requirePermissions("agent:write");

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
            invokeRegisteredTool(2L, 3L, null, "run-permission-negative", "permission_probe",
                objectMapper.createObjectNode())
        );

        assertTrue(thrown.getCause() instanceof AccessDeniedException);
        assertNull(captured.get());
        verify(registry, never()).executeTool(anyString(), any(ToolContext.class), any(JsonNode.class));
    }

    private AgentTool permissionProbe(String permission, AtomicReference<ToolContext> captured) {
        return new AgentTool() {
            @Override public String name() { return "permission_probe"; }
            @Override public String displayName() { return "权限测试工具"; }
            @Override public String description() { return "权限测试工具"; }
            @Override public ToolType type() { return ToolType.READ_ONLY; }
            @Override public String requiredPermission() { return permission; }
            @Override public ToolResult execute(ToolContext ctx, JsonNode params) {
                captured.set(ctx);
                return ToolResult.empty("permission probe");
            }
        };
    }

    private void replaceToolRegistry(ToolRegistry registry) throws Exception {
        Field field = V2AgentAiService.class.getDeclaredField("toolRegistry");
        field.setAccessible(true);
        field.set(service, registry);
    }

    @SuppressWarnings("unchecked")
    private Optional<ToolResult> invokeRegisteredTool(
        Long ownerUserId,
        Long conversationId,
        SseEmitter emitter,
        String runId,
        String tool,
        JsonNode params
    ) throws Exception {
        Method method = V2AgentAiService.class.getDeclaredMethod(
            "executeRegisteredTool",
            Long.class,
            Long.class,
            SseEmitter.class,
            String.class,
            String.class,
            JsonNode.class
        );
        method.setAccessible(true);
        return (Optional<ToolResult>) method.invoke(
            service, ownerUserId, conversationId, emitter, runId, tool, params
        );
    }

    @Test
    void chatWithImageAttachmentsUsesMultimodalDirectAnswerPath() throws Exception {
        MediaAssetEntity imageAsset = new MediaAssetEntity();
        imageAsset.setOwnerUserId(1L);
        imageAsset.setMimeType("image/png");
        imageAsset.setObjectKey("media/agent-image.png");
        when(mediaAssetRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(imageAsset));
        when(mediaStorageService.load("media/agent-image.png")).thenReturn("fake-image".getBytes());
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString(), org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(Optional.of("图片里是一张商品标签"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "帮我看看这张图", false, List.of(9L))
        );

        assertEquals("图片里是一张商品标签", response.answer());
        assertEquals("multimodal_direct_llm", response.mode());
        assertMessagesForRun(response.runId(), "text");
        verify(longCatAnthropicClient).createJsonMessage(anyString(), anyString(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void nonStreamingLlmFailurePersistsTerminalMessageWithRunId() throws Exception {
        AgentConversationEntity conversation = conversation(8L);
        when(agentConversationRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(conversation));
        MediaAssetEntity imageAsset = new MediaAssetEntity();
        imageAsset.setOwnerUserId(1L);
        imageAsset.setMimeType("image/png");
        imageAsset.setObjectKey("media/agent-image.png");
        when(mediaAssetRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(imageAsset));
        when(mediaStorageService.load("media/agent-image.png")).thenReturn("fake-image".getBytes());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(8L, "帮我看看这张图", false, List.of(9L))
        );

        assertEquals("llm_required", response.mode());
        assertEquals("failed", runAudits.get(response.runId()).getStatus());
        assertEquals("暂时无法完成这次请求，请稍后重试。", conversation.getLatestSummary());
        assertEquals(conversation.getLastMessageAt(), conversation.getUpdatedAt());
        assertMessagesForRun(response.runId(), "failed");
    }

    @Test
    void emptySupplierPayableResultDoesNotEmitInvalidBarChart() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-payable",
                    "supplier_payable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询供应商应付"
            )))
            .thenReturn(Optional.empty());
        when(supplierRepository.findPayablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(0L);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(0.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前没有应付供应商，应付总额为 0。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "供应商应付情况，用图表展示", false)
        );

        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertFalse(hasBlock(response, "bar_chart"));
        verify(supplierRepository).findPayablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any());
    }

    @Test
    void workbenchShowsOnlyRealRecentConversations() {
        when(agentConversationRepository.findAllWithMessagesByOwnerUserIdOrderByUpdatedAtDescIdDesc(
            1L,
            PageRequest.of(0, 5)
        )).thenReturn(List.of());

        V2AgentDtos.AgentWorkbenchResponse response = service.getWorkbench();

        assertTrue(response.kpiCards().isEmpty());
        assertTrue(response.riskAlerts().isEmpty());
        assertTrue(response.todaySummary() == null || response.todaySummary().isBlank());
        assertTrue(response.quickQuestions().isEmpty());
        assertTrue(response.quickQuestions().stream().noneMatch(V2AgentAiServiceTest::isReportLikeQuestion));
        assertTrue(response.recentConversations().isEmpty());
        assertTrue(response.pendingDrafts().isEmpty());
        assertTrue(response.greeting().isBlank());
        assertFalse(isReportLikeQuestion(response.greeting()));
        assertEquals("clean_entry_ready", response.status());
        assertNull(response.dataPolicy());
        assertTrue(response.capabilities().isEmpty());
        assertTrue(response.warnings().isEmpty());
        verify(currentOwnerService).requireCurrentOwnerUserId();
        verify(agentConversationRepository).findAllWithMessagesByOwnerUserIdOrderByUpdatedAtDescIdDesc(1L, PageRequest.of(0, 5));
        verifyNoInteractions(agentDraftRepository, agentMessageRepository);
    }

    @Test
    void workbenchIncludesRealConversationPreviewAndMessageCount() {
        AgentConversationEntity conversation = new AgentConversationEntity();
        setId(conversation, 42L);
        conversation.setTitle("库存查询");
        conversation.setStatus("active");
        conversation.setUpdatedAt(2_000L);
        conversation.setLastMessageAt(3_000L);
        conversation.setLatestSummary("当前有 3 个低库存商品");
        when(agentConversationRepository.findAllWithMessagesByOwnerUserIdOrderByUpdatedAtDescIdDesc(
            1L,
            PageRequest.of(0, 5)
        )).thenReturn(List.of(conversation));
        when(agentMessageRepository.countByOwnerUserIdAndConversationIdInGroupBy(1L, List.of(42L)))
            .thenReturn(List.<Object[]>of(new Object[] {42L, 2L}));

        V2AgentDtos.AgentWorkbenchResponse response = service.getWorkbench();

        assertEquals(1, response.recentConversations().size());
        V2AgentDtos.RecentConversationItem recent = response.recentConversations().getFirst();
        assertEquals(42L, recent.id());
        assertEquals("库存查询", recent.title());
        assertEquals(3_000L, recent.lastMessageAt());
        assertEquals(2, recent.messageCount());
        assertEquals("当前有 3 个低库存商品", recent.latestSummary());
    }

    @Test
    void chatRejectsMoreThanNineImageAttachmentsBeforeLoadingAssets() {
        List<Long> imageAssetIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.chat(new V2AgentDtos.AgentChatRequest(null, "分析这些图片", false, imageAssetIds))
        );

        verifyNoInteractions(mediaAssetRepository, mediaStorageService);
    }

    @Test
    void nonStreamingBlankAnswerPersistsTerminalFailureMessage() {
        doReturn(new AgentTypes.FinalAnswer("", "unexpected_empty_answer", "completed", true))
            .when(answerSynthesizer)
            .buildFinalAnswer(anyString(), any(AgentTypes.ResponsePayload.class), anyList(), any());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "查询商品", false)
        );

        assertEquals("unexpected_empty_answer", response.mode());
        assertEquals("failed", runAudits.get(response.runId()).getStatus());
        assertMessagesForRun(response.runId(), "failed");
    }

    @Test
    void chatStreamDoesNotRequeryCurrentOwnerInsideAsyncWorker() throws Exception {
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of());

        service.chatStream(new V2AgentDtos.AgentChatRequest(null, "客户应收情况", true));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            boolean completed = runAudits.values().stream().anyMatch(audit ->
                "completed".equals(audit.getStatus()) || "blocked".equals(audit.getStatus()) || "failed".equals(audit.getStatus()));
            if (completed) {
                break;
            }
            Thread.sleep(10L);
        }

        assertTrue(runAudits.values().stream().anyMatch(audit ->
            "completed".equals(audit.getStatus()) || "failed".equals(audit.getStatus())), runAudits.toString());
        AgentMessageEntity userMessage = agentMessages.stream()
            .filter(message -> "user".equals(message.getRole()))
            .findFirst()
            .orElseThrow();
        assertNotNull(userMessage.getRunId());
        List<AgentMessageEntity> runMessages = agentMessages.stream()
            .filter(message -> userMessage.getRunId().equals(message.getRunId()))
            .toList();
        assertEquals(2, runMessages.size(), runMessages.toString());
        assertTrue(runMessages.stream().anyMatch(message -> "assistant".equals(message.getRole())));
        verify(currentOwnerService, times(1)).requireCurrentOwnerUserId();
    }

    private static boolean isReportLikeQuestion(String question) {
        return question.contains("今日")
            || question.contains("今天")
            || question.contains("销售额")
            || question.contains("报表")
            || question.contains("KPI")
            || question.contains("kpi")
            || question.contains("图表")
            || question.contains("统计图")
            || question.contains("看板")
            || question.contains("摘要")
            || question.contains("排行")
            || question.contains("风险")
            || question.contains("补货");
    }

    private void assertMessagesForRun(String runId, String assistantMessageType) {
        List<AgentMessageEntity> messages = agentMessages.stream()
            .filter(message -> runId.equals(message.getRunId()))
            .toList();
        assertEquals(2, messages.size(), messages.toString());
        assertTrue(messages.stream().anyMatch(message -> "user".equals(message.getRole())));
        assertTrue(messages.stream().anyMatch(message ->
            "assistant".equals(message.getRole())
                && assistantMessageType.equals(message.getMessageType())));
        assertTrue(messages.stream().allMatch(message -> runId.equals(message.getRunId())));
    }

    @Test
    void streamLlmFailureDoesNotEmitSyntheticAnswer() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-fallback",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of());
        // LLM streaming and fallback both fail to produce a grounded answer
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.empty());
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(101L);
        when(agentConversationRepository.findByIdAndOwnerUserId(101L, 1L)).thenReturn(Optional.of(conversation));

        service.runChatStream(1L, conversation, "客户应收情况", List.of(), "run-test", emitter);

        // When LLM fails to produce a grounded answer, the run fails with error event
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"error\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"code\":\"LLM_ANSWER_UNAVAILABLE\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("暂时无法完成这次请求，请稍后重试。")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("替代文本")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_delta\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
        AgentRunAuditEntity audit = runAudits.get("run-test");
        assertNotNull(audit);
        assertEquals("failed", audit.getStatus());
        assertEquals("llm_answer_unavailable", audit.getMode());
        assertEquals("model_empty_or_ungrounded", audit.getLlmStatus());
        assertEquals("native_tool_use", audit.getPlanSource());
        assertEquals("暂时无法完成这次请求，请稍后重试。", conversation.getLatestSummary());
        assertEquals(conversation.getLastMessageAt(), conversation.getUpdatedAt());
        assertTrue(emitter.completed);
    }

    @Test
    void streamDisabledModelAnswerDoesNotEmitSyntheticAnswer() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("disabled");
        when(longCatAnthropicClient.streamingUnavailableStatus()).thenReturn("disabled");
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(102L);
        when(agentConversationRepository.findByIdAndOwnerUserId(102L, 1L)).thenReturn(Optional.of(conversation));

        service.runChatStream(1L, conversation, "客户应收情况", List.of(), "run-disabled", emitter);

        // When LLM is not configured, the run fails with llm_required mode
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"error\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"code\":\"LLM_REQUIRED\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        AgentRunAuditEntity audit = runAudits.get("run-disabled");
        assertNotNull(audit);
        assertEquals("failed", audit.getStatus());
        assertEquals("llm_required", audit.getMode());
        assertEquals("disabled", audit.getLlmStatus());
        assertEquals("llm_unavailable", audit.getPlanSource());
        assertEquals("暂时无法完成这次请求，请稍后重试。", conversation.getLatestSummary());
        assertEquals(conversation.getLastMessageAt(), conversation.getUpdatedAt());
        assertTrue(emitter.completed);
    }

    @Test
    void streamKeepsSafetyReviewInternalToTheServer() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-hidden-safety",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(103L), "客户应收情况", List.of(), "run-hidden-safety", emitter);

        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("safety_check_")),
            String.join("\n", emitter.payloads));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"tool_started\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
    }

    @Test
    void streamConfiguredModelFallsBackToRuleSummaryWhenProviderAnswerIsNotGrounded() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-model-stream",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        // LLM streaming returns a non-grounded answer (invented numbers)
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                onDelta.accept("客户A欠款9999元");
                return Optional.of("客户A欠款9999元");
            });
        // Fallback createJsonMessage also fails
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.empty());
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(104L);

        service.runChatStream(1L, conversation, "客户应收情况，用图表展示", List.of(), "run-model-stream", emitter);

        // When LLM returns a non-grounded answer, the run fails
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"error\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"code\":\"LLM_ANSWER_UNAVAILABLE\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        assertEquals(-1, payloadIndexContaining(emitter, "\"event_type\":\"result_block\""));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_delta\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
        verify(longCatAnthropicClient).streamTextMessage(anyString(), anyString(), anyString(), any());
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerStreamsVisibleChunksBeforeCompletion() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-model-batch",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "应收客户查询结果已返回。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(112L), "客户应收情况，用图表展示", List.of(), "run-model-batch", emitter);

        List<String> answerDeltaPayloads = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .toList();
        assertFalse(answerDeltaPayloads.isEmpty(), String.join("\n", emitter.payloads));
        assertTrue(answerDeltaPayloads.stream().allMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        assertEquals(-1, payloadIndexContaining(emitter, "\"event_type\":\"result_block\""));
        assertTrue(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"model_stream\"")));
    }

    @Test
    void streamConfiguredModelFallsBackWhenProviderDoesNotReturnGroundedJson() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-stream-interrupted",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        // LLM streaming returns non-grounded answer
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                onDelta.accept("客户A欠款5000元");
                return Optional.of("客户A欠款5000元");
            });
        // Fallback also fails
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.empty());
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(111L), "客户应收情况，用图表展示", List.of(), "run-stream-interrupted", emitter);

        // When LLM returns non-grounded answer, the run fails
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"error\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"code\":\"LLM_ANSWER_UNAVAILABLE\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        assertEquals(-1, payloadIndexContaining(emitter, "\"event_type\":\"result_block\""));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_delta\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
        verify(longCatAnthropicClient).streamTextMessage(anyString(), anyString(), anyString(), any());
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerEmitsServerNoticeTailBeforeCompletionWhenBackendAppendsBoundaries() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-server-notice",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        List<com.zhihuiji.backend.domain.entity.CustomerEntity> customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            customers.add(customer((long) i, "客户" + i, 100.0 + i));
        }
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(customers);
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(10L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(1055.0);
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "应收客户数量为 10，应收总额 1055。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(110L), "客户应收情况", List.of(), "run-server-notice", emitter);

        List<String> deltaPayloads = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .toList();
        assertFalse(deltaPayloads.isEmpty(), String.join("\n", emitter.payloads));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        String completed = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(completed.contains("应收客户数量为 10"), completed);
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"server_notice\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"rule_summary\"")));
    }

    @Test
    void streamToolCompletedIncludesAuditMetadata() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-audit",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0), customer(2L, "客户B", 80.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(103L);

        service.runChatStream(1L, conversation, "客户应收情况", List.of(), "run-audit", emitter);

        String completedPayload = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_completed\""))
            .findFirst()
            .orElse("");
        assertTrue(completedPayload.contains("\"tool_name\":\"customer_receivable_lookup\""), completedPayload);
        assertTrue(completedPayload.contains("\"tool_call_id\":\"call-audit\""), completedPayload);
        assertTrue(completedPayload.contains("\"selection_origin\":\"model_tool_call\""), completedPayload);
        assertTrue(completedPayload.contains("\"tool_sequence\":1"), completedPayload);
        assertTrue(completedPayload.contains("\"audit_id\":\"run-audit:audit\""), completedPayload);
        assertTrue(completedPayload.contains("\"trace_id\":\"run-audit:trace\""), completedPayload);
        assertTrue(completedPayload.contains("\"returned_count\":2"), completedPayload);
        assertTrue(completedPayload.contains("\"limit\":10"), completedPayload);
        assertTrue(completedPayload.contains("\"is_truncated\":false"), completedPayload);
        assertTrue(completedPayload.contains("\"duration_ms\""), completedPayload);
        assertTrue(completedPayload.contains("\"input_summary\":\"查询当前账号客户应收余额"), completedPayload);
        assertTrue(completedPayload.contains("\"query_window\":{\"owner_scope\":\"current_owner\"}"), completedPayload);
        assertTrue(completedPayload.contains("\"started_at\""), completedPayload);
        assertTrue(completedPayload.contains("\"completed_at\""), completedPayload);
        assertTrue(completedPayload.contains("\"next_cursor\":null"), completedPayload);
        assertTrue(completedPayload.contains("\"evidence\":{\"source\":\"tool:customer_receivable_lookup\""), completedPayload);
    }

    @Test
    void streamEmitsEachToolResultBlockBeforeNextToolCompletes() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-multi-1",
                        "inventory_low_stock_lookup",
                        objectMapper.createObjectNode()
                    ),
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-multi-2",
                        "customer_receivable_lookup",
                        objectMapper.createObjectNode()
                    )
                ),
                "查询库存和客户应收"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findLowStockProducts(1L, PageRequest.of(0, 10)))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 2.0, 10.0, 12.5)));
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "库存与客户应收查询结果已返回。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(109L), "库存和客户应收情况，用图表展示", List.of(), "run-multi-blocks", emitter);

        int inventoryCompleted = firstPayloadIndexContaining(
            emitter,
            "\"event_type\":\"tool_completed\"",
            "\"tool_name\":\"inventory_low_stock_lookup\""
        );
        int receivableCompleted = firstPayloadIndexContaining(
            emitter,
            "\"event_type\":\"tool_completed\"",
            "\"tool_name\":\"customer_receivable_lookup\""
        );
        int answerCompleted = firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\"");

        assertTrue(inventoryCompleted < receivableCompleted, String.join("\n", emitter.payloads));
        assertTrue(receivableCompleted < answerCompleted, String.join("\n", emitter.payloads));
        assertEquals(-1, payloadIndexContaining(emitter, "\"event_type\":\"result_block\""));
        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"库存风险\""));
        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"应收概览\""));
        assertTrue(receivableCompleted < answerCompleted, String.join("\n", emitter.payloads));
        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"本次回答依据\""));
    }

    @Test
    void streamPartialToolFailureEmitsToolFailedAndDoesNotInventMissingData() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-partial-1",
                        "inventory_low_stock_lookup",
                        objectMapper.createObjectNode()
                    ),
                    new LongCatAnthropicClient.ToolUseBlock(
                        "call-partial-2",
                        "customer_receivable_lookup",
                        objectMapper.createObjectNode()
                    )
                ),
                "查询库存和客户应收"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findLowStockProducts(1L, PageRequest.of(0, 10)))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 2.0, 10.0, 12.5)));
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("database timeout"));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "库存侧共发现 1 个低库存商品，部分查询失败（customer_receivable_lookup），失败部分未使用模拟数据替代。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(111L), "库存和客户应收情况，用图表展示", List.of(), "run-partial-tool-failure", emitter);

        String failedPayload = firstPayload(emitter, "\"event_type\":\"tool_failed\"");
        assertEquals(1, emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_failed\"")
                && payload.contains("\"tool_call_id\":\"call-partial-2\""))
            .count());
        assertTrue(failedPayload.contains("\"tool_name\":\"customer_receivable_lookup\""), failedPayload);
        assertTrue(failedPayload.contains("\"tool_call_id\":\"call-partial-2\""), failedPayload);
        assertTrue(failedPayload.contains("\"tool_sequence\":2"), failedPayload);
        assertTrue(failedPayload.contains("\"error_code\":\"TOOL_QUERY_FAILED\""), failedPayload);
        assertTrue(failedPayload.contains("\"safe_message\":\"IllegalStateException: database timeout\""), failedPayload);
        assertTrue(failedPayload.contains("\"input_summary\":\"查询当前账号客户应收余额"), failedPayload);
        assertTrue(failedPayload.contains("\"query_window\":{\"owner_scope\":\"current_owner\"}"), failedPayload);
        assertFalse(emitter.payloads.stream()
            .anyMatch(payload -> payload.contains("\"event_type\":\"tool_completed\"")
                && payload.contains("\"tool_name\":\"customer_receivable_lookup\"")));

        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("库存侧共发现 1 个低库存商品"), answerCompleted);
        assertTrue(answerCompleted.contains("部分查询失败"), answerCompleted);
        assertTrue(answerCompleted.contains("customer_receivable_lookup"), answerCompleted);
        assertTrue(answerCompleted.contains("失败部分未使用模拟数据替代"), answerCompleted);
        assertFalse(answerCompleted.contains("客户侧没有明显应收欠款压力"), answerCompleted);
        assertFalse(answerCompleted.contains("欠款客户总数 0 个"), answerCompleted);
        assertFalse(answerCompleted.contains("应收总额 ¥0.00"), answerCompleted);

        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"库存风险\""));
        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"本次回答依据\""));
        assertEquals(-1, payloadIndexContaining(emitter, "\"title\":\"应收概览\""));
    }

    @Test
    void streamEventsIncludeCompatibleEnvelopeMetadata() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-envelope",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(105L);

        service.runChatStream(1L, conversation, "客户应收情况，用图表展示", List.of(), "run-envelope", emitter);

        String runStarted = firstPayload(emitter, "\"event_type\":\"run_started\"");
        assertTrue(runStarted.contains("\"event_id\":\"run-envelope:1\""), runStarted);
        assertTrue(runStarted.contains("\"seq\":1"), runStarted);
        assertTrue(runStarted.contains("\"conversation_id\":105"), runStarted);

        String planDelta = firstPayload(emitter, "\"event_type\":\"plan_delta\"");
        assertTrue(planDelta.contains("\"plan_source\":\"native_tool_use\""), planDelta);
        assertTrue(planDelta.contains("customer_receivable_lookup"), planDelta);

        String toolCompleted = firstPayload(emitter, "\"event_type\":\"tool_completed\"");
        assertTrue(toolCompleted.contains("\"event_id\""), toolCompleted);
        assertTrue(toolCompleted.contains("\"seq\""), toolCompleted);
        assertTrue(toolCompleted.contains("\"conversation_id\":105"), toolCompleted);
        assertTrue(toolCompleted.contains("\"trace_id\":\"run-envelope:trace\""), toolCompleted);

        String answerDelta = firstPayload(emitter, "\"event_type\":\"answer_delta\"");
        assertTrue(answerDelta.contains("\"event_id\""), answerDelta);
        assertTrue(answerDelta.contains("\"seq\""), answerDelta);
        assertTrue(answerDelta.contains("\"conversation_id\":105"), answerDelta);
        assertTrue(answerDelta.contains("\"audit_id\":\"run-envelope:audit\""), answerDelta);
        assertTrue(answerDelta.contains("\"trace_id\":\"run-envelope:trace\""), answerDelta);
        assertEquals(-1, payloadIndexContaining(emitter, "\"event_type\":\"result_block\""));
        String runCompleted = firstPayload(emitter, "\"event_type\":\"run_completed\"");
        assertTrue(runCompleted.contains("\"llm_status\":\"completed\""), runCompleted);

        AgentRunAuditEntity audit = runAudits.get("run-envelope");
        assertNotNull(audit);
        assertEquals(1L, audit.getOwnerUserId());
        assertEquals(105L, audit.getConversationId());
        assertEquals("completed", audit.getStatus());
        assertEquals("tool_query_llm_streamed", audit.getMode());
        assertEquals("completed", audit.getLlmStatus());
        assertEquals("native_tool_use", audit.getPlanSource());
        assertEquals(1, audit.getToolCount());
        assertTrue(audit.getEventCount() > 0);
        verify(agentRunAuditRepository, times(2)).save(any(AgentRunAuditEntity.class));
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> "run-envelope".equals(event.getRunId()))
            .toList();
        assertEquals(audit.getEventCount(), events.size());
        assertTrue(events.stream().anyMatch(event -> "run_started".equals(event.getEventType())));
        assertTrue(events.stream().anyMatch(event -> "plan_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"plan_source\":\"native_tool_use\"")));
        assertTrue(events.stream().anyMatch(event -> "tool_completed".equals(event.getEventType())
            && event.getPayloadJson().contains("\"tool_name\":\"customer_receivable_lookup\"")));
        assertTrue(events.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"model_stream\"")));
        assertSequentialAuditEvents("run-envelope", events);
    }

    @Test
    void streamUsesNativeFunctionCallOutputContinuationBeforeTextStreaming() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-native-stream",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收",
                "resp-native-stream"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(
            eq(1L),
            eq(0.0),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.continueWithToolOutputs(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(),
            "客户应收总额为 100。",
            "resp-native-stream-final"
        )));
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(
            1L,
            conversation(114L),
            "查询客户应收",
            List.of(),
            "run-native-stream",
            emitter
        );

        verify(longCatAnthropicClient).continueWithToolOutputs(
            eq("resp-native-stream"),
            anyString(),
            eq("查询客户应收"),
            any(),
            any(),
            any()
        );
        verify(longCatAnthropicClient, never()).streamTextMessage(
            anyString(),
            anyString(),
            anyString(),
            any()
        );
        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("\"mode\":\"tool_query_llm_native\""), answerCompleted);
        assertTrue(answerCompleted.contains("\"llm_status\":\"native_continuation_completed\""), answerCompleted);
        assertTrue(answerCompleted.contains("客户应收总额为 100"), answerCompleted);
        assertTrue(firstPayload(emitter, "\"event_type\":\"answer_delta\"")
            .contains("\"delta_source\":\"model_native_continuation\""));
        String runCompleted = firstPayload(emitter, "\"event_type\":\"run_completed\"");
        assertTrue(runCompleted.contains("\"llm_status\":\"native_continuation_completed\""), runCompleted);
    }

    @Test
    void nativeContinuationMapsDuplicateToolResultsByCallId() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.continueWithToolOutputs(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
            List.of(),
            "两次查询金额分别为 100 和 200。",
            "resp-duplicate-final"
        )));

        AgentTypes.ToolExecutionResult firstResult = new AgentTypes.ToolExecutionResult(
            "customer_receivable_lookup",
            "第一条结果",
            objectMapper.createObjectNode().put("amount", 100),
            false,
            "call-duplicate-2",
            2
        );
        AgentTypes.ToolExecutionResult secondResult = new AgentTypes.ToolExecutionResult(
            "customer_receivable_lookup",
            "第二条结果",
            objectMapper.createObjectNode().put("amount", 200),
            false,
            "call-duplicate-1",
            1
        );
        AgentTypes.AgentToolPlan plan = new AgentTypes.AgentToolPlan(
            List.of("customer_receivable_lookup"),
            "重复调用",
            "native_tool_use",
            Map.of(),
            "resp-duplicate",
            List.of(
                new AgentTypes.NativeToolCallBlock(
                    "call-duplicate-1",
                    "customer_receivable_lookup",
                    "{}"
                ),
                new AgentTypes.NativeToolCallBlock(
                    "call-duplicate-2",
                    "customer_receivable_lookup",
                    "{}"
                )
            )
        );
        AgentTypes.ResponsePayload payload = new AgentTypes.ResponsePayload(
            List.of(),
            List.of(firstResult, secondResult),
            List.of(),
            plan
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "查询两次客户应收",
            payload,
            List.of(),
            null
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LongCatAnthropicClient.FunctionCallOutputItem>> outputsCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(longCatAnthropicClient).continueWithToolOutputs(
            eq("resp-duplicate"),
            anyString(),
            eq("查询两次客户应收"),
            any(),
            outputsCaptor.capture(),
            any()
        );
        assertEquals(2, outputsCaptor.getValue().size());
        assertTrue(outputsCaptor.getValue().get(0).output().contains("\"amount\":200"));
        assertTrue(outputsCaptor.getValue().get(1).output().contains("\"amount\":100"));
        assertEquals("两次查询金额分别为 100 和 200。", answer.answer());
        assertEquals("tool_query_llm_native", answer.mode());
    }

    @Test
    void removesModelTableWhenToolAlreadyReturnedVisibleTableBlock() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.of(
            "好的，这是您最近的账户转账明细：\n\n"
                + "| 交易号 | 转出账户 | 转入账户 | 金额 | 状态 |\n"
                + "|--------|----------|----------|------|------|\n"
                + "| T-1 | 账户（ID:1） | 账户（ID:2） | ¥50.00 | 已完成 |\n\n"
                + "账户当前共有 2 个，余额合计 ¥1099.00。"
        ));
        JsonNode facts = objectMapper.createObjectNode()
            .put("transfer_count", 1)
            .put("total_amount", "¥50.00")
            .put("total_fee", "¥0.00")
            .put("account_count", 2)
            .put("total_balance", "¥1099.00")
            .put("from_account_id", 1)
            .put("to_account_id", 2)
            .put("from_balance", "¥1000.00")
            .put("to_balance", "¥99.00");
        V2AgentDtos.ResultBlockDto table = new V2AgentDtos.ResultBlockDto(
            "table",
            "账户转账记录",
            objectMapper.createObjectNode()
        );
        AgentTypes.ResponsePayload payload = new AgentTypes.ResponsePayload(
            List.of(table),
            List.of(new AgentTypes.ToolExecutionResult(
                "account_transfer_lookup",
                "最近账户转账 1 条，转账总额 ¥50.00",
                facts,
                false
            )),
            List.of(),
            new AgentTypes.AgentToolPlan(
                List.of("account_transfer_lookup"),
                "查询账户转账",
                "react_iterated",
                Map.of()
            )
        );

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "最近账户之间转过哪些钱？",
            payload,
            List.of(),
            null
        );

        assertEquals("tool_query_llm", answer.mode());
        assertTrue(answer.answer().contains("账户转账明细"));
        assertTrue(answer.answer().contains("余额合计 ¥1099.00"));
        assertFalse(answer.answer().contains("|--------|"));
        assertFalse(answer.answer().contains("| 交易号 |"));
    }

    @Test
    void streamNativeToolUseCreateCustomerEmitsDraftCreatedEvent() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call_stream_1",
                    "create_customer",
                    objectMapper.createObjectNode()
                        .put("name", "李四")
                        .put("phone", "13812345678")
                        .put("remark", "流式草稿")
                )),
                "直接生成客户草稿"
            )));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "生成草稿 新建客户：李四，请确认后执行创建。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(113L), "帮我新建客户李四 电话13812345678", List.of(), "run-draft-stream", emitter);

        String planDelta = firstPayload(emitter, "\"event_type\":\"plan_delta\"");
        assertTrue(planDelta.contains("\"plan_source\":\"native_tool_use\""), planDelta);
        assertTrue(planDelta.contains("create_customer"), planDelta);

        String toolCompleted = firstPayload(emitter, "\"event_type\":\"tool_completed\"");
        assertTrue(toolCompleted.contains("\"tool_name\":\"create_customer\""), toolCompleted);

        String draftCreated = firstPayload(emitter, "\"event_type\":\"draft_created\"");
        assertTrue(draftCreated.contains("\"draft_id\":501"), draftCreated);
        assertTrue(draftCreated.contains("\"draft_type\":\"create_customer\""), draftCreated);
        assertTrue(draftCreated.contains("\"tool_name\":\"create_customer\""), draftCreated);
        assertTrue(draftCreated.contains("新建客户：李四"), draftCreated);

        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("\"plan_source\":\"native_tool_use\""), answerCompleted);
        assertTrue(answerCompleted.contains("生成草稿 新建客户：李四"), answerCompleted);

        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"tool_completed\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"draft_created\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"draft_created\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\""),
            String.join("\n", emitter.payloads)
        );

        assertTrue(runAuditEvents.stream().anyMatch(event -> "draft_created".equals(event.getEventType())
            && event.getPayloadJson().contains("\"draft_type\":\"create_customer\"")));
        assertTrue(runAuditEvents.stream().anyMatch(event -> "plan_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"plan_source\":\"native_tool_use\"")));
        ArgumentCaptor<AgentDraftEntity> draftCaptor = ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(draftCaptor.capture());
        assertEquals(113L, draftCaptor.getValue().getConversationId());
    }

    @Test
    void slowAuditEventWriteDoesNotBlockVisibleStreamPayloads() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-slow-audit",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CountDownLatch firstAuditWriteStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstAuditWrite = new CountDownLatch(1);
        AtomicBoolean blockedFirstWrite = new AtomicBoolean(false);
        when(agentRunAuditEventRepository.save(any(AgentRunAuditEventEntity.class))).thenAnswer(invocation -> {
            AgentRunAuditEventEntity entity = invocation.getArgument(0);
            if (blockedFirstWrite.compareAndSet(false, true)) {
                firstAuditWriteStarted.countDown();
                assertTrue(releaseFirstAuditWrite.await(2, TimeUnit.SECONDS));
            }
            runAuditEvents.add(entity);
            return entity;
        });
        CapturingEmitter emitter = new CapturingEmitter();

        CompletableFuture<Void> streamFuture = CompletableFuture.runAsync(() -> {
            try {
                service.runChatStream(1L, conversation(107L), "客户应收情况", List.of(), "run-slow-audit", emitter);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });

        assertTrue(firstAuditWriteStarted.await(1, TimeUnit.SECONDS));
        try {
            assertTrue(
                waitUntilPayloadContains(emitter, "\"event_type\":\"run_completed\"", 1, TimeUnit.SECONDS),
                "SSE payloads should keep flowing while the first audit write is blocked: " + emitter.snapshotPayloads()
            );
            assertFalse(streamFuture.isDone(), "run should wait for audit drain only at finishRunAudit");
        } finally {
            releaseFirstAuditWrite.countDown();
        }
        streamFuture.get(2, TimeUnit.SECONDS);

        AgentRunAuditEntity audit = runAudits.get("run-slow-audit");
        assertNotNull(audit);
        assertEquals("completed", audit.getStatus());
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> "run-slow-audit".equals(event.getRunId()))
            .toList();
        assertEquals(audit.getEventCount(), events.size());
        assertTrue(events.stream().anyMatch(event -> "run_completed".equals(event.getEventType())));
        assertSequentialAuditEvents("run-slow-audit", events);
    }

    @Test
    void failedAuditEventWriteDoesNotFailStreamOrPoisonLaterEvents() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-audit-fail",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        AtomicBoolean failFirstWrite = new AtomicBoolean(true);
        when(agentRunAuditEventRepository.save(any(AgentRunAuditEventEntity.class))).thenAnswer(invocation -> {
            AgentRunAuditEventEntity entity = invocation.getArgument(0);
            if (failFirstWrite.getAndSet(false)) {
                throw new IllegalStateException("simulated audit write failure");
            }
            runAuditEvents.add(entity);
            return entity;
        });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(108L), "客户应收情况", List.of(), "run-audit-write-failure", emitter);

        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        AgentRunAuditEntity audit = runAudits.get("run-audit-write-failure");
        assertNotNull(audit);
        assertEquals("completed", audit.getStatus());
        assertNull(audit.getErrorCode());
        assertNull(audit.getErrorMessage());
        assertEquals(0, audit.getAuditWriteDroppedCount());
        assertEquals(1, audit.getAuditWriteFailedCount());
        assertEquals(true, audit.getAuditLossy());
        assertTrue(audit.getEmittedEventCount() > audit.getEventCount());
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> "run-audit-write-failure".equals(event.getRunId()))
            .toList();
        assertEquals(audit.getEventCount(), events.size());
        assertTrue(events.stream().anyMatch(event -> "run_completed".equals(event.getEventType())));
        assertOrderedPersistedAuditEvents("run-audit-write-failure", events);
    }

    @Test
    void rejectedAuditWriteRecordsDropNoticeWithoutFailingStream() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-audit-rejected",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        replaceAuditWriteExecutor(new RejectingThreadPoolExecutor());
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(109L), "客户应收情况", List.of(), "run-audit-rejected", emitter);

        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_completed\"")));
        AgentRunAuditEntity audit = runAudits.get("run-audit-rejected");
        assertNotNull(audit);
        assertEquals("completed", audit.getStatus());
        assertNull(audit.getErrorCode());
        assertNull(audit.getErrorMessage());
        assertEquals(0, audit.getEventCount());
        assertTrue(audit.getEmittedEventCount() > audit.getEventCount());
        assertEquals(0, audit.getAuditWriteFailedCount());
        assertTrue(audit.getAuditWriteDroppedCount() > 0);
        assertEquals(true, audit.getAuditLossy());
        assertEquals(0, runAuditEvents.stream().filter(event -> "run-audit-rejected".equals(event.getRunId())).count());

        V2AgentDtos.AgentRunAuditResponse response = service.getRunAudit("run-audit-rejected");
        assertEquals("completed", response.status());
        assertNull(response.errorCode());
        assertNull(response.errorMessage());
        assertEquals(0, response.eventCount());
        assertTrue(response.emittedEventCount() > response.eventCount());
        assertEquals(0, response.auditWriteFailedCount());
        assertTrue(response.auditWriteDroppedCount() > 0);
        assertEquals(true, response.auditLossy());
        assertTrue(response.warnings().stream().anyMatch(warning -> warning.startsWith("audit_events_dropped:")));
        assertEquals(0, response.events().size());
    }

    @Test
    void cancelRunMarksActiveStreamCancelledAndEmitsRunCancelledEvent() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-cancel",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        CountDownLatch toolQueryEntered = new CountDownLatch(1);
        CountDownLatch releaseToolQuery = new CountDownLatch(1);
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                toolQueryEntered.countDown();
                assertTrue(releaseToolQuery.await(3, TimeUnit.SECONDS), "tool query release timed out");
                return List.of(customer(1L, "客户A", 100.0));
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(107L);
        when(agentConversationRepository.findByIdAndOwnerUserId(107L, 1L)).thenReturn(Optional.of(conversation));
        AtomicReference<Throwable> streamFailure = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            try {
                service.runChatStream(1L, conversation, "客户应收情况", List.of(), "run-cancel", emitter);
            } catch (RuntimeException ex) {
                if (!String.valueOf(ex.getMessage()).startsWith("Agent run cancelled: run-cancel")) {
                    streamFailure.set(ex);
                }
            } catch (Throwable ex) {
                streamFailure.set(ex);
            }
        });
        worker.start();
        assertTrue(toolQueryEntered.await(3, TimeUnit.SECONDS), "stream did not reach tool query");

        V2AgentDtos.AgentRunCancelResponse response = service.cancelRun("run-cancel");
        releaseToolQuery.countDown();
        worker.join(3_000);

        assertFalse(worker.isAlive(), "stream worker should stop after server cancellation");
        assertNull(streamFailure.get());
        assertEquals("cancelled", response.status());
        assertEquals(true, response.cancelled());
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_cancelled\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));

        AgentRunAuditEntity audit = runAudits.get("run-cancel");
        assertNotNull(audit);
        assertEquals("cancelled", audit.getStatus());
        assertEquals("cancelled", audit.getMode());
        assertEquals("cancelled", audit.getLlmStatus());
        assertEquals("用户已停止生成", audit.getErrorMessage());
        assertNotNull(audit.getCompletedAt());
        assertTrue(runAuditEvents.stream().anyMatch(event -> "run_cancelled".equals(event.getEventType())
            && event.getPayloadJson().contains("\"reason\":\"用户已停止生成\"")));
        assertEquals("本次生成已取消，未完成的操作没有继续执行。", conversation.getLatestSummary());
        assertEquals(conversation.getLastMessageAt(), conversation.getUpdatedAt());
        assertTrue(agentMessages.stream().anyMatch(message ->
            "run-cancel".equals(message.getRunId())
                && "assistant".equals(message.getRole())
                && "cancelled".equals(message.getMessageType())));
    }

    @Test
    void cancelRunDoesNotPretendUnknownRunWasCancelled() {
        V2AgentDtos.AgentRunCancelResponse response = service.cancelRun("missing-run");

        assertEquals("missing-run", response.runId());
        assertEquals("not_found", response.status());
        assertEquals(false, response.cancelled());
        assertFalse(runAudits.containsKey("missing-run"));
        assertFalse(runAuditEvents.stream().anyMatch(event -> "run_cancelled".equals(event.getEventType())));
    }

    @Test
    void cancelRunDoesNotCancelOtherOwnerActiveRun() throws Exception {
        CapturingEmitter emitter = new CapturingEmitter();
        registerActiveRun(2L, "other-owner-run", 201L, emitter);

        V2AgentDtos.AgentRunCancelResponse response = service.cancelRun("other-owner-run");

        assertEquals("other-owner-run", response.runId());
        assertEquals("not_found", response.status());
        assertEquals(false, response.cancelled());
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"run_cancelled\"")));
        assertFalse(runAudits.containsKey("other-owner-run"));
        assertFalse(runAuditEvents.stream().anyMatch(event -> "run_cancelled".equals(event.getEventType())));
    }

    @Test
    void nonStreamingChatIncludesAuditableAgentRunContract() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0), customer(2L, "客户B", 80.0)));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(2L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(180.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前有 2 个欠款客户。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("customer_receivable_lookup"), response.planSummary());
        assertFalse(response.toolCalls().isEmpty());
        V2AgentDtos.AgentToolCallDto toolCall = response.toolCalls().get(0);
        assertEquals("customer_receivable_lookup", toolCall.toolName());
        assertEquals("completed", toolCall.status());
        assertEquals(2, toolCall.returnedCount());
        assertEquals(10, toolCall.limit());
        assertEquals(false, toolCall.isTruncated());
        assertTrue(toolCall.durationMs() != null && toolCall.durationMs() >= 0);
        assertFalse(response.evidenceRefs().isEmpty());
        assertEquals(toolCall.toolCallId(), response.evidenceRefs().get(0).toolCallId());
        assertFalse(hasBlock(response, "evidence_card"));
        assertTrue(response.blocks().isEmpty());
        assertEquals(response.blocks().size(), response.resultBlocks().size());
        assertTrue(response.performanceSummary().durationMs() >= 0);
        assertTrue(response.performanceSummary().toolDurationMs() >= 0);
        assertTrue(response.performanceSummary().modelDurationMs() >= 0);
        assertTrue(response.auditId().endsWith(":audit"));
        assertTrue(response.traceId().endsWith(":trace"));
        assertNotNull(response.observability());
        assertEquals(response.runId(), response.observability().requestId());
        assertEquals(response.runId(), response.observability().correlationId());
        assertEquals(response.auditId(), response.observability().auditId());
        assertEquals(response.traceId(), response.observability().traceId());
        assertEquals("agent-run:" + response.runId(), response.observability().logRef());
        AgentRunAuditEntity audit = runAudits.get(response.runId());
        assertNotNull(audit);
        assertEquals(1L, audit.getOwnerUserId());
        assertEquals(response.conversationId(), audit.getConversationId());
        assertEquals("completed", audit.getStatus());
        assertEquals(response.mode(), audit.getMode());
        assertEquals(response.llmStatus(), audit.getLlmStatus());
        assertEquals(response.planSource(), audit.getPlanSource());
        assertEquals(1, audit.getToolCount());
        assertEquals(response.auditId(), audit.getAuditId());
        assertEquals(response.traceId(), audit.getTraceId());
        assertNotNull(audit.getCompletedAt());
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> response.runId().equals(event.getRunId()))
            .sorted(java.util.Comparator.comparing(AgentRunAuditEventEntity::getSeq))
            .toList();
        assertTrue(events.stream().anyMatch(event -> "run_started".equals(event.getEventType())
            && event.getPayloadJson().contains("\"prompt\":\"客户应收情况\"")), events.toString());
        assertTrue(events.stream().anyMatch(event -> "tool_started".equals(event.getEventType())
            && event.getPayloadJson().contains("\"tool_call_id\":\"call-receivable\"")), events.toString());
        assertTrue(events.stream().anyMatch(event -> "tool_completed".equals(event.getEventType())
            && event.getPayloadJson().contains("\"tool_sequence\":1")), events.toString());
    }

    @Test
    void nonStreamingRuntimeFailureFinalizesAuditAndPersistsTerminalMessageBeforeRethrowing() {
        doThrow(new IllegalStateException("provider token=secret-value\nresponse parse failed"))
            .when(answerSynthesizer)
            .buildFinalAnswer(anyString(), any(AgentTypes.ResponsePayload.class), anyList(), any());

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> service.chat(new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false))
        );

        assertEquals("provider token=secret-value\nresponse parse failed", thrown.getMessage());
        AgentRunAuditEntity audit = runAudits.values().stream().findFirst().orElseThrow();
        assertEquals("failed", audit.getStatus());
        assertEquals("AGENT_RUN_FAILED", audit.getErrorCode());
        assertEquals("IllegalStateException: provider token=*** response parse failed", audit.getErrorMessage());
        assertNotNull(audit.getCompletedAt());
        assertTrue(audit.getCompletedAt() >= audit.getStartedAt());

        AgentMessageEntity terminalMessage = agentMessages.stream()
            .filter(message -> "assistant".equals(message.getRole()))
            .findFirst()
            .orElseThrow();
        assertEquals("failed", terminalMessage.getMessageType());
        assertEquals("暂时无法完成这次请求，请稍后重试。", terminalMessage.getContent());
        assertEquals(audit.getRunId(), terminalMessage.getRunId());
        assertFalse(terminalMessage.getContent().contains("secret-value"));
    }

    @Test
    void nonStreamingLlmAnswerUsesServerFactSlotsAndHidesBlocksWithoutVisualization() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询商品目录"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 12.0, 4.0, 10.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前商品数量为 3，库存总计 35。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "查询商品库存情况", false)
        );

        assertEquals("tool_query_llm", response.mode());
        assertEquals("completed", response.llmStatus());
        assertEquals("当前商品数量为 3，库存总计 35。", response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertFalse(response.answer().contains("{{"), response.answer());
        verify(longCatAnthropicClient).createJsonMessage(anyString(), contains("本轮工具真实结果 JSON"));
    }

    @Test
    void modelMustReadRealFactsBeforeItCanEnableVisualization() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查询真实商品数据"
            )));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("真实工具结果 JSON")))
            .thenReturn(Optional.of(
                "{\"tools\":[{\"name\":\"result_visualization\",\"params\":{\"mode\":\"table\",\"reason\":\"根据真实商品查询结果展示\"}}]}"
            ))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前商品数量为 3，已根据查询结果生成表格。"
            ));
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 12.0, 4.0, 10.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "查询商品库存并用表格展示", false)
        );

        assertEquals("model_json_plan_react", response.planSource());
        assertEquals("product_catalog_lookup", response.toolCalls().get(0).toolName());
        assertEquals("result_visualization", response.toolCalls().get(1).toolName());
        assertTrue(response.blocks().stream().anyMatch(block -> "table".equals(block.blockType())), response.blocks().toString());
        assertTrue(response.answer().contains("当前商品数量为 3"), response.answer());
        verify(longCatAnthropicClient, org.mockito.Mockito.atLeastOnce())
            .createJsonMessage(anyString(), contains("真实工具结果 JSON"));
    }

    @Test
    void nativeSecondPhaseVisualizationCallFiltersBlocksToRequestedMode() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "先读取真实商品数据"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-visualization",
                    "result_visualization",
                    objectMapper.createObjectNode()
                        .put("mode", "table")
                        .put("reason", "模型根据真实结果决定使用表格")
                )),
                "根据真实商品结果调用表格展示工具"
            )));
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 12.0, 4.0, 10.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前商品数量为 3，已根据查询结果调用表格展示。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "查询商品库存并用表格展示", false)
        );

        assertEquals("native_tool_use_react", response.planSource());
        assertEquals(2, response.toolCalls().size());
        assertEquals("result_visualization", response.toolCalls().get(1).toolName());
        assertTrue(response.blocks().stream().anyMatch(block -> "table".equals(block.blockType())), response.blocks().toString());
        assertEquals(1, response.blocks().stream().filter(block -> "table".equals(block.blockType())).count());
        assertFalse(response.blocks().stream().anyMatch(block -> "kpi_grid".equals(block.blockType())), response.blocks().toString());
        verify(longCatAnthropicClient, org.mockito.Mockito.times(2))
            .createMessageWithTools(anyString(), anyString(), any());
        verify(longCatAnthropicClient, org.mockito.Mockito.never())
            .createMessageWithTools(anyString(), anyString(), any(), anyString());
    }

    @Test
    void nonStreamingLlmAnswerRejectsUntrustedNumericDataAndUsesRealSummary() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-fake",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询商品目录"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 12.0, 4.0, 10.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("当前有 999 个商品。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "查询商品库存情况", false)
        );

        assertEquals("llm_answer_unavailable", response.mode());
        assertEquals("model_empty_or_ungrounded", response.llmStatus());
        assertFalse(response.answer().contains("999"), response.answer());
    }

    @Test
    void nativeToolSchemasUseClosedObjectSchemasForToolsWithoutCustomSchema() {
        Map<String, Object> schema = toolPlanner.buildNativeToolDefinitions().stream()
            .filter(definition -> "product_catalog_lookup".equals(definition.name()))
            .findFirst()
            .orElseThrow()
            .input_schema();

        assertEquals("object", schema.get("type"));
        assertEquals(false, schema.get("additionalProperties"));
        assertTrue(schema.containsKey("properties"));
        assertEquals(List.of("keyword", "status", "category_id", "unit_id"), schema.get("required"));
        Map<String, Object> productProperties = (Map<String, Object>) schema.get("properties");
        assertEquals(List.of("string", "null"), ((Map<String, Object>) productProperties.get("keyword")).get("type"));
        assertEquals(List.of("integer", "null"), ((Map<String, Object>) productProperties.get("status")).get("type"));

        Map<String, Object> purchaseSchema = toolPlanner.buildNativeToolDefinitions().stream()
            .filter(definition -> "purchase_tracking_lookup".equals(definition.name()))
            .findFirst()
            .orElseThrow()
            .input_schema();
        assertEquals(List.of("keyword", "order_id"), purchaseSchema.get("required"));
        Map<String, Object> purchaseProperties = (Map<String, Object>) purchaseSchema.get("properties");
        assertEquals(List.of("string", "null"), ((Map<String, Object>) purchaseProperties.get("keyword")).get("type"));
        assertEquals(List.of("integer", "null"), ((Map<String, Object>) purchaseProperties.get("order_id")).get("type"));
    }

    @Test
    void createOnlyToolsExposeRequiredInputContractsToNativeFunctionCalling() {
        Map<String, List<String>> requiredByTool = Map.of(
            "create_customer", List.of("name"),
            "create_supplier", List.of("name"),
            "create_product", List.of("name", "code"),
            "create_sale_order", List.of("customer_name", "items"),
            "create_purchase_order", List.of("supplier_name", "items"),
            "create_pay_order", List.of("supplier_name", "amount"),
            "create_finance_record", List.of("type", "amount")
        );

        for (Map.Entry<String, List<String>> entry : requiredByTool.entrySet()) {
            Map<String, Object> schema = toolPlanner.buildNativeToolDefinitions().stream()
                .filter(definition -> entry.getKey().equals(definition.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing tool: " + entry.getKey()))
                .input_schema();

            assertEquals("object", schema.get("type"), entry.getKey());
            assertEquals(false, schema.get("additionalProperties"), entry.getKey());
            assertTrue(schema.get("properties") instanceof Map, entry.getKey());
            List<String> required = (List<String>) schema.get("required");
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertTrue(required.containsAll(entry.getValue()), entry.getKey());
            assertEquals(properties.keySet(), new java.util.HashSet<>(required), entry.getKey());
        }
    }

    @Test
    void nonStreamingLlmAnswerRendersOnlyServerFactSlotsWithoutVisualizationBlocks() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                null
            )), Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 35.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("""
                当前账号共有 3 个商品，库存总计 35。
                """));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品库存情况", false)
        );

        assertEquals("tool_query_llm", response.mode());
        assertEquals("completed", response.llmStatus());
        assertTrue(response.answer().contains("3 个商品"), response.answer());
        assertTrue(response.answer().contains("35"), response.answer());
        assertFalse(response.answer().contains("{{"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.resultBlocks().isEmpty(), response.resultBlocks().toString());
    }

    @Test
    void nonStreamingLlmAnswerAcceptsDirectNaturalLanguageWhenEveryNumberIsReal() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products-direct",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                null
            )), Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 35.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("当前账号共有3个商品，库存总计35。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品库存情况", false)
        );

        assertEquals("tool_query_llm", response.mode());
        assertEquals("completed", response.llmStatus());
        assertEquals("当前账号共有3个商品，库存总计35。", response.answer());
    }

    @Test
    void formalAnswerUsesModelNaturalLanguageWithoutServerTemplateRendering() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-natural-answer",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询商品目录"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 35.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("库存中有 3 个商品，共 35 件。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品库存情况", false)
        );

        assertEquals("库存中有 3 个商品，共 35 件。", response.answer());
        assertFalse(response.answer().contains("fact_"), response.answer());
        verify(longCatAnthropicClient).createJsonMessage(
            anyString(),
            argThat(prompt -> prompt.contains("本轮工具真实结果 JSON") && !prompt.contains("fact_slot_id"))
        );
    }

    @Test
    void formalAnswerRejectsJsonEnvelopeInsteadOfRenderingTemplateText() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-json-answer",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询商品目录"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 35.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("{\"answer\":\"当前有 {{fact_1_product_catalog_lookup_product_count}} 个商品。\"}"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品库存情况", false)
        );

        assertEquals("llm_answer_unavailable", response.mode());
        assertTrue(response.answer().isBlank(), response.answer());
        assertFalse(response.answer().contains("3 个商品"), response.answer());
    }

    @Test
    void nonStreamingLlmAnswerWithRawInventedValueFallsBackToRealToolSummary() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                null
            )), Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 35.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(3L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(35.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("当前共有 999 个商品。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品库存情况", false)
        );

        assertEquals("llm_answer_unavailable", response.mode());
        assertEquals("model_empty_or_ungrounded", response.llmStatus());
        assertFalse(response.answer().contains("999"), response.answer());
    }

    @Test
    void nonStreamingChatExplainsLimitedQueryBoundaryAndFieldLevelEvidence() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        List<com.zhihuiji.backend.domain.entity.CustomerEntity> customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            customers.add(customer((long) i, "客户" + i, 100.0 + i));
        }
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(customers);
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(10L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(1055.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "查询边界：客户应收查询仅返回前 10 条，不能视为全量结论。当前有 10 个欠款客户。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertTrue(response.answer().contains("查询边界："), response.answer());
        assertTrue(response.answer().contains("客户应收查询仅返回前 10 条"), response.answer());
        assertTrue(response.answer().contains("不能视为全量结论"), response.answer());
        assertEquals(true, response.toolCalls().get(0).isTruncated());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("customer_count") && "10个".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_receivable") && ref.value().contains("¥")
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("top10_receivable_total") && ref.value().contains("¥")
        ));
        assertFalse(hasBlock(response, "evidence_card"));
    }

    @Test
    void productCatalogUsesRepositoryAggregatesForSummaryInsteadOfPageSample() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-products",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询商品目录"
            )))
            .thenReturn(Optional.empty());
        when(productRepository.findByOwnerUserIdAndKeywordAndFiltersOrderByUpdatedAtDesc(eq(1L), any(), any(), any(), any(), any()))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 2.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(25L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(300.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(4L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "商品总数 25 个，库存总计 300，低库存商品 4 个。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品目录情况，用指标卡展示", false)
        );

        assertTrue(response.answer().contains("商品总数 25 个"), response.answer());
        assertTrue(response.answer().contains("库存总计 300"), response.answer());
        assertTrue(response.answer().contains("低库存商品 4 个"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("product_count") && "25个".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("stock_total") && "300".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("low_stock_count") && "4个".equals(ref.value())
        ));
        assertEquals(1, response.toolCalls().get(0).returnedCount());
        assertEquals(10, response.toolCalls().get(0).limit());
        verify(productRepository, never()).findAllByOwnerUserId(1L);
    }

    @Test
    void receivableAndPayableUseRepositoryAggregatesForTotalsInsteadOfTopPageSample() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable-payable",
                    "receivable_payable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询应收应付对账"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(12L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(900.0);
        when(supplierRepository.findPayablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(supplier(1L, "供应商A", 80.0)));
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(8L);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(700.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "欠款客户总数 12 个，应收总额 ¥900.00，应付供应商总数 8 个，应付总额 ¥700.00。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收和供应商应付情况，用图表展示", false)
        );

        assertTrue(response.answer().contains("欠款客户总数 12 个"), response.answer());
        assertTrue(response.answer().contains("应收总额 ¥900.00"), response.answer());
        assertTrue(response.answer().contains("应付供应商总数 8 个"), response.answer());
        assertTrue(response.answer().contains("应付总额 ¥700.00"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_receivable") && "¥900.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("net_exposure") && "¥200.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_payable") && "¥700.00".equals(ref.value())
        ));
    }

    @Test
    void nonStreamingRuleSummaryDistinguishesNotConfiguredModelStatus() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("not_configured");

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertEquals("llm_required", response.mode());
        assertEquals("not_configured", response.llmStatus());
        assertEquals(0L, response.performanceSummary().modelDurationMs());
        assertTrue(response.answer().isEmpty(), response.answer());
    }

    @Test
    void nonStreamingChatPlansEnglishBusinessKeywordsForDeviceQa() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(
                    new LongCatAnthropicClient.ToolUseBlock("call-1", "supplier_payable_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-2", "purchase_order_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-3", "finance_record_lookup", objectMapper.createObjectNode()),
                    new LongCatAnthropicClient.ToolUseBlock("call-4", "sales_overview_lookup", objectMapper.createObjectNode())
                ),
                "查询销售、采购、财务和经营概览"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        when(supplierRepository.findPayablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(0L);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(0.0);
        when(purchaseOrderRepository.search(1L, null, null, PageRequest.of(0, 10))).thenReturn(List.of());
        when(financeRecordRepository.search(1L, null, null, null, null, PageRequest.of(0, 10))).thenReturn(List.of());
        when(saleOrderRepository.sumTotalAmountBetween(any(), any(), any())).thenReturn(0.0);
        when(saleOrderRepository.sumPaidAmountBetween(any(), any(), any())).thenReturn(0.0);
        when(saleOrderRepository.countNonCancelledBetween(any(), any(), any())).thenReturn(0L);
        when(productRepository.findLowStockProducts(1L, PageRequest.of(0, 5))).thenReturn(List.of());
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(0.0);
        when(saleOrderRepository.customerSales(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(saleOrderRepository.salesTrendBuckets(any(), any(), any(), any(), any())).thenReturn(List.<Object[]>of(
            new Object[] {0L, 120.0, 2L, 80.0}
        ));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "近期销售额 0，回款 0。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "recent sales purchase finance business overview chart", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("supplier_payable_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("purchase_order_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("finance_record_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("sales_overview_lookup"), response.planSummary());
        assertEquals(4, response.toolCalls().size());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "supplier_payable_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "purchase_order_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "finance_record_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "sales_overview_lookup".equals(tool.toolName())));
        assertFalse(response.toolCalls().stream().anyMatch(tool -> "result_visualization".equals(tool.toolName())));
        assertTrue(response.resultBlocks().isEmpty(), response.resultBlocks().toString());
        verify(saleOrderRepository).salesTrendBuckets(any(), any(), any(), any(), any());
        verify(saleOrderRepository, never()).findByOwnerUserIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    void receivablePayableLookupProvidesCombinedReconciliationSummary() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable-payable",
                    "receivable_payable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询应收应付对账"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "张三商贸", 300.0), customer(2L, "李四超市", 120.0)));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(5L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(1500.0);
        when(supplierRepository.findPayablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(supplier(1L, "晨光供货", 260.0), supplier(2L, "万联批发", 80.0)));
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(4L);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(900.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "应收总额 ¥1500.00，应付总额 ¥900.00，净敞口 ¥600.00。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "请帮我做应收应付对账，用图表展示", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("receivable_payable_lookup"), response.planSummary());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "receivable_payable_lookup".equals(tool.toolName())));
        assertTrue(response.answer().contains("应收总额 ¥1500.00"), response.answer());
        assertTrue(response.answer().contains("应付总额 ¥900.00"), response.answer());
        assertTrue(response.answer().contains("净敞口 ¥600.00"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_receivable") && "¥1500.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_payable") && "¥900.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("net_exposure") && "¥600.00".equals(ref.value())
        ));
    }

    @Test
    void customerProfileLookupProvidesCustomerInsightAndCollectionSuggestion() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-profile",
                    "customer_profile_lookup",
                    objectMapper.createObjectNode().put("keyword", "张三商贸")
                )),
                "查询客户画像"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        com.zhihuiji.backend.domain.entity.CustomerEntity matchedCustomer = customer(1L, "张三商贸", 600.0);
        matchedCustomer.setPhone("13812345678");
        matchedCustomer.setLevel(2);
        when(customerRepository.search(1L, "张三商贸", null, null)).thenReturn(List.of(matchedCustomer));
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(matchedCustomer));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(1L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(600.0);

        SaleOrderEntity order1 = saleOrder(11L, 1L, "SO-001", "张三商贸", 500.0, 300.0, 1_720_000_000_000L);
        SaleOrderEntity order2 = saleOrder(12L, 1L, "SO-002", "张三商贸", 200.0, 200.0, 1_721_000_000_000L);
        when(saleOrderRepository.search(1L, "张三商贸", null, null, null, null, null, null, null,
            PageRequest.of(0, 50)))
            .thenReturn(List.of(order2, order1));

        PaymentEntity payment1 = payment(101L, 12L, 200.0, 1, 1_721_000_100_000L);
        PaymentEntity payment2 = payment(102L, 11L, 300.0, 1, 1_720_000_100_000L);
        when(paymentRepository.findByOwnerUserIdAndOrderIdIn(eq(1L), any())).thenReturn(List.of(payment1, payment2));

        SalesReturnEntity salesReturn = salesReturn(201L, 11L, "SR-001", "张三商贸", 50.0, 20.0, 1_721_000_200_000L);
        when(salesReturnRepository.findByOwnerUserIdAndOriginalOrderIdInOrderByCreatedAtDesc(eq(1L), any()))
            .thenReturn(List.of(salesReturn));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "客户张三商贸，累计销售额 ¥700.00，当前欠款 ¥600.00，付款习惯偏微信。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "看下张三商贸的客户画像和催收建议，用指标卡展示", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("customer_profile_lookup"), response.planSummary());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "customer_profile_lookup".equals(tool.toolName())));
        assertTrue(response.answer().contains("张三商贸"), response.answer());
        assertTrue(response.answer().contains("累计销售额 ¥700.00"), response.answer());
        assertTrue(response.answer().contains("当前欠款 ¥600.00"), response.answer());
        assertTrue(response.answer().contains("付款习惯偏微信"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("customer_name") && "张三商贸".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("balance") && "¥600.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("payment_habit") && "微信".equals(ref.value())
        ));
    }

    @Test
    void keywordFallbackUsesRecentConversationContextForCustomerFollowUpQuestion() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode().put("keyword", "张三商贸")
                )),
                "根据历史对话查询张三商贸的欠款"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        AgentConversationEntity conversation = conversation(301L);
        conversation.setLatestSummary("刚才查询了客户张三商贸的客户画像，当前欠款 ¥600.00。");
        when(agentConversationRepository.findByIdAndOwnerUserId(301L, 1L)).thenReturn(Optional.of(conversation));
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(1L, 301L, PageRequest.of(0, 10)))
            .thenReturn(List.of(
                message(702L, 301L, "assistant", "刚才查询了客户张三商贸的客户画像，当前欠款 ¥600.00。", 20L),
                message(701L, 301L, "user", "帮我看下张三商贸的客户画像", 10L)
            ));

        com.zhihuiji.backend.domain.entity.CustomerEntity matchedCustomer = customer(1L, "张三商贸", 600.0);
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(matchedCustomer));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(1L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(600.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "当前欠款 ¥600.00。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(301L, "刚才那个客户的欠款呢", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertFalse(response.toolCalls().isEmpty());
        V2AgentDtos.AgentToolCallDto toolCall = response.toolCalls().get(0);
        assertEquals("customer_receivable_lookup", toolCall.toolName());
        assertTrue(toolCall.inputSummary().contains("张三商贸"), toolCall.inputSummary());
        assertTrue(response.answer().contains("¥600.00"), response.answer());
    }

    @Test
    void inventoryPanoramaLookupProvidesInventoryHealthInsight() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-panorama",
                    "inventory_panorama_lookup",
                    objectMapper.createObjectNode().put("keyword", "矿泉水")
                )),
                "查询库存全景"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        ProductEntity product = product(1L, "P001", "矿泉水", 12.0, 20.0, 3.5);
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(1L, PageRequest.of(0, 10)))
            .thenReturn(List.of(product));
        when(inventoryMonthlyStatsRepository.findByOwnerUserIdAndProductIdAndYearAndMonth(any(), any(), any(), any()))
            .thenReturn(Optional.of(inventoryMonthlyStats(1L, 1L, "P001", "矿泉水", 20.0, 12.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "商品矿泉水，当前库存 12，安全库存 20，近30天销量 12，建议补货量 28。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "看下矿泉水的库存全景和库存周转，用表格展示", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("inventory_panorama_lookup"), response.planSummary());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "inventory_panorama_lookup".equals(tool.toolName())));
        assertTrue(response.answer().contains("矿泉水"), response.answer());
        assertTrue(response.answer().contains("当前库存 12"), response.answer());
        assertTrue(response.answer().contains("安全库存 20"), response.answer());
        assertTrue(response.answer().contains("近30天销量 12"), response.answer());
        assertTrue(response.answer().contains("建议补货量 28"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("商品名称") && "矿泉水".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("建议补货量") && "28".equals(ref.value())
        ));
    }

    @Test
    void purchaseTrackingLookupProvidesReceiptAndReturnChainInsight() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-tracking",
                    "purchase_tracking_lookup",
                    objectMapper.createObjectNode().put("keyword", "可口供应链")
                )),
                "查询采购跟踪"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        PurchaseOrderEntity order = purchaseOrder(21L, "PO-001", "可口供应链", 1200.0, 800.0, 900.0, 1_721_000_000_000L);
        when(purchaseOrderRepository.search(1L, "可口供应链", null)).thenReturn(List.of(order));

        PurchaseReceiptEntity receipt1 = purchaseReceipt(31L, 21L, "PR-001", "可口供应链", 600.0, 1, 1_721_000_100_000L);
        PurchaseReceiptEntity receipt2 = purchaseReceipt(32L, 21L, "PR-002", "可口供应链", 300.0, 1, 1_721_000_200_000L);
        when(purchaseReceiptRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(1L, 21L))
            .thenReturn(List.of(receipt2, receipt1));

        PurchaseReturnEntity purchaseReturn = purchaseReturn(41L, 21L, "PTR-001", "可口供应链", 120.0, 80.0, 2, 1_721_000_300_000L);
        when(purchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc(1L, 21L))
            .thenReturn(List.of(purchaseReturn));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "采购单 PO-001（可口供应链）采购总额 ¥1200.00，已到货 ¥900.00，待付款 ¥400.00，关联入库 2 条，退货 1 条。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "看下可口供应链的采购跟踪和入库退货，用表格展示", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("purchase_tracking_lookup"), response.planSummary());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "purchase_tracking_lookup".equals(tool.toolName())));
        assertTrue(response.answer().contains("PO-001"), response.answer());
        assertTrue(response.answer().contains("可口供应链"), response.answer());
        assertTrue(response.answer().contains("采购总额 ¥1200.00"), response.answer());
        assertTrue(response.answer().contains("已到货 ¥900.00"), response.answer());
        assertTrue(response.answer().contains("待付款 ¥400.00"), response.answer());
        assertTrue(response.answer().contains("关联入库 2 条"), response.answer());
        assertTrue(response.answer().contains("退货 1 条"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("采购单号") && "PO-001".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("入库单数") && "2条".equals(ref.value())
        ));
    }

    @Test
    void accountHealthLookupProvidesAccountBalanceFlowAndRiskInsight() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-account",
                    "account_health_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询账户健康"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString())).thenReturn(Optional.empty());
        when(accountRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc(1L)).thenReturn(List.of(
            account(11L, "WX", "微信账户", 3, 5200.0, false, 1, "日常收款"),
            account(12L, "BANK", "招商银行", 1, 1800.0, true, 1, "默认结算"),
            account(13L, "CASH", "备用金", 0, 0.0, false, 0, "线下零用")
        ));
        long now = System.currentTimeMillis();
        when(accountTransferRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
            accountTransfer(21L, "AT-002", 11L, 12L, 300.0, 1.0, 1, "周转", now - 3_600_000L),
            accountTransfer(22L, "AT-001", 12L, 11L, 500.0, 2.0, 1, "调拨", now - 7_200_000L)
        ));
        when(cashChangeRecordRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
            cashChange(31L, "sale_order", 901L, 100.0, 120.0, 20.0, 11L, "找零", now - 5_400_000L),
            cashChange(32L, "sale_order", 902L, 80.0, 80.0, 0.0, 12L, "整单收款", now - 2_700_000L)
        ));
        when(financeRecordRepository.cashflowSummary(any(), any(), any(), any(), any()))
            .thenReturn(new Object[]{9800.0, 7600.0, 6L});
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "账户总余额 ¥7000.00，收支比 1.29，低余额账户 1 个，近期转账 2 条。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "看下账户健康和收支比，用表格展示", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("account_health_lookup"), response.planSummary());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "account_health_lookup".equals(tool.toolName())));
        assertTrue(response.answer().contains("账户总余额 ¥7000.00"), response.answer());
        assertTrue(response.answer().contains("收支比 1.29"), response.answer());
        assertTrue(response.answer().contains("低余额账户 1 个"), response.answer());
        assertTrue(response.answer().contains("近期转账 2 条"), response.answer());
        assertTrue(response.blocks().isEmpty(), response.blocks().toString());
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("账户总余额") && "¥7000.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("低余额账户") && "1个".equals(ref.value())
        ));
    }

    @Test
    void chatCreateSaleOrderMessageFallsBackToDraftGenerationWithoutLlm() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call_1",
                    "create_sale_order",
                    objectMapper.createObjectNode()
                        .put("customer_id", 101L)
                        .put("customer_name", "张三")
                        .put("remark", "月底送货")
                        .set("items", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                .put("product_id", 202L)
                                .put("product_name", "测试商品")
                                .put("quantity", 1)
                                .put("price", 12.3)))
                )),
                "直接生成销售单草稿"
            )));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of(
                "已生成销售单草稿，客户为张三，请确认后执行。"
            ));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "帮张三开一张销售单 备注月底送货", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("create_sale_order"), response.planSummary());
        assertEquals(1, response.toolCalls().size());
        assertEquals("create_sale_order", response.toolCalls().get(0).toolName());
        assertTrue(response.answer().contains("销售单草稿"), response.answer());
        assertTrue(response.blocks().stream().anyMatch(block ->
            ("draft".equals(block.blockType()) || "draft_card".equals(block.blockType()))
                && "create_sale_order".equals(block.data().path("draft_type").asText())
                && "张三".equals(block.data().path("customer_name").asText())
        ));
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
    }

    @Test
    void chatUsesNativeToolUsePlanForCreateCustomerDraftWhenAvailable() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call_1",
                    "create_customer",
                    objectMapper.createObjectNode()
                        .put("name", "李四")
                        .put("phone", "13812345678")
                        .put("remark", "原生工具调用")
                )),
                "直接生成客户草稿"
            )));
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("生成草稿 新建客户：李四，请确认后执行创建。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "帮我新建客户李四 电话13812345678", false)
        );

        assertEquals("native_tool_use", response.planSource());
        assertTrue(response.planSummary().contains("create_customer"), response.planSummary());
        assertEquals(1, response.toolCalls().size());
        assertEquals("create_customer", response.toolCalls().get(0).toolName());
        assertTrue(response.answer().contains("生成草稿 新建客户：李四"), response.answer());
        assertTrue(response.blocks().stream().anyMatch(block ->
            ("draft".equals(block.blockType()) || "draft_card".equals(block.blockType()))
                && "create_customer".equals(block.data().path("draft_type").asText())
                && block.data().path("title").asText().contains("李四")
        ));
        ArgumentCaptor<AgentDraftEntity> draftCaptor = ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(draftCaptor.capture());
        assertEquals(101L, draftCaptor.getValue().getConversationId());
        verify(longCatAnthropicClient).createMessageWithTools(anyString(), anyString(), any());
    }

    @Test
    void finalAnswerAllowsOrderedListMarkersButKeepsBusinessNumberGuard() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        AgentTypes.ToolExecutionResult result = new AgentTypes.ToolExecutionResult(
            "product_catalog_lookup",
            "商品总数 3 个，返回 1 个",
            objectMapper.createObjectNode().put("product_count", 3),
            false
        );
        AgentTypes.ResponsePayload payload = new AgentTypes.ResponsePayload(List.of(), List.of(result));

        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("1. 当前有 3 个商品。\n2. 目录查询已完成。"));

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "查看商品目录",
            payload,
            List.of(),
            null
        );

        assertEquals("1. 当前有 3 个商品。\n2. 目录查询已完成。", answer.answer());

        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("当前有 999 个商品。"));
        AgentTypes.FinalAnswer ungrounded = answerSynthesizer.buildFinalAnswer(
            "查看商品目录",
            payload,
            List.of(),
            null
        );
        assertTrue(ungrounded.answer().isBlank());
        assertEquals("llm_answer_unavailable", ungrounded.mode());
    }

    @Test
    void finalAnswerRetriesWhenFirstModelResponseIsEmpty() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        AgentTypes.ToolExecutionResult result = new AgentTypes.ToolExecutionResult(
            "product_catalog_lookup",
            "商品总数 3 个，返回 1 个",
            objectMapper.createObjectNode().put("product_count", 3),
            false
        );
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of("当前有 3 个商品。"));

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "查看商品目录",
            new AgentTypes.ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("当前有 3 个商品。", answer.answer());
        assertEquals("validated_retry", answer.llmStatus());
        verify(longCatAnthropicClient, times(2))
            .createJsonMessage(anyString(), contains("本轮工具真实结果 JSON"));
    }

    @Test
    void finalAnswerCannotClaimDraftWhenNoCreateToolCompleted() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        AgentTypes.ToolExecutionResult result = new AgentTypes.ToolExecutionResult(
            "product_catalog_lookup",
            "商品总数 3 个，返回 3 个",
            objectMapper.createObjectNode().put("product_count", 3),
            false
        );
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("好的，已为您选择第一个商品生成盘点草稿，请确认。"));

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "选一个商品做盘点草稿",
            new AgentTypes.ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertTrue(answer.answer().isBlank());
        assertEquals("llm_answer_unavailable", answer.mode());
    }

    @Test
    void finalAnswerMayClaimDraftWhenDraftFactContainsRealDraftId() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        AgentTypes.ToolExecutionResult result = new AgentTypes.ToolExecutionResult(
            "create_inventory_count_draft",
            "生成盘点草稿 #88",
            objectMapper.createObjectNode().put("draft_id", 88L).put("product_id", 7L),
            false
        );
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("好的，已生成盘点草稿 #88，请确认。"));

        AgentTypes.FinalAnswer answer = answerSynthesizer.buildFinalAnswer(
            "选一个商品做盘点草稿",
            new AgentTypes.ResponsePayload(List.of(), List.of(result)),
            List.of(),
            null
        );

        assertEquals("好的，已生成盘点草稿 #88，请确认。", answer.answer());
    }

    @Test
    void getRunAuditReturnsOwnerScopedSummaryAndEvents() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-audit-read",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findReceivablesByOwnerUserIdAndFilters(eq(1L), eq(0.0), any(), any(), any(), any()))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
                String response = "客户应收查询已完成。";
                onDelta.accept(response);
                return Optional.of(response);
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(106L), "客户应收情况", List.of(), "run-audit-read", emitter);

        V2AgentDtos.AgentRunAuditResponse response = service.getRunAudit("run-audit-read");

        assertEquals("run-audit-read", response.runId());
        assertEquals(1L, response.ownerUserId());
        assertEquals(106L, response.conversationId());
        assertEquals("completed", response.status());
        assertEquals("tool_query_llm_streamed", response.mode());
        assertEquals("completed", response.llmStatus());
        assertEquals("native_tool_use", response.planSource());
        assertEquals("run-audit-read:audit", response.auditId());
        assertEquals("run-audit-read:trace", response.traceId());
        assertTrue(response.eventCount() > 0);
        assertEquals(response.eventCount(), response.emittedEventCount());
        assertEquals(0, response.auditWriteDroppedCount());
        assertEquals(0, response.auditWriteFailedCount());
        assertEquals(false, response.auditLossy());
        assertTrue(response.warnings().isEmpty());
        assertEquals(response.eventCount(), response.events().size());
        assertEquals("run_started", response.events().get(0).eventType());
        assertEquals("run-audit-read", response.events().get(0).payload().path("run_id").asText());
        assertTrue(response.events().stream().anyMatch(event ->
            "tool_completed".equals(event.eventType())
                && "customer_receivable_lookup".equals(event.payload().path("tool_name").asText())
        ));
    }

    @Test
    void chatAfterCompactionUsesOnlyBoundaryAfterMessagesForCurrentRequest() throws Exception {
        // LLM 未配置 -> 走确定性压缩降级路径，不依赖 Provider。
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        AgentConversationEntity conversation = conversation(401L);
        when(agentConversationRepository.findByIdAndOwnerUserId(401L, 1L)).thenReturn(Optional.of(conversation));
        // 保存检查点时返回带 id 的实体，使 CompactionResult.occurred() = true。
        when(agentContextCheckpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> {
                AgentContextCheckpointEntity entity = invocation.getArgument(0);
                setId(entity, 999L);
                return entity;
            });
        // 历史 7 条消息：两个完整轮次 + 当前 user 消息（id=7）。
        List<AgentMessageEntity> descending = List.of(
            message(7L, 401L, "user", "当前问题", 7L),
            message(6L, 401L, "tool", null, 6L),
            message(5L, 401L, "assistant", "第二轮回答", 5L),
            message(4L, 401L, "user", "第二轮问题", 4L),
            message(3L, 401L, "tool", null, 3L),
            message(2L, 401L, "assistant", "第一轮回答", 2L),
            message(1L, 401L, "user", "第一轮问题", 1L)
        );
        for (AgentMessageEntity toolMessage : List.of(descending.get(1), descending.get(4))) {
            toolMessage.setStructuredDataJson("{\"tool_name\":\"product_catalog_lookup\",\"tool_call_id\":\"call-x\"}");
        }
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            1L, 401L, PageRequest.of(0, 24)))
            .thenReturn(descending);

        // 超长当前问题强制压缩预算超限。
        String longQuestion = "请帮我仔细查询并核对这家门店的历史经营数据".repeat(2000);
        ArgumentCaptor<List<AgentMessageEntity>> historyCaptor = ArgumentCaptor.forClass(List.class);
        service.chat(new V2AgentDtos.AgentChatRequest(401L, longQuestion, false));

        // 压缩发生后，当前请求只应使用检查点边界（id=6）之后的原始消息（id=7），
        // 而不是压缩前的完整消息列表。
        verify(answerSynthesizer).buildFinalAnswer(
            anyString(), any(), historyCaptor.capture(), anyString()
        );
        List<AgentMessageEntity> history = historyCaptor.getValue();
        assertEquals(1, history.size(), "压缩后历史只保留边界之后的原始消息");
        assertEquals(7L, history.get(0).getId());
    }

    @Test
    void streamCompactionEmitsContextCompactedAndKeepsBoundaryAfterMessages() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(agentContextCheckpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> {
                AgentContextCheckpointEntity entity = invocation.getArgument(0);
                setId(entity, 888L);
                return entity;
            });
        List<AgentMessageEntity> descending = List.of(
            message(7L, 402L, "user", "当前问题", 7L),
            message(6L, 402L, "tool", null, 6L),
            message(5L, 402L, "assistant", "第二轮回答", 5L),
            message(4L, 402L, "user", "第二轮问题", 4L),
            message(3L, 402L, "tool", null, 3L),
            message(2L, 402L, "assistant", "第一轮回答", 2L),
            message(1L, 402L, "user", "第一轮问题", 1L)
        );
        for (AgentMessageEntity toolMessage : List.of(descending.get(1), descending.get(4))) {
            toolMessage.setStructuredDataJson("{\"tool_name\":\"product_catalog_lookup\",\"tool_call_id\":\"call-y\"}");
        }
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            1L, 402L, PageRequest.of(0, 24)))
            .thenReturn(descending);

        String longQuestion = "请帮我统计所有客户这个季度的应收与付款计划".repeat(2000);
        CapturingEmitter emitter = new CapturingEmitter();
        service.runChatStream(1L, conversation(402L), longQuestion, List.of(), "run-ctx-compact", emitter);

        // context_compacted 事件携带边界、压缩条数和摘要预览。
        assertTrue(emitter.containsPayload("context_compacted"), String.join("\n", emitter.payloads));
        String compacted = firstPayload(emitter, "context_compacted");
        assertTrue(compacted.contains("\"compacted_count\":6"), compacted);
        assertTrue(compacted.contains("\"source_boundary_message_id\":6"), compacted);
        assertTrue(compacted.contains("\"reason\":\"context_budget_threshold\""), compacted);
        // 压缩使用确定性摘要，不携带凭据类原文。
        assertFalse(compacted.contains("sk-"), compacted);
    }

    private static boolean hasBlock(V2AgentDtos.AgentChatResponse response, String blockType) {
        return response.blocks().stream().anyMatch(block -> blockType.equals(block.blockType()));
    }

    @SuppressWarnings("unchecked")
    private void registerActiveRun(Long ownerUserId, String runId, Long conversationId, SseEmitter emitter) throws Exception {
        // activeRuns 已迁移到 RunAuditService，ActiveAgentRun 为其 public static 内部类
        Object activeRun = new RunAuditService.ActiveAgentRun(ownerUserId, runId, conversationId, emitter);
        runAuditService.registerRun((RunAuditService.ActiveAgentRun) activeRun);
    }

    private void replaceAuditWriteExecutor(ThreadPoolExecutor executor) throws Exception {
        Field auditExecutorField = RunAuditService.class.getDeclaredField("auditWriteExecutor");
        auditExecutorField.setAccessible(true);
        ThreadPoolExecutor previous = (ThreadPoolExecutor) auditExecutorField.get(runAuditService);
        previous.shutdownNow();
        auditExecutorField.set(runAuditService, executor);
    }

    private static String firstPayload(CapturingEmitter emitter, String marker) {
        return emitter.payloads.stream()
            .filter(payload -> payload.contains(marker))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing payload: " + marker + "\n" + String.join("\n", emitter.payloads)));
    }

    private static int firstPayloadIndex(CapturingEmitter emitter, String marker) {
        for (int index = 0; index < emitter.payloads.size(); index++) {
            if (emitter.payloads.get(index).contains(marker)) {
                return index;
            }
        }
        throw new AssertionError("Missing payload: " + marker + "\n" + String.join("\n", emitter.payloads));
    }

    private static int firstPayloadIndexContaining(CapturingEmitter emitter, String firstMarker, String secondMarker) {
        for (int index = 0; index < emitter.payloads.size(); index++) {
            String payload = emitter.payloads.get(index);
            if (payload.contains(firstMarker) && payload.contains(secondMarker)) {
                return index;
            }
        }
        throw new AssertionError(
            "Missing payload containing: " + firstMarker + " and " + secondMarker + "\n" + String.join("\n", emitter.payloads)
        );
    }

    private static int payloadIndexContaining(CapturingEmitter emitter, String marker) {
        for (int index = 0; index < emitter.payloads.size(); index++) {
            if (emitter.payloads.get(index).contains(marker)) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> answerDeltaPayloads(CapturingEmitter emitter) {
        return emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .toList();
    }

    private static void assertSequentialAuditEvents(String runId, List<AgentRunAuditEventEntity> events) {
        List<AgentRunAuditEventEntity> sorted = events.stream()
            .sorted((left, right) -> Integer.compare(left.getSeq(), right.getSeq()))
            .toList();
        for (int index = 0; index < sorted.size(); index++) {
            int expectedSeq = index + 1;
            assertEquals(expectedSeq, sorted.get(index).getSeq());
            assertEquals(runId + ":" + expectedSeq, sorted.get(index).getEventId());
        }
    }

    private static void assertOrderedPersistedAuditEvents(String runId, List<AgentRunAuditEventEntity> events) {
        List<AgentRunAuditEventEntity> sorted = events.stream()
            .sorted((left, right) -> Integer.compare(left.getSeq(), right.getSeq()))
            .toList();
        int previousSeq = 0;
        for (AgentRunAuditEventEntity event : sorted) {
            assertTrue(event.getSeq() > previousSeq);
            assertEquals(runId + ":" + event.getSeq(), event.getEventId());
            previousSeq = event.getSeq();
        }
    }

    private static boolean waitUntilPayloadContains(CapturingEmitter emitter, String marker, long timeout, TimeUnit unit)
        throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (emitter.containsPayload(marker)) {
                return true;
            }
            Thread.sleep(10L);
        }
        return emitter.containsPayload(marker);
    }

    private static AgentConversationEntity conversation(Long id) {
        AgentConversationEntity entity = new AgentConversationEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setTitle("测试会话");
        entity.setStatus("active");
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static AgentMessageEntity message(Long id, Long conversationId, String role, String content, long createdAt) {
        AgentMessageEntity entity = new AgentMessageEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setMessageType("text");
        entity.setContent(content);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private static ProductEntity product(Long id, String code, String name, double stock, double safeStock, double salePrice) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setCategory("默认分类");
        entity.setUnit("件");
        entity.setStock(stock);
        entity.setSafeStock(safeStock);
        entity.setSalePrice(salePrice);
        entity.setPurchasePrice(salePrice * 0.7);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static SupplierEntity supplier(Long id, String name, double balance) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setPhone("13900000000");
        entity.setBalance(balance);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static com.zhihuiji.backend.domain.entity.CustomerEntity customer(Long id, String name, double balance) {
        com.zhihuiji.backend.domain.entity.CustomerEntity entity = new com.zhihuiji.backend.domain.entity.CustomerEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setPhone("13800000000");
        entity.setBalance(balance);
        entity.setLevel(1);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static SaleOrderEntity saleOrder(Long id, Long customerId, String orderNo, String customerName, double totalAmount, double paidAmount, long createdAt) {
        SaleOrderEntity entity = new SaleOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCustomerId(customerId);
        entity.setOrderNo(orderNo);
        entity.setCustomerName(customerName);
        entity.setSubtotalAmount(totalAmount);
        entity.setDiscountAmount(0.0);
        entity.setTotalAmount(totalAmount);
        entity.setPaidAmount(paidAmount);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static PaymentEntity payment(Long id, Long orderId, double amount, int method, long createdAt) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderId(orderId);
        entity.setAmount(amount);
        entity.setMethod(method);
        entity.setType(1);
        entity.setReferenceNo("REF-" + id);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private static SalesReturnEntity salesReturn(Long id, Long originalOrderId, String returnNo, String customerName, double totalAmount, double refundAmount, long createdAt) {
        SalesReturnEntity entity = new SalesReturnEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOriginalOrderId(originalOrderId);
        entity.setReturnNo(returnNo);
        entity.setCustomerName(customerName);
        entity.setTotalAmount(totalAmount);
        entity.setRefundAmount(refundAmount);
        entity.setStatus(1);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static PurchaseOrderEntity purchaseOrder(
        Long id,
        String orderNo,
        String supplierName,
        double totalAmount,
        double paidAmount,
        double receivedAmount,
        long createdAt
    ) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setSupplierId(1L);
        entity.setOrderNo(orderNo);
        entity.setSupplierName(supplierName);
        entity.setTotalAmount(totalAmount);
        entity.setPaidAmount(paidAmount);
        entity.setReceivedAmount(receivedAmount);
        entity.setSettlementMethod(1);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static PurchaseReceiptEntity purchaseReceipt(
        Long id,
        Long purchaseOrderId,
        String receiptNo,
        String supplierName,
        double totalAmount,
        int status,
        long createdAt
    ) {
        PurchaseReceiptEntity entity = new PurchaseReceiptEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setPurchaseOrderId(purchaseOrderId);
        entity.setSupplierId(1L);
        entity.setReceiptNo(receiptNo);
        entity.setSupplierName(supplierName);
        entity.setTotalAmount(totalAmount);
        entity.setStatus(status);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static PurchaseReturnEntity purchaseReturn(
        Long id,
        Long purchaseOrderId,
        String returnNo,
        String supplierName,
        double totalAmount,
        double refundAmount,
        int status,
        long createdAt
    ) {
        PurchaseReturnEntity entity = new PurchaseReturnEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setPurchaseOrderId(purchaseOrderId);
        entity.setSupplierId(1L);
        entity.setReturnNo(returnNo);
        entity.setSupplierName(supplierName);
        entity.setTotalAmount(totalAmount);
        entity.setRefundAmount(refundAmount);
        entity.setStatus(status);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static AccountEntity account(
        Long id,
        String code,
        String name,
        int type,
        double balance,
        boolean isDefault,
        int status,
        String notes
    ) {
        AccountEntity entity = new AccountEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setCode(code);
        entity.setName(name);
        entity.setType(type);
        entity.setBalance(balance);
        entity.setIsDefault(isDefault);
        entity.setStatus(status);
        entity.setSortOrder(0);
        entity.setNotes(notes);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static AccountTransferEntity accountTransfer(
        Long id,
        String transferNo,
        Long fromAccountId,
        Long toAccountId,
        double amount,
        double fee,
        int status,
        String notes,
        long createdAt
    ) {
        AccountTransferEntity entity = new AccountTransferEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setTransferNo(transferNo);
        entity.setFromAccountId(fromAccountId);
        entity.setToAccountId(toAccountId);
        entity.setAmount(amount);
        entity.setFee(fee);
        entity.setStatus(status);
        entity.setNotes(notes);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static CashChangeRecordEntity cashChange(
        Long id,
        String orderType,
        Long orderId,
        double receivable,
        double received,
        double changeAmount,
        Long accountId,
        String notes,
        long createdAt
    ) {
        CashChangeRecordEntity entity = new CashChangeRecordEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setOrderType(orderType);
        entity.setOrderId(orderId);
        entity.setReceivable(receivable);
        entity.setReceived(received);
        entity.setChangeAmount(changeAmount);
        entity.setAccountId(accountId);
        entity.setStatus(1);
        entity.setNotes(notes);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private static InventoryMonthlyStatsEntity inventoryMonthlyStats(
        Long id,
        Long productId,
        String productCode,
        String productName,
        double quantityBegin,
        double quantityOut
    ) {
        InventoryMonthlyStatsEntity entity = new InventoryMonthlyStatsEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setProductId(productId);
        entity.setProductCode(productCode);
        entity.setProductName(productName);
        entity.setYear(2026);
        entity.setMonth(6);
        entity.setQuantityBegin(quantityBegin);
        entity.setQuantityIn(0.0);
        entity.setQuantityOut(quantityOut);
        entity.setQuantityAdjust(0.0);
        entity.setQuantityEnd(Math.max(0.0, quantityBegin - quantityOut));
        entity.setTotalCostIn(0.0);
        entity.setTotalCostOut(0.0);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set test entity id", ex);
        }
    }

    private static final class CapturingEmitter extends SseEmitter {
        private final List<String> payloads = new ArrayList<>();
        private boolean completed;

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            for (ResponseBodyEmitter.DataWithMediaType item : builder.build()) {
                payloads.add(String.valueOf(item.getData()));
            }
        }

        private synchronized boolean containsPayload(String marker) {
            return payloads.stream().anyMatch(payload -> payload.contains(marker));
        }

        private synchronized List<String> snapshotPayloads() {
            return List.copyOf(payloads);
        }

        @Override
        public void complete() {
            completed = true;
        }
    }

    private static final class RejectingThreadPoolExecutor extends ThreadPoolExecutor {
        private RejectingThreadPoolExecutor() {
            super(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                runnable -> {
                    Thread thread = new Thread(runnable, "rejecting-audit-write");
                    thread.setDaemon(true);
                    return thread;
                }
            );
        }

        @Override
        public void execute(Runnable command) {
            throw new RejectedExecutionException("test rejection");
        }
    }
}

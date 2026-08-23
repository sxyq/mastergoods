package com.zhihuiji.backend.application.service.v2.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.V2AgentAiService;
import com.zhihuiji.backend.application.service.v2.agent.component.AnswerSynthesizer;
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyGuard;
import com.zhihuiji.backend.application.service.v2.agent.component.SseStreamEmitter;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilder;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionService;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextWindowResolver;
import com.zhihuiji.backend.application.service.v2.agent.context.TokenEstimator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolExecutor;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.CustomerDirectoryLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.CustomerReceivableLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.GeneratePosterPromptTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.ProductCatalogLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.PurchaseOrderLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.ResultVisualizationTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.SupplierDirectoryLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.readonly.SupplierPayableLookupTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePayOrderTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePurchaseOrderTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePurchaseReceiptTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreatePurchaseReturnTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.write.CreateSaleOrderTool;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
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
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReceiptRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderItemRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SalesReturnRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

/**
 * Agent 工具选择与草稿边界回归测试。
 *
 * <p>覆盖优化计划阶段 1/2 的重点回归用例 009/012/016/041/048/049/051/052/053/054，
 * 验证以下服务端逻辑：
 * <ol>
 *   <li>查询类任务满足完成策略 → COMPLETED，不创建草稿</li>
 *   <li>查询完成但目标工具未完成 → EXHAUSTED（不返回成功语义）</li>
 *   <li>目标 CREATE_ONLY 工具成功生成草稿 → CONFIRMATION_PENDING，不写正式业务表</li>
 *   <li>generate_poster_prompt 依赖链：查询+生成 → COMPLETED（READ_ONLY 目标工具）</li>
 *   <li>native transcript 每个 assistant tool call 有配对结果</li>
 *   <li>缺少必填参数的 CREATE_ONLY 工具被跳过，不进入业务 Repository</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class V2AgentToolSelectionRegressionTest {

    @Mock private CurrentOwnerService currentOwnerService;
    @Mock private AgentConversationRepository agentConversationRepository;
    @Mock private AgentMessageRepository agentMessageRepository;
    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private AgentTaskRepository agentTaskRepository;
    @Mock private AgentNotificationRepository agentNotificationRepository;
    @Mock private AgentRunAuditRepository agentRunAuditRepository;
    @Mock private AgentRunAuditEventRepository agentRunAuditEventRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private MediaStorageService mediaStorageService;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private SaleOrderItemRepository saleOrderItemRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock private PurchaseReceiptRepository purchaseReceiptRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SalesReturnRepository salesReturnRepository;
    @Mock private LongCatAnthropicClient longCatAnthropicClient;
    @Mock private AgentContextCheckpointRepository agentContextCheckpointRepository;

    private V2AgentAiService service;
    private ObjectMapper objectMapper;
    private RunAuditService runAuditService;
    private SseStreamEmitter sseStreamEmitter;
    private SafetyGuard safetyGuard;
    private ToolRegistry toolRegistry;
    private ToolPlanner toolPlanner;
    private AnswerSynthesizer answerSynthesizer;
    private List<AgentMessageEntity> agentMessages;
    private Map<String, AgentRunAuditEntity> runAudits;
    private List<AgentRunAuditEventEntity> runAuditEvents;

    @BeforeEach
    void setUp() {
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
            new SupplierPayableLookupTool(supplierRepository),
            new ProductCatalogLookupTool(productRepository),
            new SupplierDirectoryLookupTool(supplierRepository),
            new CustomerDirectoryLookupTool(customerRepository),
            new PurchaseOrderLookupTool(purchaseOrderRepository, purchaseOrderItemRepository),
            new GeneratePosterPromptTool(productRepository),
            new ResultVisualizationTool(objectMapper),
            new CreatePayOrderTool(agentDraftRepository),
            new CreatePurchaseOrderTool(agentDraftRepository),
            new CreatePurchaseReceiptTool(agentDraftRepository),
            new CreatePurchaseReturnTool(agentDraftRepository),
            new CreateSaleOrderTool(agentDraftRepository)
        ));
        toolPlanner = new ToolPlanner(longCatAnthropicClient, toolRegistry, objectMapper);
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry, currentOwnerService);
        answerSynthesizer = new AnswerSynthesizer(
            longCatAnthropicClient,
            sseStreamEmitter,
            runAuditService,
            agentMessageRepository,
            objectMapper,
            toolPlanner
        );
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
        when(currentOwnerService.findCurrentStoreId()).thenReturn(Optional.empty());
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("configured");
        when(longCatAnthropicClient.streamingUnavailableStatus()).thenReturn("disabled");
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(false);
        when(longCatAnthropicClient.supportsToolResultContinuation()).thenReturn(false);
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
        when(agentRunAuditEventRepository.findAllByRunIdAndOwnerUserIdOrderBySeqAsc(anyString(), any()))
            .thenReturn(List.of());
        when(agentRunAuditEventRepository.countByRunIdAndOwnerUserId(anyString(), any())).thenReturn(0L);
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(any(), any()))
            .thenReturn(List.of());
        // Default final answer mock: prevent isLlmFailure from overriding terminal status
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("已为您处理。"));
    }

    // =========================================================================
    // 009 pattern: read-only query → COMPLETED, no drafts
    // =========================================================================

    @Test
    void readOnlyReceivableQueryCompletesWithCompletedTerminalStatus() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receivable-009",
                    "customer_receivable_lookup",
                    objectMapper.createObjectNode()
                )),
                "查询客户应收"
            )))
            .thenReturn(Optional.empty());
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(
            eq(1L), eq(0.0), any()
        )).thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(1L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(100.0);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("当前有 1 个欠款客户。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "哪些客户还欠我钱？", false)
        );

        assertEquals("COMPLETED", response.terminalStatus(), "read-only query must complete");
        assertFalse(response.toolCalls().isEmpty(), "should have tool calls");
        assertEquals("customer_receivable_lookup", response.toolCalls().get(0).toolName());
        assertEquals("completed", response.toolCalls().get(0).status());
        verify(agentDraftRepository, never()).save(any());
    }

    // =========================================================================
    // 049 pattern: write intent + dependency query only → EXHAUSTED
    // =========================================================================

    @Test
    void payOrderDependencyQueryOnlyReturnsExhaustedNotCompleted() {
        // First call: model selects supplier_directory_lookup
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-049",
                    "supplier_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查供应商"
            )))
            // Continuation: model returns terminal text, no more tools
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(),
                "已查到供应商，无需继续。"
            )));
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(supplier(7L, "真实供应商", 500.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已查到供应商。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "给供应商记一笔 1.23 元付款，先别直接付款，做成草稿。", false)
        );

        assertEquals("EXHAUSTED", response.terminalStatus(),
            "query done but target tool not called must be EXHAUSTED, not COMPLETED");
        assertEquals("AGENT_ITERATION_EXHAUSTED", response.errorCode());
        assertTrue(response.missingTargetTools().contains("create_pay_order"),
            "missing target tools must list create_pay_order");
        assertTrue(response.completedTools().contains("supplier_directory_lookup"),
            "completed tools must list supplier_directory_lookup");
        verify(agentDraftRepository, never()).save(any());
    }

    // =========================================================================
    // 049 pattern: write intent + dependency query + create tool → CONFIRMATION_PENDING
    // =========================================================================

    @Test
    void payOrderDependencyQueryAndCreateToolReturnsConfirmationPending() {
        // First call: model selects supplier_directory_lookup
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-049b",
                    "supplier_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查供应商"
            )))
            // Continuation: model selects create_pay_order with real supplier_id
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-pay-049b",
                    "create_pay_order",
                    objectMapper.createObjectNode()
                        .put("supplier_id", 7)
                        .put("supplier_name", "真实供应商")
                        .put("amount", 1.23)
                )),
                "生成付款草稿"
            )));
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(supplier(7L, "真实供应商", 500.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成付款草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "给供应商记一笔 1.23 元付款，先别直接付款，做成草稿。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus(),
            "draft generated must be CONFIRMATION_PENDING, not COMPLETED");
        assertNotNull(response.safeMessage());
        assertTrue(response.safeMessage().contains("草稿") || response.safeMessage().contains("确认"),
            "safe message should mention draft/confirmation");
        assertTrue(response.completedTools().contains("create_pay_order"),
            "completed tools must list create_pay_order");
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
        // No formal business table writes
        verify(paymentRepository, never()).save(any());
        verify(purchaseOrderRepository, never()).save(any());
    }

    // =========================================================================
    // 012 pattern: poster dependency query only → EXHAUSTED
    // =========================================================================

    @Test
    void posterDependencyQueryOnlyReturnsExhaustedNotCompleted() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-012",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查商品"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(),
                "已查到商品。"
            )));
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(product(9L, "P001", "美尚热水器", 10, 5, 199.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(10.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已查到商品。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "拿商品信息帮我写个海报提示词，先不要生成图片。", false)
        );

        assertEquals("EXHAUSTED", response.terminalStatus(),
            "poster query done but generate_poster_prompt not called must be EXHAUSTED");
        assertTrue(response.missingTargetTools().contains("generate_poster_prompt"),
            "missing target tools must list generate_poster_prompt");
        verify(agentDraftRepository, never()).save(any());
    }

    // =========================================================================
    // 012 pattern: poster dependency query + poster tool → COMPLETED
    // =========================================================================

    @Test
    void posterDependencyQueryAndPosterToolReturnsCompleted() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-012b",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查商品"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-poster-012b",
                    "generate_poster_prompt",
                    objectMapper.createObjectNode().put("product_id", 9)
                )),
                "生成海报提示词"
            )));
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(product(9L, "P001", "美尚热水器", 10, 5, 199.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(10.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(productRepository.findByIdAndOwnerUserId(9L, 1L))
            .thenReturn(Optional.of(product(9L, "P001", "美尚热水器", 10, 5, 199.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成海报提示词。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "拿商品信息帮我写个海报提示词，先不要生成图片。", false)
        );

        assertEquals("COMPLETED", response.terminalStatus(),
            "poster is READ_ONLY target; completion = COMPLETED, not CONFIRMATION_PENDING");
        // COMPLETED 响应的 completedTools 字段为空（既有契约），用 toolCalls 验证工具完成。
        assertTrue(response.toolCalls().stream().anyMatch(call ->
                "generate_poster_prompt".equals(call.toolName())),
            "tool calls must include generate_poster_prompt");
        verify(agentDraftRepository, never()).save(any());
    }

    // =========================================================================
    // 051 pattern: purchase order dependency query + create tool → CONFIRMATION_PENDING
    // =========================================================================

    @Test
    void purchaseOrderDraftUsesRealIdsFromDependencyQueries() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-051",
                    "supplier_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查供应商"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-051",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "再查商品"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-purchase-051",
                    "create_purchase_order",
                    objectMapper.valueToTree(java.util.Map.of(
                        "supplier_id", 11,
                        "supplier_name", "真实供应商",
                        "items", List.of(java.util.Map.of(
                            "product_id", 22,
                            "product_name", "真实商品",
                            "quantity", 1,
                            "price", 1.23
                        ))
                    ))
                )),
                "生成采购草稿"
            )));
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(supplier(11L, "真实供应商", 500.0)));
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(product(22L, "P002", "真实商品", 10, 5, 1.23)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(10.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成采购草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "向现有供应商买一个真实商品，数量 1、单价 1.23，先做采购草稿让我看看。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus(),
            "purchase order draft generated must be CONFIRMATION_PENDING");
        assertTrue(response.completedTools().contains("create_purchase_order"),
            "completed tools must list create_purchase_order");
        assertTrue(response.completedTools().contains("supplier_directory_lookup"));
        assertTrue(response.completedTools().contains("product_catalog_lookup"));
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
        // No formal business table writes
        verify(purchaseOrderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    // =========================================================================
    // 051 pattern: create tool with missing parameters → skipped, EXHAUSTED
    // =========================================================================

    @Test
    void createPurchaseOrderWithMissingParametersIsSkippedAndExhausted() {
        // Model tries to call create_purchase_order directly without dependencies
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-premature-051",
                    "create_purchase_order",
                    objectMapper.createObjectNode()
                        .put("supplier_name", "猜测供应商")
                    // missing supplier_id, items
                )),
                "直接生成草稿"
            )))
            .thenReturn(Optional.empty());
        when(longCatAnthropicClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of("无法完成。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "向现有供应商买一个真实商品，数量 1、单价 1.23，先做采购草稿让我看看。", false)
        );

        // Missing required parameters → tool skipped → target not completed
        assertTrue("EXHAUSTED".equals(response.terminalStatus()) || "FAILED".equals(response.terminalStatus()),
            "premature create call with missing params must not be COMPLETED; got " + response.terminalStatus());
        verify(agentDraftRepository, never()).save(any());
        verify(purchaseOrderRepository, never()).save(any());
    }

    // =========================================================================
    // 054 pattern: sale order dependency query + create tool → CONFIRMATION_PENDING
    // =========================================================================

    @Test
    void saleOrderDraftUsesRealIdsFromDependencyQueries() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-customer-054",
                    "customer_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查客户"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-054",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "再查商品"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-sale-054",
                    "create_sale_order",
                    objectMapper.valueToTree(java.util.Map.of(
                        "customer_id", 33,
                        "customer_name", "真实客户",
                        "items", List.of(java.util.Map.of(
                            "product_id", 22,
                            "product_name", "真实商品",
                            "quantity", 1,
                            "price", 1.23
                        ))
                    ))
                )),
                "生成销售草稿"
            )));
        when(customerRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(customer(33L, "真实客户", 0.0)));
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(product(22L, "P002", "真实商品", 10, 5, 1.23)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(10.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成销售草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "给一个现有客户开一单，商品 1 件、单价 1.23，先生成销售草稿。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus(),
            "sale order draft generated must be CONFIRMATION_PENDING");
        assertTrue(response.completedTools().contains("create_sale_order"));
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
        verify(saleOrderRepository, never()).save(any());
    }

    // =========================================================================
    // 052 pattern: purchase receipt dependency query + create tool → CONFIRMATION_PENDING
    // =========================================================================

    @Test
    void purchaseReceiptDraftUsesRealPurchaseOrderId() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-po-052",
                    "purchase_order_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查采购单"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-receipt-052",
                    "create_purchase_receipt",
                    objectMapper.valueToTree(java.util.Map.of(
                        "purchase_order_id", 55,
                        "items", List.of(java.util.Map.of(
                            "product_id", 22,
                            "product_name", "真实商品",
                            "quantity", 1,
                            "price", 1.23
                        ))
                    ))
                )),
                "生成入库草稿"
            )));
        when(purchaseOrderRepository.search(eq(1L), any(), any(), any()))
            .thenReturn(List.of(purchaseOrder(55L, "PO-001", "供应商A", 100.0, 0, 0, 1L)));
        when(purchaseOrderItemRepository.findByOwnerUserIdAndOrderIdIn(eq(1L), any()))
            .thenReturn(List.of());
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成入库草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "把一张采购单里的 1 件货做入库，先生成入库草稿，不要直接记账。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus(),
            "purchase receipt draft generated must be CONFIRMATION_PENDING");
        assertTrue(response.completedTools().contains("create_purchase_receipt"));
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
        verify(purchaseReceiptRepository, never()).save(any());
    }

    // =========================================================================
    // 053 pattern: purchase return dependency query + create tool → CONFIRMATION_PENDING
    // =========================================================================

    @Test
    void purchaseReturnDraftUsesRealPurchaseOrderId() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-po-053",
                    "purchase_order_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查采购单"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-return-053",
                    "create_purchase_return",
                    objectMapper.valueToTree(java.util.Map.of(
                        "purchase_order_id", 55,
                        "reason", "全量工具测试",
                        "items", List.of(java.util.Map.of(
                            "product_id", 22,
                            "product_name", "真实商品",
                            "quantity", 1,
                            "price", 1.23
                        ))
                    ))
                )),
                "生成退货草稿"
            )));
        when(purchaseOrderRepository.search(eq(1L), any(), any(), any()))
            .thenReturn(List.of(purchaseOrder(55L, "PO-001", "供应商A", 100.0, 0, 0, 1L)));
        when(purchaseOrderItemRepository.findByOwnerUserIdAndOrderIdIn(eq(1L), any()))
            .thenReturn(List.of());
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成退货草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "采购来的货退 1 件，原因写全量工具测试，先给我退货草稿。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus(),
            "purchase return draft generated must be CONFIRMATION_PENDING");
        assertTrue(response.completedTools().contains("create_purchase_return"));
        verify(agentDraftRepository).save(any(AgentDraftEntity.class));
        verify(purchaseReturnRepository, never()).save(any());
    }

    // =========================================================================
    // 049 pattern: dependency query only → EXHAUSTED, not success semantics
    // =========================================================================

    @Test
    void exhaustedResponseDoesNotClaimSuccessInAnswerOrAudit() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-exh",
                    "supplier_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查供应商"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(),
                "已查到供应商。"
            )));
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(supplier(7L, "真实供应商", 500.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已查到供应商。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "给供应商记一笔 1.23 元付款，先别直接付款，做成草稿。", false)
        );

        assertEquals("EXHAUSTED", response.terminalStatus());
        assertEquals("AGENT_ITERATION_EXHAUSTED", response.errorCode());
        assertNotNull(response.safeMessage());
        assertTrue(response.safeMessage().contains("未完成") || response.safeMessage().contains("未写入"),
            "EXHAUSTED safe message must state what's missing/incomplete");
        assertTrue(response.answer().contains("[状态]") || response.answer().contains("未完成"),
            "EXHAUSTED answer must include status suffix, not claim success");
        // Audit must record exhausted status
        AgentRunAuditEntity audit = runAudits.get(response.runId());
        assertNotNull(audit);
        assertEquals("exhausted", audit.getStatus());
    }

    // =========================================================================
    // 048 pattern: write intent + dependency query only → EXHAUSTED
    // =========================================================================

    @Test
    void inventoryCountDraftDependencyQueryOnlyReturnsExhausted() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-product-048",
                    "product_catalog_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查商品"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(),
                "已查到商品。"
            )));
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(product(9L, "P001", "商品A", 10, 5, 199.0)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(1L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(10.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(0L);
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已查到商品。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "选一个商品按现在的库存做一次盘点，先生成草稿给我确认。", false)
        );

        assertEquals("EXHAUSTED", response.terminalStatus(),
            "dependency query only must be EXHAUSTED, not COMPLETED");
        assertTrue(response.missingTargetTools().contains("create_inventory_count_draft"),
            "missing target tools must list create_inventory_count_draft");
        verify(agentDraftRepository, never()).save(any());
    }

    // =========================================================================
    // 049 pattern: CONFIRMATION_PENDING answer must not claim formal write
    // =========================================================================

    @Test
    void confirmationPendingAnswerDoesNotClaimFormalWrite() {
        when(longCatAnthropicClient.createMessageWithTools(anyString(), anyString(), any()))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-supplier-conf",
                    "supplier_directory_lookup",
                    objectMapper.createObjectNode()
                )),
                "先查供应商"
            )))
            .thenReturn(Optional.of(new LongCatAnthropicClient.ToolUseResponse(
                List.of(new LongCatAnthropicClient.ToolUseBlock(
                    "call-pay-conf",
                    "create_pay_order",
                    objectMapper.createObjectNode()
                        .put("supplier_id", 7)
                        .put("supplier_name", "真实供应商")
                        .put("amount", 1.23)
                )),
                "生成付款草稿"
            )));
        when(supplierRepository.findAllByOwnerUserIdOrderByNameAsc(eq(1L), any()))
            .thenReturn(List.of(supplier(7L, "真实供应商", 500.0)));
        when(longCatAnthropicClient.createJsonMessage(anyString(), contains("本轮工具真实结果 JSON")))
            .thenReturn(Optional.of("已生成付款草稿。"));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null,
                "给供应商记一笔 1.23 元付款，先别直接付款，做成草稿。", false)
        );

        assertEquals("CONFIRMATION_PENDING", response.terminalStatus());
        assertTrue(response.answer().contains("[状态]") && response.answer().contains("确认"),
            "CONFIRMATION_PENDING answer must include status suffix mentioning confirmation");
        assertTrue(response.answer().contains("未创建任何正式单据") || response.answer().contains("未写入"),
            "CONFIRMATION_PENDING answer must state no formal document was created");
        // Audit must record confirmation_pending status
        AgentRunAuditEntity audit = runAudits.get(response.runId());
        assertNotNull(audit);
        assertEquals("confirmation_pending", audit.getStatus());
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

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

    private static CustomerEntity customer(Long id, String name, double balance) {
        CustomerEntity entity = new CustomerEntity();
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

    private static PurchaseOrderEntity purchaseOrder(
        Long id, String orderNo, String supplierName,
        double totalAmount, double paidAmount, double receivedAmount, long supplierId
    ) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setSupplierId(supplierId);
        entity.setOrderNo(orderNo);
        entity.setSupplierName(supplierName);
        entity.setTotalAmount(totalAmount);
        entity.setPaidAmount(paidAmount);
        entity.setReceivedAmount(receivedAmount);
        entity.setSettlementMethod(1);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
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
}

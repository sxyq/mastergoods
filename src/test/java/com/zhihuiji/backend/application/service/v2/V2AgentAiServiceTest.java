package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.repository.AgentConversationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import com.zhihuiji.backend.infrastructure.repository.PayOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.PaymentRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class V2AgentAiServiceTest {
    @Mock private CurrentOwnerService currentOwnerService;
    @Mock private AgentConversationRepository agentConversationRepository;
    @Mock private AgentMessageRepository agentMessageRepository;
    @Mock private AgentDraftRepository agentDraftRepository;
    @Mock private AgentTaskRepository agentTaskRepository;
    @Mock private AgentNotificationRepository agentNotificationRepository;
    @Mock private AgentRunAuditRepository agentRunAuditRepository;
    @Mock private AgentRunAuditEventRepository agentRunAuditEventRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SaleOrderRepository saleOrderRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PayOrderRepository payOrderRepository;
    @Mock private FinanceRecordRepository financeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private LongCatAnthropicClient longCatAnthropicClient;

    private V2AgentAiService service;
    private Map<String, AgentRunAuditEntity> runAudits;
    private List<AgentRunAuditEventEntity> runAuditEvents;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        runAudits = new HashMap<>();
        runAuditEvents = new CopyOnWriteArrayList<>();
        service = new V2AgentAiService(
            currentOwnerService,
            agentConversationRepository,
            agentMessageRepository,
            agentDraftRepository,
            agentTaskRepository,
            agentNotificationRepository,
            agentRunAuditRepository,
            agentRunAuditEventRepository,
            productRepository,
            customerRepository,
            supplierRepository,
            saleOrderRepository,
            purchaseOrderRepository,
            payOrderRepository,
            financeRecordRepository,
            paymentRepository,
            new ObjectMapper(),
            longCatAnthropicClient
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
        when(agentMessageRepository.save(any(AgentMessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        when(agentRunAuditEventRepository.findAllByRunIdOrderBySeqAsc(anyString())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0, String.class);
            return runAuditEvents.stream()
                .filter(event -> runId.equals(event.getRunId()))
                .sorted((left, right) -> Integer.compare(left.getSeq(), right.getSeq()))
                .toList();
        });
        when(agentRunAuditEventRepository.countByRunId(anyString())).thenAnswer(invocation -> {
            String runId = invocation.getArgument(0, String.class);
            return runAuditEvents.stream()
                .filter(event -> runId.equals(event.getRunId()))
                .count();
        });
    }

    @Test
    void emptyReceivableResultDoesNotEmitInvalidBarChart() {
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertTrue(hasBlock(response, "kpi_grid"));
        assertTrue(hasBlock(response, "rank_list"));
        assertFalse(hasBlock(response, "bar_chart"));
        assertEquals("tool_query_rule_summary", response.mode());
        assertEquals("disabled", response.llmStatus());
        assertTrue(response.answer().contains("规则摘要"), response.answer());
        assertTrue(response.answer().contains("当前未使用模型生成"), response.answer());
    }

    @Test
    void emptySupplierPayableResultDoesNotEmitInvalidBarChart() {
        when(supplierRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());
        when(purchaseOrderRepository.search(1L, null, null)).thenReturn(List.of());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "供应商应付情况", false)
        );

        assertTrue(hasBlock(response, "kpi_grid"));
        assertTrue(hasBlock(response, "rank_list"));
        assertFalse(hasBlock(response, "bar_chart"));
        verify(supplierRepository).findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(
            1L,
            0.0,
            PageRequest.of(0, 10)
        );
    }

    @Test
    void workbenchDoesNotExposeReportDashboardDefaults() {
        V2AgentDtos.AgentWorkbenchResponse response = service.getWorkbench();

        assertTrue(response.kpiCards().isEmpty());
        assertTrue(response.riskAlerts().isEmpty());
        assertTrue(response.todaySummary() == null || response.todaySummary().isBlank());
        assertTrue(response.quickQuestions().isEmpty());
        assertTrue(response.quickQuestions().stream().noneMatch(V2AgentAiServiceTest::isReportLikeQuestion));
        assertTrue(response.recentConversations().isEmpty());
        assertTrue(response.pendingDrafts().isEmpty());
        assertEquals("你好，我是智慧记 AI 助手", response.greeting());
        assertFalse(isReportLikeQuestion(response.greeting()));
        assertEquals("clean_entry_ready", response.status());
        assertNotNull(response.dataPolicy());
        assertTrue(response.dataPolicy().contains("不预取或展示报表型经营数据"));
        assertTrue(response.dataPolicy().contains("发送问题后才创建真实"));
        assertEquals(3, response.capabilities().size());
        assertTrue(response.capabilities().stream().anyMatch(capability -> "real_data_chat".equals(capability.id())));
        assertTrue(response.capabilities().stream().anyMatch(capability -> "auditable_agent_trace".equals(capability.id())));
        assertFalse(response.warnings().isEmpty());
        assertTrue(response.warnings().get(0).contains("不返回默认 KPI"));
        verify(currentOwnerService).requireCurrentOwnerUserId();
        verifyNoInteractions(agentConversationRepository, agentDraftRepository, agentMessageRepository);
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

    @Test
    void streamFallbackAnswerCompletesRuleSummaryWithoutFakeDeltas() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(101L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-test", emitter);

        assertEquals(0, answerDeltaPayloads(emitter).size(), String.join("\n", emitter.payloads));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_rule_summary\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"stream_failed_or_empty\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")), String.join("\n", emitter.payloads));
        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("\"plan_source\":\"keyword_fallback\""), answerCompleted);
        assertFalse(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())));
        assertTrue(emitter.completed);
    }

    @Test
    void streamDisabledModelAnswerCompletesRuleSummaryWithoutFakeDeltas() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(102L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-disabled", emitter);

        assertEquals(0, answerDeltaPayloads(emitter).size(), String.join("\n", emitter.payloads));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_rule_summary\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"disabled\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")), String.join("\n", emitter.payloads));
        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("\"plan_source\":\"keyword_fallback\""), answerCompleted);
        assertTrue(answerCompleted.contains("\"audit_id\":\"run-disabled:audit\""), answerCompleted);
        assertTrue(answerCompleted.contains("\"trace_id\":\"run-disabled:trace\""), answerCompleted);
        assertTrue(answerCompleted.contains("\"log_ref\":\"agent-run:run-disabled\""), answerCompleted);
        assertFalse(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())));
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerEmitsOnlyModelStreamDeltasAndStreamedCompletion() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                onDelta.accept("应收100元");
                return Optional.of("客户A应收100元");
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(104L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-model-stream", emitter);

        List<String> deltaPayloads = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .toList();
        List<String> modelDeltaPayloads = deltaPayloads.stream()
            .filter(payload -> payload.contains("\"delta_source\":\"model_stream\""))
            .toList();
        assertEquals(2, modelDeltaPayloads.size(), String.join("\n", emitter.payloads));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"audit_id\":\"run-model-stream:audit\"")));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"trace_id\":\"run-model-stream:trace\"")));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"log_ref\":\"agent-run:run-model-stream\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_llm_streamed\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"streaming\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"rule_summary\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")));
        String answerCompleted = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(answerCompleted.contains("\"plan_source\":\"keyword_fallback\""), answerCompleted);
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"tool_completed\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_delta\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"answer_delta\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"result_block\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"result_block\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerBatchesSmallDeltasWithoutDelayingFirstVisibleAnswer() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                onDelta.accept("应收");
                onDelta.accept("100");
                onDelta.accept("元");
                onDelta.accept("，建议跟进。");
                return Optional.of("客户A应收100元，建议跟进。");
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(112L), "客户应收情况", "run-model-batch", emitter);

        List<String> modelDeltaPayloads = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .filter(payload -> payload.contains("\"delta_source\":\"model_stream\""))
            .toList();
        assertTrue(modelDeltaPayloads.size() < 5, String.join("\n", emitter.payloads));
        assertTrue(modelDeltaPayloads.stream().anyMatch(payload -> payload.contains("客户A")), String.join("\n", modelDeltaPayloads));
        assertTrue(modelDeltaPayloads.stream().anyMatch(payload -> payload.contains("建议跟进")), String.join("\n", modelDeltaPayloads));
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"answer_delta\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"result_block\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("建议跟进")));
    }

    @Test
    void streamInterruptedAfterVisibleModelDeltaKeepsPartialAnswerAndBlocksAfterText() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A应收");
                return Optional.empty();
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(111L), "客户应收情况", "run-stream-interrupted", emitter);

        String answerDelta = firstPayload(emitter, "\"event_type\":\"answer_delta\"");
        assertTrue(answerDelta.contains("\"delta_source\":\"model_stream\""), answerDelta);
        assertTrue(answerDelta.contains("客户A应收"), answerDelta);
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"answer_delta\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"result_block\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"result_block\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\""),
            String.join("\n", emitter.payloads)
        );
        String completed = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(completed.contains("\"mode\":\"tool_query_llm_stream_interrupted\""), completed);
        assertTrue(completed.contains("\"llm_status\":\"stream_interrupted\""), completed);
        assertTrue(completed.contains("客户A应收"), completed);
        assertFalse(completed.contains("当前未使用模型生成"), completed);
        assertTrue(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"model_stream\"")));
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerEmitsServerNoticeTailBeforeCompletionWhenBackendAppendsBoundaries() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        List<com.zhihuiji.backend.domain.entity.CustomerEntity> customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            customers.add(customer((long) i, "客户" + i, 100.0 + i));
        }
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(customers);
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(10L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(1055.0);
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户应收 Top10 已查询。");
                return Optional.of("客户应收 Top10 已查询。");
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(110L), "客户应收情况", "run-server-notice", emitter);

        String modelDelta = firstPayload(emitter, "\"delta_source\":\"model_stream\"");
        String serverNotice = firstPayload(emitter, "\"delta_source\":\"server_notice\"");
        assertTrue(serverNotice.contains("查询边界"), serverNotice);
        assertTrue(serverNotice.contains("客户应收查询仅返回前 10 条"), serverNotice);
        assertTrue(
            firstPayloadIndex(emitter, modelDelta) < firstPayloadIndex(emitter, serverNotice),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, serverNotice)
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\""),
            String.join("\n", emitter.payloads)
        );
        String completed = firstPayload(emitter, "\"event_type\":\"answer_completed\"");
        assertTrue(completed.contains("查询边界"), completed);
        assertTrue(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"server_notice\"")));
    }

    @Test
    void streamToolCompletedIncludesAuditMetadata() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0), customer(2L, "客户B", 80.0)));
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(103L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-audit", emitter);

        String completedPayload = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"tool_completed\""))
            .findFirst()
            .orElse("");
        assertTrue(completedPayload.contains("\"tool_name\":\"customer_receivable_lookup\""), completedPayload);
        assertTrue(completedPayload.contains("\"tool_call_id\":\"run-audit:customer_receivable_lookup\""), completedPayload);
        assertTrue(completedPayload.contains("\"audit_id\":\"run-audit:audit\""), completedPayload);
        assertTrue(completedPayload.contains("\"trace_id\":\"run-audit:trace\""), completedPayload);
        assertTrue(completedPayload.contains("\"returned_count\":2"), completedPayload);
        assertTrue(completedPayload.contains("\"limit\":10"), completedPayload);
        assertTrue(completedPayload.contains("\"is_truncated\":false"), completedPayload);
        assertTrue(completedPayload.contains("\"duration_ms\""), completedPayload);
        assertTrue(completedPayload.contains("\"input_summary\":\"查询当前账号客户应收余额"), completedPayload);
        assertTrue(completedPayload.contains("\"query_window\":{\"owner_scope\":\"current_owner\",\"limit\":10}"), completedPayload);
        assertTrue(completedPayload.contains("\"started_at\""), completedPayload);
        assertTrue(completedPayload.contains("\"completed_at\""), completedPayload);
        assertTrue(completedPayload.contains("\"next_cursor\":null"), completedPayload);
        assertTrue(completedPayload.contains("\"evidence\":{\"source\":\"tool:customer_receivable_lookup\""), completedPayload);
    }

    @Test
    void streamEmitsEachToolResultBlockBeforeNextToolCompletes() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(productRepository.findLowStockProducts(1L, PageRequest.of(0, 10)))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 2.0, 10.0, 12.5)));
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(109L), "库存和客户应收情况", "run-multi-blocks", emitter);

        int inventoryCompleted = firstPayloadIndexContaining(
            emitter,
            "\"event_type\":\"tool_completed\"",
            "\"tool_name\":\"inventory_low_stock_lookup\""
        );
        int firstResultBlock = firstPayloadIndex(emitter, "\"event_type\":\"result_block\"");
        int receivableCompleted = firstPayloadIndexContaining(
            emitter,
            "\"event_type\":\"tool_completed\"",
            "\"tool_name\":\"customer_receivable_lookup\""
        );
        int answerCompleted = firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\"");

        assertTrue(inventoryCompleted < receivableCompleted, String.join("\n", emitter.payloads));
        assertTrue(receivableCompleted < answerCompleted, String.join("\n", emitter.payloads));
        assertTrue(answerCompleted < firstResultBlock, String.join("\n", emitter.payloads));
        assertTrue(firstPayloadIndex(emitter, "\"title\":\"库存风险\"") > answerCompleted, String.join("\n", emitter.payloads));
        assertTrue(firstPayloadIndex(emitter, "\"title\":\"应收概览\"") > receivableCompleted, String.join("\n", emitter.payloads));
        assertTrue(firstPayloadIndex(emitter, "\"title\":\"本次回答依据\"") > answerCompleted, String.join("\n", emitter.payloads));
    }

    @Test
    void streamEventsIncludeCompatibleEnvelopeMetadata() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                return Optional.of("客户A应收100元");
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(105L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-envelope", emitter);

        String runStarted = firstPayload(emitter, "\"event_type\":\"run_started\"");
        assertTrue(runStarted.contains("\"event_id\":\"run-envelope:1\""), runStarted);
        assertTrue(runStarted.contains("\"seq\":1"), runStarted);
        assertTrue(runStarted.contains("\"conversation_id\":105"), runStarted);

        String planDelta = firstPayload(emitter, "\"event_type\":\"plan_delta\"");
        assertTrue(planDelta.contains("\"plan_source\":\"keyword_fallback\""), planDelta);
        assertTrue(planDelta.contains("关键词兜底"), planDelta);

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
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"answer_delta\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"result_block\""),
            String.join("\n", emitter.payloads)
        );
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"result_block\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\""),
            String.join("\n", emitter.payloads)
        );

        AgentRunAuditEntity audit = runAudits.get("run-envelope");
        assertNotNull(audit);
        assertEquals(1L, audit.getOwnerUserId());
        assertEquals(105L, audit.getConversationId());
        assertEquals("completed", audit.getStatus());
        assertEquals("tool_query_llm_streamed", audit.getMode());
        assertEquals("streaming", audit.getLlmStatus());
        assertEquals("keyword_fallback", audit.getPlanSource());
        assertEquals(1, audit.getToolCount());
        assertTrue(audit.getEventCount() > 0);
        verify(agentRunAuditRepository, times(2)).save(any(AgentRunAuditEntity.class));
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> "run-envelope".equals(event.getRunId()))
            .toList();
        assertEquals(audit.getEventCount(), events.size());
        assertTrue(events.stream().anyMatch(event -> "run_started".equals(event.getEventType())));
        assertTrue(events.stream().anyMatch(event -> "plan_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"plan_source\":\"keyword_fallback\"")));
        assertTrue(events.stream().anyMatch(event -> "tool_completed".equals(event.getEventType())
            && event.getPayloadJson().contains("\"tool_name\":\"customer_receivable_lookup\"")));
        assertTrue(events.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"model_stream\"")));
        assertSequentialAuditEvents("run-envelope", events);
    }

    @Test
    void slowAuditEventWriteDoesNotBlockVisibleStreamPayloads() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                return Optional.of("客户A应收100元");
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
                service.runChatStream(1L, conversation(107L), "客户应收情况", "run-slow-audit", emitter);
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
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                return Optional.of("客户A应收100元");
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

        service.runChatStream(1L, conversation(108L), "客户应收情况", "run-audit-write-failure", emitter);

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
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                return Optional.of("客户A应收100元");
            });
        replaceAuditWriteExecutor(new RejectingThreadPoolExecutor());
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(109L), "客户应收情况", "run-audit-rejected", emitter);

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
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        CountDownLatch modelStreamEntered = new CountDownLatch(1);
        CountDownLatch releaseModelStream = new CountDownLatch(1);
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                modelStreamEntered.countDown();
                assertTrue(releaseModelStream.await(3, TimeUnit.SECONDS), "model stream release timed out");
                return Optional.of("模型返回但用户已经取消");
            });
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(107L);
        AtomicReference<Throwable> streamFailure = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            try {
                service.runChatStream(1L, conversation, "客户应收情况", "run-cancel", emitter);
            } catch (RuntimeException ex) {
                if (!String.valueOf(ex.getMessage()).startsWith("Agent run cancelled: run-cancel")) {
                    streamFailure.set(ex);
                }
            } catch (Throwable ex) {
                streamFailure.set(ex);
            }
        });
        worker.start();
        assertTrue(modelStreamEntered.await(3, TimeUnit.SECONDS), "stream did not reach model call");

        V2AgentDtos.AgentRunCancelResponse response = service.cancelRun("run-cancel");
        releaseModelStream.countDown();
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
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0), customer(2L, "客户B", 80.0)));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertEquals("keyword_fallback", response.planSource());
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
        assertTrue(hasBlock(response, "evidence_card"));
        assertEquals(response.blocks().size(), response.resultBlocks().size());
        assertTrue(response.performanceSummary().durationMs() >= 0);
        assertTrue(response.performanceSummary().toolDurationMs() >= 0);
        assertEquals(0L, response.performanceSummary().modelDurationMs());
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
    }

    @Test
    void nonStreamingChatExplainsLimitedQueryBoundaryAndFieldLevelEvidence() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        List<com.zhihuiji.backend.domain.entity.CustomerEntity> customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            customers.add(customer((long) i, "客户" + i, 100.0 + i));
        }
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(customers);
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(10L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(1055.0);

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
        assertTrue(response.blocks().stream().anyMatch(block ->
            "evidence_card".equals(block.blockType())
                && block.data().toString().contains("customer_count")
                && block.data().toString().contains("total_receivable")
                && block.data().toString().contains("top10_receivable_total")
                && block.data().toString().contains("tool:customer_receivable_lookup")
        ));
    }

    @Test
    void productCatalogUsesRepositoryAggregatesForSummaryInsteadOfPageSample() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(productRepository.findAllByOwnerUserIdOrderByNameAsc(1L, PageRequest.of(0, 10)))
            .thenReturn(List.of(product(1L, "SKU-1", "纸巾", 2.0, 10.0, 12.5)));
        when(productRepository.countByOwnerUserId(1L)).thenReturn(25L);
        when(productRepository.sumStockByOwnerUserId(1L)).thenReturn(300.0);
        when(productRepository.countLowStockByOwnerUserId(1L)).thenReturn(4L);

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "商品目录情况", false)
        );

        assertTrue(response.answer().contains("商品总数 25 个"), response.answer());
        assertTrue(response.answer().contains("库存总计 300"), response.answer());
        assertTrue(response.answer().contains("低库存商品 4 个"), response.answer());
        assertTrue(response.blocks().stream().anyMatch(block ->
            "kpi_grid".equals(block.blockType())
                && block.data().toString().contains("商品总数")
                && block.data().toString().contains("\"value\":\"25\"")
                && block.data().toString().contains("库存总计")
                && block.data().toString().contains("\"value\":\"300\"")
                && block.data().toString().contains("低库存商品")
                && block.data().toString().contains("\"value\":\"4\"")
        ));
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
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(customerRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(12L);
        when(customerRepository.sumPositiveBalance(1L)).thenReturn(900.0);
        when(supplierRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(supplier(1L, "供应商A", 80.0)));
        when(supplierRepository.countByOwnerUserIdAndBalanceGreaterThan(1L, 0.0)).thenReturn(8L);
        when(supplierRepository.sumPositiveBalance(1L)).thenReturn(700.0);
        when(purchaseOrderRepository.search(1L, null, null, PageRequest.of(0, 10))).thenReturn(List.of());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收和供应商应付情况", false)
        );

        assertTrue(response.answer().contains("欠款客户总数 12 个"), response.answer());
        assertTrue(response.answer().contains("应收总额 ¥900.00"), response.answer());
        assertTrue(response.answer().contains("应付供应商总数 8 个"), response.answer());
        assertTrue(response.answer().contains("应付总额 ¥700.00"), response.answer());
        assertTrue(response.blocks().stream().anyMatch(block ->
            "kpi_grid".equals(block.blockType())
                && block.data().toString().contains("欠款客户总数")
                && block.data().toString().contains("\"value\":\"12\"")
                && block.data().toString().contains("应收总额")
                && block.data().toString().contains("¥900.00")
        ));
        assertTrue(response.blocks().stream().anyMatch(block ->
            "kpi_grid".equals(block.blockType())
                && block.data().toString().contains("应付供应商总数")
                && block.data().toString().contains("\"value\":\"8\"")
                && block.data().toString().contains("应付总额")
                && block.data().toString().contains("¥700.00")
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_receivable") && "¥900.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("top10_receivable_total") && "¥100.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("total_payable") && "¥700.00".equals(ref.value())
        ));
        assertTrue(response.evidenceRefs().stream().anyMatch(ref ->
            ref.label().contains("top10_payable_total") && "¥80.00".equals(ref.value())
        ));
    }

    @Test
    void nonStreamingRuleSummaryDistinguishesNotConfiguredModelStatus() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(longCatAnthropicClient.configurationStatus()).thenReturn("not_configured");
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertEquals("tool_query_rule_summary", response.mode());
        assertEquals("not_configured", response.llmStatus());
        assertEquals(0L, response.performanceSummary().modelDurationMs());
        assertTrue(response.answer().contains("当前未使用模型生成"), response.answer());
    }

    @Test
    void streamRuleSummaryDistinguishesNonStreamingProviderFromDisabledModel() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(false);
        when(longCatAnthropicClient.streamingUnavailableStatus()).thenReturn("stream_not_supported");
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(108L), "客户应收情况", "run-no-stream-provider", emitter);

        assertEquals(0, answerDeltaPayloads(emitter).size(), String.join("\n", emitter.payloads));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_rule_summary\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"stream_not_supported\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")), String.join("\n", emitter.payloads));
        assertTrue(
            firstPayloadIndex(emitter, "\"event_type\":\"answer_completed\"")
                < firstPayloadIndex(emitter, "\"event_type\":\"result_block\""),
            String.join("\n", emitter.payloads)
        );
        assertFalse(runAuditEvents.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())));
        assertTrue(emitter.completed);
    }

    @Test
    void nonStreamingChatPlansEnglishBusinessKeywordsForDeviceQa() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(supplierRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());
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

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "recent sales purchase finance business overview", false)
        );

        assertEquals("keyword_fallback", response.planSource());
        assertTrue(response.planSummary().contains("supplier_payable_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("purchase_order_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("finance_record_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("sales_overview_lookup"), response.planSummary());
        assertEquals(4, response.toolCalls().size());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "supplier_payable_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "purchase_order_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "finance_record_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "sales_overview_lookup".equals(tool.toolName())));
        assertTrue(response.resultBlocks().stream().anyMatch(block ->
            "line_chart".equals(block.blockType()) && block.data().toString().contains("\"回款\"")
        ));
        verify(saleOrderRepository).salesTrendBuckets(any(), any(), any(), any(), any());
        verify(saleOrderRepository, never()).findByOwnerUserIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    void ruleSummaryDoesNotInventZeroSalesOverviewFieldsWhenFactsAreMissing() throws Exception {
        Object toolResult = toolExecutionResult(
            "sales_overview_lookup",
            "销售概览缺少聚合字段",
            new ObjectMapper().createObjectNode()
        );

        String answer = synthesizeAnswer("查看销售额", List.of(toolResult), "兜底回答");

        assertTrue(answer.contains("后端未返回销售笔数"), answer);
        assertTrue(answer.contains("后端未返回销售额"), answer);
        assertTrue(answer.contains("后端未返回回款金额"), answer);
        assertFalse(answer.contains("¥0.00"), answer);
        assertFalse(answer.contains("销售 0 笔"), answer);
    }

    @Test
    void ruleSummaryDoesNotInventZeroAmountsForMissingToolFacts() throws Exception {
        List<Object> toolResults = List.of(
            toolExecutionResult("inventory_low_stock_lookup", "库存缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("product_catalog_lookup", "商品缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("customer_receivable_lookup", "应收缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("supplier_payable_lookup", "应付缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("sale_order_lookup", "销售单缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("purchase_order_lookup", "采购单缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("pay_order_lookup", "付款单缺字段", new ObjectMapper().createObjectNode()),
            toolExecutionResult("finance_record_lookup", "流水缺字段", new ObjectMapper().createObjectNode())
        );

        String answer = synthesizeAnswer("汇总经营情况", toolResults, "兜底回答");

        assertTrue(answer.contains("后端未返回低库存商品数量"), answer);
        assertTrue(answer.contains("后端未返回库存总计"), answer);
        assertTrue(answer.contains("后端未返回应收总额"), answer);
        assertTrue(answer.contains("后端未返回应付总额"), answer);
        assertTrue(answer.contains("后端未返回销售额"), answer);
        assertTrue(answer.contains("后端未返回采购额"), answer);
        assertTrue(answer.contains("后端未返回付款额"), answer);
        assertTrue(answer.contains("后端未返回收入"), answer);
        assertFalse(answer.contains("¥0.00"), answer);
        assertFalse(answer.contains("查询 0 条"), answer);
        assertFalse(answer.contains("发现 0 个"), answer);
        assertFalse(answer.contains("查询到 0 个"), answer);
    }

    @Test
    void getRunAuditReturnsOwnerScopedSummaryAndEvents() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
        when(longCatAnthropicClient.supportsStreaming()).thenReturn(true);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0)));
        when(longCatAnthropicClient.streamTextMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> onDelta = invocation.getArgument(2, Consumer.class);
                onDelta.accept("客户A");
                return Optional.of("客户A应收100元");
            });
        CapturingEmitter emitter = new CapturingEmitter();

        service.runChatStream(1L, conversation(106L), "客户应收情况", "run-audit-read", emitter);

        V2AgentDtos.AgentRunAuditResponse response = service.getRunAudit("run-audit-read");

        assertEquals("run-audit-read", response.runId());
        assertEquals(1L, response.ownerUserId());
        assertEquals(106L, response.conversationId());
        assertEquals("completed", response.status());
        assertEquals("tool_query_llm_streamed", response.mode());
        assertEquals("streaming", response.llmStatus());
        assertEquals("keyword_fallback", response.planSource());
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

    private static boolean hasBlock(V2AgentDtos.AgentChatResponse response, String blockType) {
        return response.blocks().stream().anyMatch(block -> blockType.equals(block.blockType()));
    }

    private String synthesizeAnswer(String userMessage, List<?> toolResults, String fallbackAnswer) throws Exception {
        Method method = V2AgentAiService.class.getDeclaredMethod("synthesizeAnswer", String.class, List.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, userMessage, toolResults, fallbackAnswer);
    }

    private static Object toolExecutionResult(String toolName, String summary, JsonNode facts) throws Exception {
        Class<?> toolResultClass = Class.forName(
            "com.zhihuiji.backend.application.service.v2.V2AgentAiService$ToolExecutionResult"
        );
        Constructor<?> constructor = toolResultClass.getDeclaredConstructor(String.class, String.class, JsonNode.class);
        constructor.setAccessible(true);
        return constructor.newInstance(toolName, summary, facts);
    }

    @SuppressWarnings("unchecked")
    private void registerActiveRun(Long ownerUserId, String runId, Long conversationId, SseEmitter emitter) throws Exception {
        Field activeRunsField = V2AgentAiService.class.getDeclaredField("activeRuns");
        activeRunsField.setAccessible(true);
        Map<String, Object> activeRuns = (Map<String, Object>) activeRunsField.get(service);
        Class<?> activeRunClass = Class.forName(
            "com.zhihuiji.backend.application.service.v2.V2AgentAiService$ActiveAgentRun"
        );
        Constructor<?> constructor = activeRunClass.getDeclaredConstructor(
            Long.class,
            String.class,
            Long.class,
            SseEmitter.class
        );
        constructor.setAccessible(true);
        activeRuns.put(runId, constructor.newInstance(ownerUserId, runId, conversationId, emitter));
    }

    private void replaceAuditWriteExecutor(ThreadPoolExecutor executor) throws Exception {
        Field auditExecutorField = V2AgentAiService.class.getDeclaredField("auditWriteExecutor");
        auditExecutorField.setAccessible(true);
        ThreadPoolExecutor previous = (ThreadPoolExecutor) auditExecutorField.get(service);
        previous.shutdownNow();
        auditExecutorField.set(service, executor);
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

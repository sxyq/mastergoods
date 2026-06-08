package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        runAuditEvents = new ArrayList<>();
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
        when(agentConversationRepository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(1L, PageRequest.of(0, 5)))
            .thenReturn(List.of());
        when(agentDraftRepository.findAllByOwnerUserIdAndStatusIgnoreCaseOrderByUpdatedAtDescIdDesc(1L, "active", PageRequest.of(0, 5)))
            .thenReturn(List.of());

        V2AgentDtos.AgentWorkbenchResponse response = service.getWorkbench();

        assertTrue(response.kpiCards().isEmpty());
        assertTrue(response.riskAlerts().isEmpty());
        assertTrue(response.todaySummary() == null || response.todaySummary().isBlank());
        assertTrue(response.quickQuestions().isEmpty());
        assertTrue(response.quickQuestions().stream().noneMatch(V2AgentAiServiceTest::isReportLikeQuestion));
        assertEquals("你好，我是智慧记 AI 助手", response.greeting());
        assertFalse(isReportLikeQuestion(response.greeting()));
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
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of());
        CapturingEmitter emitter = new CapturingEmitter();
        AgentConversationEntity conversation = conversation(101L);

        service.runChatStream(1L, conversation, "客户应收情况", "run-test", emitter);

        long deltaCount = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .count();
        assertEquals(0, deltaCount, String.join("\n", emitter.payloads));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_rule_summary\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"stream_failed_or_empty\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")), String.join("\n", emitter.payloads));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
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

        long deltaCount = emitter.payloads.stream()
            .filter(payload -> payload.contains("\"event_type\":\"answer_delta\""))
            .count();
        assertEquals(0, deltaCount, String.join("\n", emitter.payloads));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_rule_summary\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"disabled\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")), String.join("\n", emitter.payloads));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"audit_id\":\"run-disabled:audit\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"trace_id\":\"run-disabled:trace\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"log_ref\":\"agent-run:run-disabled\"")));
        assertTrue(emitter.completed);
    }

    @Test
    void streamModelAnswerEmitsOnlyModelStreamDeltasAndStreamedCompletion() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
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
        assertEquals(2, deltaPayloads.size(), String.join("\n", emitter.payloads));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"delta_source\":\"model_stream\"")));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"audit_id\":\"run-model-stream:audit\"")));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"trace_id\":\"run-model-stream:trace\"")));
        assertTrue(deltaPayloads.stream().allMatch(payload -> payload.contains("\"log_ref\":\"agent-run:run-model-stream\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"mode\":\"tool_query_llm_streamed\"")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"llm_status\":\"streaming\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"delta_source\":\"rule_summary\"")));
        assertFalse(emitter.payloads.stream().anyMatch(payload -> payload.contains("当前未使用模型生成")));
        assertTrue(emitter.payloads.stream().anyMatch(payload -> payload.contains("\"event_type\":\"answer_completed\"")));
        assertTrue(emitter.completed);
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
    void streamEventsIncludeCompatibleEnvelopeMetadata() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
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

        AgentRunAuditEntity audit = runAudits.get("run-envelope");
        assertNotNull(audit);
        assertEquals(1L, audit.getOwnerUserId());
        assertEquals(105L, audit.getConversationId());
        assertEquals("completed", audit.getStatus());
        assertEquals("tool_query_llm_streamed", audit.getMode());
        assertEquals("streaming", audit.getLlmStatus());
        assertEquals("keyword", audit.getPlanSource());
        assertEquals(1, audit.getToolCount());
        assertTrue(audit.getEventCount() > 0);
        List<AgentRunAuditEventEntity> events = runAuditEvents.stream()
            .filter(event -> "run-envelope".equals(event.getRunId()))
            .toList();
        assertEquals(audit.getEventCount(), events.size());
        assertTrue(events.stream().anyMatch(event -> "run_started".equals(event.getEventType())));
        assertTrue(events.stream().anyMatch(event -> "tool_completed".equals(event.getEventType())
            && event.getPayloadJson().contains("\"tool_name\":\"customer_receivable_lookup\"")));
        assertTrue(events.stream().anyMatch(event -> "answer_delta".equals(event.getEventType())
            && event.getPayloadJson().contains("\"delta_source\":\"model_stream\"")));
    }

    @Test
    void cancelRunMarksActiveStreamCancelledAndEmitsRunCancelledEvent() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
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
    void nonStreamingChatIncludesAuditableAgentRunContract() {
        when(longCatAnthropicClient.isConfigured()).thenReturn(false);
        when(customerRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(1L, 0.0, PageRequest.of(0, 10)))
            .thenReturn(List.of(customer(1L, "客户A", 100.0), customer(2L, "客户B", 80.0)));

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "客户应收情况", false)
        );

        assertEquals("keyword", response.planSource());
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
        when(saleOrderRepository.findByOwnerUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());

        V2AgentDtos.AgentChatResponse response = service.chat(
            new V2AgentDtos.AgentChatRequest(null, "recent sales purchase finance business overview", false)
        );

        assertEquals("keyword", response.planSource());
        assertTrue(response.planSummary().contains("supplier_payable_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("purchase_order_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("finance_record_lookup"), response.planSummary());
        assertTrue(response.planSummary().contains("sales_overview_lookup"), response.planSummary());
        assertEquals(4, response.toolCalls().size());
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "supplier_payable_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "purchase_order_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "finance_record_lookup".equals(tool.toolName())));
        assertTrue(response.toolCalls().stream().anyMatch(tool -> "sales_overview_lookup".equals(tool.toolName())));
    }

    @Test
    void getRunAuditReturnsOwnerScopedSummaryAndEvents() throws Exception {
        when(longCatAnthropicClient.isConfigured()).thenReturn(true);
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
        assertEquals("keyword", response.planSource());
        assertEquals("run-audit-read:audit", response.auditId());
        assertEquals("run-audit-read:trace", response.traceId());
        assertTrue(response.eventCount() > 0);
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

    private static String firstPayload(CapturingEmitter emitter, String marker) {
        return emitter.payloads.stream()
            .filter(payload -> payload.contains(marker))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing payload: " + marker + "\n" + String.join("\n", emitter.payloads)));
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

        @Override
        public void complete() {
            completed = true;
        }
    }
}

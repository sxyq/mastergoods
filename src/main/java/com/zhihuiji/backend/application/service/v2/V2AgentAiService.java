package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.domain.entity.PayOrderEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.PurchaseOrderEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
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
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.FinalAnswer;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ResponsePayload;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolFailureResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AnswerSynthesizer;
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyDecision;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyGuard;
import com.zhihuiji.backend.application.service.v2.agent.component.SseStreamEmitter;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class V2AgentAiService {
    private static final int CANCELLED_SALE_ORDER_STATUS = 2;
    private static final long DAY_BUCKET_MILLIS = 24L * 60 * 60 * 1000;
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long STREAM_TIMEOUT_MS = 60_000L;
    private static final ZoneId CHART_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final int DEFAULT_TOOL_LIMIT = 10;
    private static final int MAX_AGENT_ITERATIONS = 3;
    private static final int SUPPLIER_SCAN_LIMIT = 50;
    private static final int OVERVIEW_SIGNAL_LIMIT = 5;

    private final CurrentOwnerService currentOwnerService;
    private final AgentConversationRepository agentConversationRepository;
    private final AgentMessageRepository agentMessageRepository;
    private final AgentDraftRepository agentDraftRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentNotificationRepository agentNotificationRepository;
    private final AgentRunAuditRepository agentRunAuditRepository;
    private final AgentRunAuditEventRepository agentRunAuditEventRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PayOrderRepository payOrderRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final LongCatAnthropicClient longCatAnthropicClient;
    private final ToolRegistry toolRegistry;
    private final RunAuditService runAuditService;
    private final SseStreamEmitter sseStreamEmitter;
    private final SafetyGuard safetyGuard;
    private final ToolPlanner toolPlanner;
    private final AnswerSynthesizer answerSynthesizer;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public V2AgentAiService(
        CurrentOwnerService currentOwnerService,
        AgentConversationRepository agentConversationRepository,
        AgentMessageRepository agentMessageRepository,
        AgentDraftRepository agentDraftRepository,
        AgentTaskRepository agentTaskRepository,
        AgentNotificationRepository agentNotificationRepository,
        AgentRunAuditRepository agentRunAuditRepository,
        AgentRunAuditEventRepository agentRunAuditEventRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        SaleOrderRepository saleOrderRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PayOrderRepository payOrderRepository,
        FinanceRecordRepository financeRecordRepository,
        PaymentRepository paymentRepository,
        ObjectMapper objectMapper,
        LongCatAnthropicClient longCatAnthropicClient,
        ToolRegistry toolRegistry,
        RunAuditService runAuditService,
        SseStreamEmitter sseStreamEmitter,
        SafetyGuard safetyGuard,
        ToolPlanner toolPlanner,
        AnswerSynthesizer answerSynthesizer
    ) {
        this.currentOwnerService = currentOwnerService;
        this.agentConversationRepository = agentConversationRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.agentDraftRepository = agentDraftRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentNotificationRepository = agentNotificationRepository;
        this.agentRunAuditRepository = agentRunAuditRepository;
        this.agentRunAuditEventRepository = agentRunAuditEventRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.payOrderRepository = payOrderRepository;
        this.financeRecordRepository = financeRecordRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.toolRegistry = toolRegistry;
        this.runAuditService = runAuditService;
        this.sseStreamEmitter = sseStreamEmitter;
        this.safetyGuard = safetyGuard;
        this.toolPlanner = toolPlanner;
        this.answerSynthesizer = answerSynthesizer;
    }

    @PreDestroy
    public void shutdownStreamExecutor() {
        streamExecutor.shutdownNow();
    }

    @Transactional(readOnly = true)
    public V2AgentDtos.AgentWorkbenchResponse getWorkbench() {
        currentOwnerService.requireCurrentOwnerUserId();

        return new V2AgentDtos.AgentWorkbenchResponse(
            "你好，我是智慧记 AI 助手",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            "clean_entry_ready",
            "AI 首页不预取或展示报表型经营数据；发送问题后才创建真实 owner-scoped run 并查询业务工具。",
            List.of(
                new V2AgentDtos.WorkbenchCapabilityItem(
                    "real_data_chat",
                    "真实数据问答",
                    "按用户问题创建服务端 run，查询当前账号可访问的库存、往来、销售、采购和财务数据。"
                ),
                new V2AgentDtos.WorkbenchCapabilityItem(
                    "markdown_and_result_blocks",
                    "Markdown 与结构化结果",
                    "回答可包含 Markdown、表格、图表和证据卡；Android 只渲染后端返回的真实结果块。"
                ),
                new V2AgentDtos.WorkbenchCapabilityItem(
                    "auditable_agent_trace",
                    "可审计运行轨迹",
                    "每次运行记录工具、模式、模型状态、耗时、audit id 和 trace id，便于后续核对。"
                )
            ),
            List.of("当前入口不返回默认 KPI、风险、今日摘要或报表图表，避免与报表页重复或产生模拟数据。")
        );
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentTaskResponse> listTasks() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return agentTaskRepository.findTop20ByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
            .map(this::toTaskResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentNotificationResponse> listNotifications(boolean unreadOnly) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentNotificationEntity> rows = unreadOnly
            ? agentNotificationRepository.findTop30ByOwnerUserIdAndIsReadFalseOrderByCreatedAtDesc(ownerUserId)
            : agentNotificationRepository.findTop30ByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        return rows.stream()
            .map(this::toNotificationResponse)
            .toList();
    }

    @Transactional
    public V2AgentDtos.AgentNotificationResponse markNotificationRead(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentNotificationEntity entity = agentNotificationRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("agent notification 不存在"));
        entity.setIsRead(true);
        return toNotificationResponse(agentNotificationRepository.save(entity));
    }

    @Transactional
    public V2AgentDtos.AgentChatResponse chat(V2AgentDtos.AgentChatRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        long runStartedAt = now;
        String message = normalizeRequired(request.message(), "message 不能为空");
        AgentConversationEntity conversation = resolveConversation(request.conversationId(), ownerUserId, message, now);
        saveMessage(ownerUserId, conversation.getId(), "user", "text", message, null, now);

        String runId = UUID.randomUUID().toString();
        runAuditService.createRunAudit(ownerUserId, conversation.getId(), runId, runStartedAt);
        SafetyDecision safetyDecision = safetyGuard.evaluateSafety(message);
        String auditId = RunAuditService.auditIdFor(runId);
        String traceId = RunAuditService.traceIdFor(runId);
        if (!safetyDecision.passed()) {
            String blockedAnswer = "这个请求涉及越权或高风险操作，我不能直接执行。你可以改成只查询当前账号下的合规数据范围。";
            persistAssistantResponse(ownerUserId, conversation, blockedAnswer, List.of(), now);
            long blockedCompletedAt = System.currentTimeMillis();
            runAuditService.finishRunAudit(
                ownerUserId,
                runId,
                "blocked",
                "blocked",
                "not_requested",
                "safety",
                0,
                null,
                null,
                blockedCompletedAt
            );
            return new V2AgentDtos.AgentChatResponse(
                runId,
                conversation.getId(),
                blockedAnswer,
                List.of(),
                null,
                false,
                safetyDecision.reason(),
                "blocked",
                "not_requested",
                "safety",
                "安全检查拦截：未执行业务查询工具",
                List.of(),
                List.of(),
                List.of(),
                new V2AgentDtos.AgentPerformanceSummaryDto(
                    runStartedAt,
                    blockedCompletedAt,
                    Math.max(0L, blockedCompletedAt - runStartedAt),
                    0L,
                    0L
                ),
                auditId,
                traceId,
                SseStreamEmitter.observabilityFor(runId, auditId, traceId)
            );
        }

        List<AgentMessageEntity> history = loadRecentHistory(ownerUserId, conversation.getId(), 10);
        ResponsePayload payload = buildResponse(ownerUserId, message, history, conversation.getLatestSummary(), null, runId);
        long modelStartedAt = System.currentTimeMillis();
        FinalAnswer finalAnswer = buildFinalAnswer(message, payload, history, conversation.getLatestSummary());
        long completedAt = System.currentTimeMillis();
        long modelDurationMs = finalAnswer.modelAttempted()
            ? Math.max(0L, completedAt - modelStartedAt)
            : 0L;
        persistAssistantResponse(ownerUserId, conversation, finalAnswer.answer(), payload.blocks(), System.currentTimeMillis());
        runAuditService.finishRunAudit(
            ownerUserId,
            runId,
            "completed",
            finalAnswer.mode(),
            finalAnswer.llmStatus(),
            payload.planSource(),
            payload.toolResults().size(),
            null,
            null,
            completedAt
        );

        return new V2AgentDtos.AgentChatResponse(
            runId,
            conversation.getId(),
            finalAnswer.answer(),
            payload.blocks(),
            null,
            true,
            null,
            finalAnswer.mode(),
            finalAnswer.llmStatus(),
            payload.planSource(),
            payload.planSummary(),
            toToolCallDtos(runId, payload),
            toEvidenceRefs(runId, payload),
            payload.blocks(),
            new V2AgentDtos.AgentPerformanceSummaryDto(
                runStartedAt,
                completedAt,
                Math.max(0L, completedAt - runStartedAt),
                payload.toolDurationMs(),
                modelDurationMs
            ),
            auditId,
            traceId,
            SseStreamEmitter.observabilityFor(runId, auditId, traceId)
        );
    }

    public SseEmitter chatStream(V2AgentDtos.AgentChatRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        String message = normalizeRequired(request.message(), "message 不能为空");
        AgentConversationEntity conversation = resolveConversation(request.conversationId(), ownerUserId, message, now);
        saveMessage(ownerUserId, conversation.getId(), "user", "text", message, null, now);
        String runId = UUID.randomUUID().toString();
        runAuditService.createRunAudit(ownerUserId, conversation.getId(), runId, now);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        RunAuditService.ActiveAgentRun activeRun = new RunAuditService.ActiveAgentRun(ownerUserId, runId, conversation.getId(), emitter);
        runAuditService.registerRun(activeRun);
        emitter.onCompletion(() -> runAuditService.removeRun(runId));
        emitter.onTimeout(() -> runAuditService.removeRun(runId));
        emitter.onError(ignored -> runAuditService.removeRun(runId));
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                runChatStream(ownerUserId, conversation, message, runId, emitter);
            } catch (RunAuditService.AgentRunCancelledException ignored) {
                // cancelRun 已向客户端发送 run_cancelled；worker 负责收尾关闭 emitter。
                activeRun.complete();
            } catch (Exception ex) {
                runAuditService.finishRunAudit(
                    ownerUserId,
                    runId,
                    "failed",
                    null,
                    null,
                    null,
                    null,
                    "STREAM_ERROR",
                    ex.getMessage() != null ? ex.getMessage() : "stream failed",
                    System.currentTimeMillis()
                );
                try {
                    sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("error", Map.of(
                        "run_id", runId,
                        "code", "STREAM_ERROR",
                        "message", ex.getMessage() != null ? ex.getMessage() : "stream failed",
                        "timestamp", System.currentTimeMillis()
                    )));
                } catch (IOException ignored) {
                    // ignore
                }
                emitter.completeWithError(ex);
            } finally {
                runAuditService.removeRun(runId);
            }
        }, streamExecutor);
        activeRun.attachFuture(future);
        return emitter;
    }

    public V2AgentDtos.AgentRunCancelResponse cancelRun(String runId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedRunId = normalizeRequired(runId, "run_id 不能为空");
        RunAuditService.ActiveAgentRun activeRun = runAuditService.getActiveRun(normalizedRunId);
        if (activeRun == null || !activeRun.ownerUserId().equals(ownerUserId)) {
            return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "not_found", false);
        }
        if (activeRun.cancelled()) {
            return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "already_cancelled", true);
        }
        activeRun.cancel();
        longCatAnthropicClient.cancelStream(normalizedRunId);
        sseStreamEmitter.emitRunCancelled(activeRun.emitter(), normalizedRunId, "用户已停止生成");
        runAuditService.finishRunAudit(
            ownerUserId,
            normalizedRunId,
            "cancelled",
            "cancelled",
            "cancelled",
            null,
            null,
            null,
            "用户已停止生成",
            System.currentTimeMillis()
        );
        if (activeRun.cancelFutureIfNotStarted()) {
            activeRun.complete();
            runAuditService.removeRun(normalizedRunId);
        }
        return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "cancelled", true);
    }

    public V2AgentDtos.AgentRunAuditResponse getRunAudit(String runId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedRunId = normalizeRequired(runId, "run_id 不能为空");
        runAuditService.awaitRunAuditEvents(normalizedRunId);
        AgentRunAuditEntity audit = agentRunAuditRepository.findByRunIdAndOwnerUserId(normalizedRunId, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("run audit not found"));
        List<V2AgentDtos.AgentRunAuditEventResponse> events = agentRunAuditEventRepository
            .findAllByRunIdOrderBySeqAsc(normalizedRunId)
            .stream()
            .map(this::toRunAuditEventResponse)
            .toList();
        return new V2AgentDtos.AgentRunAuditResponse(
            audit.getRunId(),
            audit.getOwnerUserId(),
            audit.getConversationId(),
            audit.getStatus(),
            audit.getMode(),
            audit.getLlmStatus(),
            audit.getPlanSource(),
            audit.getToolCount(),
            audit.getEventCount(),
            audit.getAuditWriteDroppedCount(),
            audit.getAuditWriteFailedCount(),
            Boolean.TRUE.equals(audit.getAuditLossy()),
            audit.getEmittedEventCount(),
            runAuditService.auditWarnings(audit),
            audit.getAuditId(),
            audit.getTraceId(),
            audit.getErrorCode(),
            audit.getErrorMessage(),
            audit.getStartedAt(),
            audit.getCompletedAt(),
            audit.getUpdatedAt(),
            events
        );
    }

    protected void runChatStream(
        Long ownerUserId,
        AgentConversationEntity conversation,
        String message,
        String runId,
        SseEmitter emitter
    ) throws IOException {
        boolean registeredForDirectRun = runAuditService.registerRunIfAbsent(
            new RunAuditService.ActiveAgentRun(ownerUserId, runId, conversation.getId(), emitter)
        );
        runAuditService.ensureRunAuditStarted(ownerUserId, conversation.getId(), runId, System.currentTimeMillis());
        try {
            String auditId = RunAuditService.auditIdFor(runId);
            String traceId = RunAuditService.traceIdFor(runId);
            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("run_started", Map.of(
                "run_id", runId,
                "conversation_id", conversation.getId(),
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", SseStreamEmitter.observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
            runAuditService.ensureRunActive(runId);

            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("safety_check_started", Map.of(
                "run_id", runId,
                "timestamp", System.currentTimeMillis()
            )));
            runAuditService.ensureRunActive(runId);

            SafetyDecision safetyDecision = safetyGuard.evaluateSafety(message);
            if (!safetyDecision.passed()) {
                sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("safety_check_blocked", mapOf(
                    "run_id", runId,
                    "reason", safetyDecision.reason(),
                    "suggested_action", "改成仅查询当前登录账号可见的数据",
                    "timestamp", System.currentTimeMillis()
                )));
                String blockedAnswer = "这个请求涉及越权或高风险操作，我不能直接执行。";
                sseStreamEmitter.emitAnswerCompleted(emitter, runId, blockedAnswer, "blocked", "not_requested", "safety");
                persistAssistantResponse(ownerUserId, conversation, blockedAnswer, List.of(), System.currentTimeMillis());
                sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("run_completed", mapOf(
                    "run_id", runId,
                    "final_answer", blockedAnswer,
                    "mode", "blocked",
                    "llm_status", "not_requested",
                    "plan_source", "safety",
                    "audit_id", auditId,
                    "trace_id", traceId,
                    "observability", SseStreamEmitter.observabilityFor(runId, auditId, traceId),
                    "timestamp", System.currentTimeMillis()
                )));
                runAuditService.finishRunAudit(
                    ownerUserId,
                    runId,
                    "blocked",
                    "blocked",
                    "not_requested",
                    "safety",
                    0,
                    null,
                    null,
                    System.currentTimeMillis()
                );
                emitter.complete();
                return;
            }

            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("safety_check_passed", Map.of(
                "run_id", runId,
                "timestamp", System.currentTimeMillis()
            )));
            runAuditService.ensureRunActive(runId);
            List<AgentMessageEntity> history = loadRecentHistory(ownerUserId, conversation.getId(), 10);
            ResponsePayload payload = buildResponse(ownerUserId, message, history, conversation.getLatestSummary(), emitter, runId);
            runAuditService.ensureRunActive(runId);
            AtomicBoolean blocksEmitted = new AtomicBoolean(false);
            Runnable emitBlocksAfterVisibleAnswer = () -> {
                if (blocksEmitted.compareAndSet(false, true)) {
                    sseStreamEmitter.emitBlocks(emitter, runId, payload.blocks());
                    runAuditService.ensureRunActive(runId);
                }
            };
            FinalAnswer finalAnswer = buildFinalAnswerForStream(
                message,
                payload,
                emitter,
                runId,
                emitBlocksAfterVisibleAnswer,
                history,
                conversation.getLatestSummary()
            );
            runAuditService.ensureRunActive(runId);
            sseStreamEmitter.emitAnswerCompleted(
                emitter,
                runId,
                finalAnswer.answer(),
                finalAnswer.mode(),
                finalAnswer.llmStatus(),
                payload.planSource()
            );
            emitBlocksAfterVisibleAnswer.run();
            persistAssistantResponse(ownerUserId, conversation, finalAnswer.answer(), payload.blocks(), System.currentTimeMillis());
            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("run_completed", mapOf(
                "run_id", runId,
                "final_answer", finalAnswer.answer(),
                "mode", finalAnswer.mode(),
                "llm_status", finalAnswer.llmStatus(),
                "plan_source", payload.planSource(),
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", SseStreamEmitter.observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
            runAuditService.finishRunAudit(
                ownerUserId,
                runId,
                "completed",
                finalAnswer.mode(),
                finalAnswer.llmStatus(),
                payload.planSource(),
                payload.toolResults().size(),
                null,
                null,
                System.currentTimeMillis()
            );
            emitter.complete();
        } finally {
            if (registeredForDirectRun) {
                runAuditService.removeRun(runId);
            }
        }
    }

    private ResponsePayload buildResponse(Long ownerUserId, String message) {
        return buildResponse(ownerUserId, message, List.of(), null, null, null);
    }

    private ResponsePayload buildResponse(Long ownerUserId, String message, SseEmitter emitter, String runId) {
        return buildResponse(ownerUserId, message, List.of(), null, emitter, runId);
    }

    private ResponsePayload buildResponse(
        Long ownerUserId,
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        SseEmitter emitter,
        String runId
    ) {
        AgentToolPlan plan = planTools(message, history, conversationSummary);
        emitPlan(emitter, runId, plan);
        boolean createIntentPlan = containsCreateOnlyTool(plan);

        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        List<ToolExecutionResult> toolResults = new ArrayList<>();
        List<ToolFailureResult> toolFailures = new ArrayList<>();
        executeToolPlan(ownerUserId, emitter, runId, plan, blocks, answers, toolResults, toolFailures);
        if (createIntentPlan) {
            emitDraftCreatedEvents(emitter, runId, toolResults);
        }

        boolean recoveredByDeterministicRetry = false;
        if (!createIntentPlan && hasInsufficientToolResults(toolResults)) {
            Optional<AgentToolPlan> recoveryPlan = buildDeterministicRecoveryPlan(plan, toolResults);
            if (recoveryPlan.isPresent()) {
                AgentToolPlan retryPlan = recoveryPlan.get();
                emitPlan(emitter, runId, retryPlan);
                executeToolPlan(ownerUserId, emitter, runId, retryPlan, blocks, answers, toolResults, toolFailures);
                plan = new AgentToolPlan(
                    mergeTools(plan.tools(), retryPlan.tools()),
                    plan.rationale() + " + 条件放宽补查",
                    "deterministic_recovery",
                    mergeToolParams(plan.toolParams(), retryPlan.toolParams())
                );
                recoveredByDeterministicRetry = true;
            }
        }

        // ReAct 多轮迭代：每轮工具执行后，由 LLM 判断是否需要补充查询，最多 MAX_AGENT_ITERATIONS 轮
        for (int iteration = 2; !createIntentPlan && !recoveredByDeterministicRetry && iteration <= MAX_AGENT_ITERATIONS; iteration++) {
            if (!longCatAnthropicClient.isConfigured()) {
                break;
            }
            Optional<AgentToolPlan> nextPlan = planNextIteration(message, toolResults, iteration);
            if (nextPlan.isEmpty()) {
                break;
            }
            AgentToolPlan iterationPlan = nextPlan.get();
            emitPlan(emitter, runId, iterationPlan);
            executeToolPlan(ownerUserId, emitter, runId, iterationPlan, blocks, answers, toolResults, toolFailures);
            plan = new AgentToolPlan(
                mergeTools(plan.tools(), iterationPlan.tools()),
                plan.rationale() + " + 迭代补充(" + iteration + ")",
                "react_iterated",
                mergeToolParams(plan.toolParams(), iterationPlan.toolParams())
            );
        }

        // 兼容降级：若 LLM 规划的工具全部无结果且未触发迭代，用关键词兜底重试一轮
        if (!createIntentPlan && answers.isEmpty() && "llm".equals(plan.source()) && !plan.tools().isEmpty()) {
            AgentToolPlan fallbackPlan = inferToolPlan(message, history, conversationSummary);
            if (!fallbackPlan.tools().isEmpty() && !fallbackPlan.tools().equals(plan.tools())) {
                emitPlan(emitter, runId, fallbackPlan);
                executeToolPlan(ownerUserId, emitter, runId, fallbackPlan, blocks, answers, toolResults, toolFailures);
                plan = new AgentToolPlan(
                    fallbackPlan.tools(), plan.rationale() + " + 关键词兜底", "llm_with_fallback", plan.toolParams()
                );
            }
        }

        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(toolResults);
        if (!effectiveToolResults.isEmpty()) {
            runAuditService.ensureRunActive(runId);
            V2AgentDtos.ResultBlockDto evidenceBlock = buildEvidenceBlock(runId, effectiveToolResults);
            blocks.add(evidenceBlock);
        }

        if (answers.isEmpty()) {
            answers.add(toolFailures.isEmpty() ? buildUnsupportedIntentAnswer() : buildAllToolsFailedAnswer(toolFailures));
        }

        return new ResponsePayload(String.join("\n", answers), blocks, toolResults, toolFailures, plan);
    }

    private List<ToolExecutionResult> collapseToolResultsForPresentation(List<ToolExecutionResult> toolResults) {
        return answerSynthesizer.collapseToolResultsForPresentation(toolResults);
    }

    private boolean containsCreateOnlyTool(AgentToolPlan plan) {
        if (plan == null || plan.tools() == null || plan.tools().isEmpty()) {
            return false;
        }
        for (String toolName : plan.tools()) {
            Optional<AgentTool> tool = toolRegistry.getTool(toolName);
            if (tool.isPresent() && tool.get().type() == AgentTool.ToolType.CREATE_ONLY) {
                return true;
            }
        }
        return false;
    }

    private void emitDraftCreatedEvents(SseEmitter emitter, String runId, List<ToolExecutionResult> toolResults) {
        if (emitter == null || !StringUtils.hasText(runId) || toolResults == null || toolResults.isEmpty()) {
            return;
        }
        for (ToolExecutionResult toolResult : toolResults) {
            JsonNode facts = toolResult.facts();
            if (facts == null || facts.isMissingNode()) {
                continue;
            }
            long draftId = facts.path("draft_id").asLong(0L);
            if (draftId <= 0L) {
                continue;
            }
            try {
                sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("draft_created", mapOf(
                    "run_id", runId,
                    "draft_id", draftId,
                    "draft_type", facts.path("draft_type").asText("unknown"),
                    "title", facts.path("title").asText(""),
                    "status", facts.path("status").asText("active"),
                    "tool_name", toolResult.toolName(),
                    "summary", toolResult.summary(),
                    "timestamp", System.currentTimeMillis()
                )));
            } catch (IOException ex) {
                throw new IllegalStateException("发送 draft_created 失败", ex);
            }
        }
    }

    /**
     * ReAct 迭代：基于已收集的工具结果，询问 LLM 是否需要补充查询。
     * 委托 {@link ToolPlanner#planNextIteration} 实现。
     */
    private Optional<AgentToolPlan> planNextIteration(String message, List<ToolExecutionResult> toolResults, int iteration) {
        return toolPlanner.planNextIteration(message, toolResults, iteration);
    }

    private boolean hasInsufficientToolResults(List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return false;
        }
        for (ToolExecutionResult result : toolResults) {
            if (result != null && result.insufficient()) {
                return true;
            }
        }
        return false;
    }

    private Optional<AgentToolPlan> buildDeterministicRecoveryPlan(AgentToolPlan currentPlan, List<ToolExecutionResult> toolResults) {
        if (currentPlan == null || currentPlan.tools() == null || currentPlan.tools().isEmpty()
            || toolResults == null || toolResults.isEmpty()) {
            return Optional.empty();
        }
        List<String> tools = new ArrayList<>();
        Map<String, JsonNode> relaxedParams = new LinkedHashMap<>();
        for (ToolExecutionResult result : toolResults) {
            if (result == null || !result.insufficient() || !currentPlan.tools().contains(result.toolName())) {
                continue;
            }
            JsonNode originalParams = currentPlan.toolParams().get(result.toolName());
            JsonNode retryParams = relaxToolParams(result.toolName(), originalParams);
            if (retryParams == null) {
                continue;
            }
            if (originalParams != null && originalParams.equals(retryParams)) {
                continue;
            }
            if (!tools.contains(result.toolName())) {
                tools.add(result.toolName());
                relaxedParams.put(result.toolName(), retryParams);
            }
        }
        if (tools.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AgentToolPlan(
            tools,
            "首轮精确条件未命中，自动放宽筛选再补查一轮",
            "deterministic_recovery",
            relaxedParams
        ));
    }

    private JsonNode relaxToolParams(String toolName, JsonNode originalParams) {
        if (originalParams == null || !originalParams.isObject()) {
            return null;
        }
        ObjectNode relaxed = objectMapper.createObjectNode();
        switch (toolName) {
            case "sale_order_lookup" -> {
                copyTextParam(originalParams, relaxed, "keyword");
                if (!relaxed.has("keyword")) {
                    copyTextParam(originalParams, relaxed, "product_keyword");
                }
            }
            case "purchase_order_lookup", "pay_order_lookup" -> copyTextParam(originalParams, relaxed, "keyword");
            case "finance_record_lookup" -> {
                copyTextParam(originalParams, relaxed, "keyword");
                if (!relaxed.has("keyword") && originalParams.hasNonNull("type")) {
                    relaxed.set("type", originalParams.get("type"));
                }
            }
            default -> {
                return null;
            }
        }
        return relaxed;
    }

    private void copyTextParam(JsonNode source, ObjectNode target, String fieldName) {
        if (source == null || target == null || !StringUtils.hasText(fieldName)) {
            return;
        }
        JsonNode value = source.get(fieldName);
        if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
            target.put(fieldName, value.asText());
        }
    }

    private List<String> mergeTools(List<String> existing, List<String> additional) {
        List<String> merged = new ArrayList<>(existing);
        for (String tool : additional) {
            if (!merged.contains(tool)) {
                merged.add(tool);
            }
        }
        return merged;
    }

    private Map<String, JsonNode> mergeToolParams(Map<String, JsonNode> existing, Map<String, JsonNode> additional) {
        Map<String, JsonNode> merged = new LinkedHashMap<>(existing);
        merged.putAll(additional);
        return merged;
    }

    private void executeToolPlan(Long ownerUserId, SseEmitter emitter, String runId, AgentToolPlan plan,
                                  List<V2AgentDtos.ResultBlockDto> blocks, List<String> answers,
                                  List<ToolExecutionResult> toolResults, List<ToolFailureResult> toolFailures) {
        for (String tool : plan.tools()) {
            Map<String, Object> toolInput = defaultToolInput(plan.toolParams().get(tool));
            SseStreamEmitter.ToolAudit audit = sseStreamEmitter.startToolAudit(emitter, runId, tool, toolInput);
            long startedAt = System.currentTimeMillis();
            try {
                runAuditService.ensureRunActive(runId);
                ResponsePayload payload = executePlannedTool(ownerUserId, emitter, runId, tool, plan.toolParams().get(tool));
                runAuditService.ensureRunActive(runId);
                if (payload != null) {
                    populateToolAudit(audit, payload.toolResults());
                    if (StringUtils.hasText(payload.answer())) {
                        answers.add(payload.answer());
                    }
                    blocks.addAll(payload.blocks());
                    toolResults.addAll(payload.toolResults());
                    sseStreamEmitter.emitToolCompleted(
                        emitter,
                        runId,
                        tool,
                        payload.toolResults().isEmpty() ? "工具执行完成" : payload.toolResults().get(0).summary(),
                        audit
                    );
                }
            } catch (RunAuditService.AgentRunCancelledException ex) {
                throw ex;
            } catch (Exception ex) {
                if (SseStreamEmitter.isStreamEmissionFailure(ex)) {
                    if (ex instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new IllegalStateException("stream emission failed", ex);
                }
                String errorSummary = safeToolErrorSummary(ex);
                toolFailures.add(new ToolFailureResult(tool, errorSummary));
                sseStreamEmitter.emitToolFailed(
                    emitter,
                    runId,
                    tool,
                    errorSummary,
                    System.currentTimeMillis() - startedAt,
                    startedAt,
                    toolInput
                );
            }
        }
    }

    private ResponsePayload executePlannedTool(Long ownerUserId, SseEmitter emitter, String runId, String tool, JsonNode params) {
        Optional<ToolResult> registered = executeRegisteredTool(ownerUserId, emitter, runId, tool, params);
        if (registered.isPresent()) {
            return adaptToolResult(registered.get(), tool);
        }
        return null;
    }

    /**
     * 执行已注册到 {@link ToolRegistry} 的工具。
     *
     * <p>注册表优先策略：新增工具实现 {@link AgentTool} + {@code @Component} 后自动注册，
     * 此方法会优先匹配并执行；未注册的工具返回 {@link Optional#empty()}，由旧 switch 兜底。
     *
     * @param ownerUserId 归属用户 ID
     * @param emitter     SSE 推送器
     * @param runId       运行 ID
     * @param tool        工具名
     * @return 工具执行结果 Optional（未注册时返回 empty）
     */
    private Optional<ToolResult> executeRegisteredTool(Long ownerUserId, SseEmitter emitter, String runId, String tool, JsonNode params) {
        ToolContext ctx = new ToolContext(ownerUserId, null, null, runId, null, objectMapper);
        return toolRegistry.executeTool(tool, ctx, params);
    }

    private Map<String, Object> defaultToolInput(JsonNode params) {
        Map<String, Object> input = paramsToInputMap(params);
        if (input.isEmpty()) {
            return Map.of("limit", 10);
        }
        return input;
    }

    private void populateToolAudit(SseStreamEmitter.ToolAudit audit, List<ToolExecutionResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        Map<String, Object> queryAudit = results.get(0).queryAudit();
        Integer returnedCount = asInteger(queryAudit.get("returned_count"));
        Integer limit = asInteger(queryAudit.get("limit"));
        Boolean isTruncated = asBoolean(queryAudit.get("is_truncated"));
        if (returnedCount != null && limit != null) {
            audit.markLimitedResult(returnedCount, limit, Boolean.TRUE.equals(isTruncated));
        } else if (returnedCount != null) {
            audit.markReturned(returnedCount);
        }
    }

    /**
     * 将 {@link ToolResult} 适配为 {@link ResponsePayload}。
     *
     * @param result   工具执行结果
     * @param toolName 工具名
     * @return 适配后的 ResponsePayload（失败时返回 null）
     */
    private ResponsePayload adaptToolResult(ToolResult result, String toolName) {
        if (!result.success()) {
            return null;
        }
        ToolExecutionResult toolResult = new ToolExecutionResult(toolName, result.toolSummary(), result.toolFacts(), result.insufficient());
        return new ResponsePayload(result.answer(), result.blocks(), List.of(toolResult));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> paramsToInputMap(JsonNode params) {
        if (params == null || !params.isObject()) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(params, Map.class);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private AgentToolPlan planTools(String message, List<AgentMessageEntity> history, String conversationSummary) {
        return toolPlanner.planTools(message, history, conversationSummary);
    }

    private AgentToolPlan inferToolPlan(String message, List<AgentMessageEntity> history, String conversationSummary) {
        return toolPlanner.inferToolPlan(message, history, conversationSummary);
    }

    private List<List<Object>> buildSaleOrderRows(List<SaleOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            SaleOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getCustomerName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getPaidAmount())),
                saleOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSaleOrderSummaries(List<SaleOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            SaleOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "customer_name", safeText(item.getCustomerName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "paid_amount", money(safeDouble(item.getPaidAmount())),
                "status", saleOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private List<List<Object>> buildPurchaseOrderRows(List<PurchaseOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PurchaseOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getTotalAmount())),
                money(safeDouble(item.getPaidAmount())),
                purchaseOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildPurchaseOrderSummaries(List<PurchaseOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PurchaseOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "total_amount", money(safeDouble(item.getTotalAmount())),
                "paid_amount", money(safeDouble(item.getPaidAmount())),
                "status", purchaseOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private List<List<Object>> buildPayOrderRows(List<PayOrderEntity> orders) {
        List<List<Object>> rows = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return rows;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PayOrderEntity item = orders.get(index);
            rows.add(List.of(
                safeText(item.getOrderNo(), "-"),
                safeText(item.getSupplierName(), "-"),
                money(safeDouble(item.getAmount())),
                paymentMethodLabel(item.getMethod()),
                payOrderStatusLabel(item.getStatus())
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildPayOrderSummaries(List<PayOrderEntity> orders) {
        List<Map<String, Object>> items = new ArrayList<>(orders == null ? 0 : orders.size());
        if (orders == null) {
            return items;
        }
        for (int index = 0; index < orders.size(); index += 1) {
            PayOrderEntity item = orders.get(index);
            items.add(mapOf(
                "order_no", safeText(item.getOrderNo(), "-"),
                "supplier_name", safeText(item.getSupplierName(), "-"),
                "amount", money(safeDouble(item.getAmount())),
                "method", paymentMethodLabel(item.getMethod()),
                "status", payOrderStatusLabel(item.getStatus())
            ));
        }
        return items;
    }

    private List<List<Object>> buildFinanceRecordRows(List<FinanceRecordEntity> records) {
        List<List<Object>> rows = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return rows;
        }
        for (int index = 0; index < records.size(); index += 1) {
            FinanceRecordEntity item = records.get(index);
            rows.add(List.of(
                safeText(item.getRecordNo(), "-"),
                financeTypeLabel(item.getType()),
                safeText(item.getCategory(), "-"),
                money(safeDouble(item.getAmount())),
                safeText(item.getPartnerName(), "-")
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildFinanceRecordSummaries(List<FinanceRecordEntity> records) {
        List<Map<String, Object>> items = new ArrayList<>(records == null ? 0 : records.size());
        if (records == null) {
            return items;
        }
        for (int index = 0; index < records.size(); index += 1) {
            FinanceRecordEntity item = records.get(index);
            items.add(mapOf(
                "record_no", safeText(item.getRecordNo(), "-"),
                "type", financeTypeLabel(item.getType()),
                "category", safeText(item.getCategory(), "-"),
                "amount", money(safeDouble(item.getAmount())),
                "partner_name", safeText(item.getPartnerName(), "-")
            ));
        }
        return items;
    }

    private FinalAnswer buildFinalAnswer(String userMessage, ResponsePayload payload,
                                          List<AgentMessageEntity> history, String conversationSummary) {
        return answerSynthesizer.buildFinalAnswer(userMessage, payload, history, conversationSummary);
    }

    private FinalAnswer buildFinalAnswerForStream(
        String userMessage,
        ResponsePayload payload,
        SseEmitter emitter,
        String runId,
        Runnable onFirstModelDelta,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        return answerSynthesizer.buildFinalAnswerForStream(userMessage, payload, emitter, runId, onFirstModelDelta, history, conversationSummary);
    }

    private List<AgentMessageEntity> loadRecentHistory(Long ownerUserId, Long conversationId, int limit) {
        return answerSynthesizer.loadRecentHistory(ownerUserId, conversationId, limit);
    }

    private String buildUnsupportedIntentAnswer() {
        return "这个问题当前版本还没有接入对应的真实查询工具，所以我不会用不相关数据冒充答案。"
            + "你可以改问：低库存商品、商品库存/价格、客户应收、供应商应付、销售单、采购单、付款单、资金流水或近 7 天经营概览。";
    }

    private String buildAllToolsFailedAnswer(List<ToolFailureResult> toolFailures) {
        return "本轮请求匹配到了真实查询工具，但查询过程中失败了。"
            + "我没有使用模拟数据替代，因此不能给出确定结论。"
            + "\n" + answerSynthesizer.appendFailureNotice("", toolFailures);
    }

    private V2AgentDtos.ResultBlockDto buildEvidenceBlock(String runId, List<ToolExecutionResult> toolResults) {
        List<Map<String, Object>> items = new ArrayList<>(toolResults == null ? 0 : toolResults.size() * 4);
        if (toolResults != null) {
            for (ToolExecutionResult result : toolResults) {
                Map<String, Object> audit = result.queryAudit();
                String toolCallId = RunAuditService.toolCallId(runId, result.toolName());
                items.add(mapOf(
                    "label", result.toolName(),
                    "value", result.summary(),
                    "source", "tool:" + result.toolName(),
                    "tool_call_id", toolCallId,
                    "query_window", audit,
                    "is_truncated", Boolean.TRUE.equals(audit.get("is_truncated"))
                ));
                for (Map<String, String> evidenceItem : evidenceItemsFor(result)) {
                    items.add(mapOf(
                        "label", evidenceItem.get("label"),
                        "value", evidenceItem.get("value"),
                        "source", "tool:" + result.toolName(),
                        "tool_call_id", toolCallId,
                        "query_window", audit,
                        "is_truncated", Boolean.TRUE.equals(audit.get("is_truncated"))
                    ));
                }
            }
        }
        return new V2AgentDtos.ResultBlockDto(
            "evidence_card",
            "本次回答依据",
            toJsonNode(mapOf(
                "title", "本次回答依据",
                "items", items
            ))
        );
    }

    private List<V2AgentDtos.AgentToolCallDto> toToolCallDtos(String runId, ResponsePayload payload) {
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        int estimatedSize = effectiveToolResults.size()
            + (payload.toolFailures() == null ? 0 : payload.toolFailures().size());
        List<V2AgentDtos.AgentToolCallDto> calls = new ArrayList<>(estimatedSize);
        for (ToolExecutionResult result : effectiveToolResults) {
            Map<String, Object> audit = result.queryAudit();
            calls.add(new V2AgentDtos.AgentToolCallDto(
                RunAuditService.toolCallId(runId, result.toolName()),
                result.toolName(),
                "completed",
                compactJson(audit.get("tool_input")),
                toJsonNode(audit),
                asInteger(audit.get("returned_count")),
                asInteger(audit.get("total_count")),
                asInteger(audit.get("limit")),
                asBoolean(audit.get("is_truncated")),
                asLong(audit.get("duration_ms")),
                result.summary(),
                null,
                null
            ));
        }
        if (payload.toolFailures() != null) {
            for (ToolFailureResult failure : payload.toolFailures()) {
                calls.add(new V2AgentDtos.AgentToolCallDto(
                    RunAuditService.toolCallId(runId, failure.toolName()),
                    failure.toolName(),
                    "failed",
                    null,
                    toJsonNode(Map.of()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "TOOL_QUERY_FAILED",
                    failure.safeMessage()
                ));
            }
        }
        return calls;
    }

    private List<V2AgentDtos.AgentEvidenceRefDto> toEvidenceRefs(String runId, ResponsePayload payload) {
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        List<V2AgentDtos.AgentEvidenceRefDto> refs = new ArrayList<>(
            effectiveToolResults.size() * 2
        );
        if (effectiveToolResults.isEmpty()) {
            return refs;
        }
        int index = 1;
        for (ToolExecutionResult result : effectiveToolResults) {
            Map<String, Object> audit = result.queryAudit();
            List<Map<String, String>> evidenceItems = evidenceItemsFor(result);
            if (evidenceItems.isEmpty()) {
                refs.add(new V2AgentDtos.AgentEvidenceRefDto(
                    "evidence-" + index++,
                    RunAuditService.toolCallId(runId, result.toolName()),
                    result.toolName(),
                    result.toolName(),
                    result.summary(),
                    toJsonNode(audit),
                    asBoolean(audit.get("is_truncated"))
                ));
                continue;
            }
            for (Map<String, String> item : evidenceItems) {
                refs.add(new V2AgentDtos.AgentEvidenceRefDto(
                    "evidence-" + index++,
                    RunAuditService.toolCallId(runId, result.toolName()),
                    result.toolName(),
                    item.get("label"),
                    item.get("value"),
                    toJsonNode(audit),
                    asBoolean(audit.get("is_truncated"))
                ));
            }
        }
        return refs;
    }

    private List<Map<String, String>> evidenceItemsFor(ToolExecutionResult result) {
        if (result == null || result.facts() == null || result.facts().isMissingNode()) {
            return List.of();
        }
        List<Map<String, String>> items = new ArrayList<>(4);
        switch (result.toolName()) {
            case "inventory_low_stock_lookup" -> addEvidenceItem(items, result, "低库存商品数", "low_stock_count", "个");
            case "product_catalog_lookup" -> {
                addEvidenceItem(items, result, "商品数", "product_count", "个");
                addEvidenceItem(items, result, "库存总计", "stock_total", null);
                addEvidenceItem(items, result, "低库存商品数", "low_stock_count", "个");
            }
            case "inventory_panorama_lookup" -> {
                addEvidenceItem(items, result, "商品名称", "product_name", null);
                addEvidenceItem(items, result, "当前库存", "current_stock", null);
                addEvidenceItem(items, result, "安全库存", "safe_stock", null);
                addEvidenceItem(items, result, "近30天销量", "recent_sales_quantity", null);
                addEvidenceItem(items, result, "周转天数", "turnover_days", null);
                addEvidenceItem(items, result, "建议补货量", "suggested_restock", null);
            }
            case "purchase_tracking_lookup" -> {
                addEvidenceItem(items, result, "采购单号", "order_no", null);
                addEvidenceItem(items, result, "供应商", "supplier_name", null);
                addEvidenceItem(items, result, "采购总额", "total_amount", null);
                addEvidenceItem(items, result, "已到货", "received_amount", null);
                addEvidenceItem(items, result, "待付款", "outstanding_amount", null);
                addEvidenceItem(items, result, "入库单数", "receipt_count", "条");
                addEvidenceItem(items, result, "退货单数", "return_count", "条");
            }
            case "account_health_lookup" -> {
                addEvidenceItem(items, result, "账户总数", "account_count", "个");
                addEvidenceItem(items, result, "账户总余额", "total_balance", null);
                addEvidenceItem(items, result, "收支比", "income_expense_ratio", null);
                addEvidenceItem(items, result, "低余额账户", "low_balance_count", "个");
                addEvidenceItem(items, result, "近期转账", "transfer_count", "条");
                addEvidenceItem(items, result, "默认账户", "default_account_name", null);
            }
            case "customer_receivable_lookup" -> {
                addEvidenceItem(items, result, "欠款客户数", "customer_count", "个");
                addEvidenceItem(items, result, "应收总额", "total_receivable", null);
                addEvidenceItem(items, result, "Top10 应收合计", "top10_receivable_total", null);
            }
            case "customer_profile_lookup" -> {
                addEvidenceItem(items, result, "客户名称", "customer_name", null);
                addEvidenceItem(items, result, "订单数", "order_count", "笔");
                addEvidenceItem(items, result, "累计销售额", "total_sales_amount", null);
                addEvidenceItem(items, result, "当前欠款", "balance", null);
                addEvidenceItem(items, result, "付款习惯", "payment_habit", null);
            }
            case "supplier_payable_lookup" -> {
                addEvidenceItem(items, result, "应付供应商数", "supplier_count", "个");
                addEvidenceItem(items, result, "应付总额", "total_payable", null);
                addEvidenceItem(items, result, "Top10 应付合计", "top10_payable_total", null);
            }
            case "receivable_payable_lookup" -> {
                addEvidenceItem(items, result, "应收客户数", "receivable_customer_count", "个");
                addEvidenceItem(items, result, "应付供应商数", "payable_supplier_count", "个");
                addEvidenceItem(items, result, "应收总额", "total_receivable", null);
                addEvidenceItem(items, result, "应付总额", "total_payable", null);
                addEvidenceItem(items, result, "净敞口", "net_exposure", null);
            }
            case "sales_overview_lookup" -> {
                addEvidenceItem(items, result, "近7天销售额", "sales_amount", null);
                addEvidenceItem(items, result, "近7天回款", "paid_amount", null);
                addEvidenceItem(items, result, "销售单数", "sales_count", "笔");
                addEvidenceItem(items, result, "当前应收", "current_receivable", null);
            }
            case "sale_order_lookup" -> {
                addEvidenceItem(items, result, "销售单数", "order_count", "条");
                addEvidenceItem(items, result, "查询销售额", "recent_total_amount", null);
                addEvidenceItem(items, result, "未收清单数", "unpaid_count", "条");
            }
            case "purchase_order_lookup" -> {
                addEvidenceItem(items, result, "采购单数", "order_count", "条");
                addEvidenceItem(items, result, "查询采购额", "recent_total_amount", null);
                addEvidenceItem(items, result, "查询已到货金额", "recent_received_amount", null);
            }
            case "pay_order_lookup" -> {
                addEvidenceItem(items, result, "付款单数", "pay_order_count", "条");
                addEvidenceItem(items, result, "查询付款额", "recent_total_amount", null);
                addEvidenceItem(items, result, "待付款单数", "pending_count", "条");
            }
            case "finance_record_lookup" -> {
                addEvidenceItem(items, result, "资金流水条数", "record_count", "条");
                addEvidenceItem(items, result, "查询收入", "recent_income", null);
                addEvidenceItem(items, result, "查询支出", "recent_expense", null);
            }
            default -> {
                // Unknown tools fall back to the coarse summary evidence above.
            }
        }
        return items;
    }

    private void addEvidenceItem(
        List<Map<String, String>> items,
        ToolExecutionResult result,
        String label,
        String fieldName,
        String suffix
    ) {
        JsonNode value = result.facts().path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return;
        }
        String text = value.isTextual() || value.isNumber() || value.isBoolean()
            ? value.asText()
            : compactJson(value);
        if (!StringUtils.hasText(text)) {
            return;
        }
        items.add(Map.of(
            "label", label + " (" + fieldName + ")",
            "value", suffix == null ? text : text + suffix
        ));
    }

    private String compactJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String raw = objectMapper.writeValueAsString(value);
            return raw.length() <= 180 ? raw : raw.substring(0, 180) + "...";
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return null;
    }

    private String synthesizeAnswer(String userMessage, List<ToolExecutionResult> toolResults, String fallbackAnswer) {
        return answerSynthesizer.synthesizeAnswer(userMessage, toolResults, fallbackAnswer);
    }

    private AgentConversationEntity resolveConversation(Long conversationId, Long ownerUserId, String message, long now) {
        if (conversationId != null) {
            return agentConversationRepository.findByIdAndOwnerUserId(conversationId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("agent conversation 不存在"));
        }
        AgentConversationEntity conversation = new AgentConversationEntity();
        conversation.setOwnerUserId(ownerUserId);
        conversation.setTitle(buildConversationTitle(message));
        conversation.setStatus("active");
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(null);
        conversation.setLatestSummary(null);
        return agentConversationRepository.save(conversation);
    }

    private void persistAssistantResponse(
        Long ownerUserId,
        AgentConversationEntity conversation,
        String answer,
        List<V2AgentDtos.ResultBlockDto> blocks,
        long now
    ) {
        saveMessage(ownerUserId, conversation.getId(), "assistant", "text", answer, serializeBlocks(blocks), now);
        conversation.setLatestSummary(trimSummary(answer));
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        agentConversationRepository.save(conversation);
    }

    private AgentMessageEntity saveMessage(
        Long ownerUserId,
        Long conversationId,
        String role,
        String messageType,
        String content,
        String structuredDataJson,
        long createdAt
    ) {
        AgentMessageEntity entity = new AgentMessageEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setMessageType(messageType);
        entity.setContent(content);
        entity.setStructuredDataJson(structuredDataJson);
        entity.setCreatedAt(createdAt);
        return agentMessageRepository.save(entity);
    }

    private String serializeBlocks(List<V2AgentDtos.ResultBlockDto> blocks) {
        try {
            return blocks == null || blocks.isEmpty() ? null : objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 agent blocks 失败", e);
        }
    }

    private JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    private void emitPlan(SseEmitter emitter, String runId, AgentToolPlan plan) {
        if (emitter == null || runId == null || plan == null) {
            return;
        }
        try {
            String content = plan.tools().isEmpty()
                ? plan.rationale() + "：当前问题未匹配到已接入的真实查询工具"
                : plan.rationale() + "：" + String.join("、", plan.tools());
            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("plan_delta", mapOf(
                "run_id", runId,
                "plan_source", plan.source(),
                "content", content,
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 plan_delta 失败", ex);
        }
    }

    private V2AgentDtos.AgentRunAuditEventResponse toRunAuditEventResponse(AgentRunAuditEventEntity entity) {
        return new V2AgentDtos.AgentRunAuditEventResponse(
            entity.getEventId(),
            entity.getSeq(),
            entity.getEventType(),
            runAuditService.parseAuditPayload(entity.getPayloadJson()),
            entity.getCreatedAt()
        );
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private String safeToolErrorSummary(Throwable ex) {
        String type = ex == null ? "Exception" : ex.getClass().getSimpleName();
        String rawMessage = ex == null ? null : ex.getMessage();
        String message = StringUtils.hasText(rawMessage) ? redactSensitiveText(rawMessage) : "工具执行失败";
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() > 160) {
            message = message.substring(0, 160) + "...";
        }
        return type + ": " + message;
    }

    private String redactSensitiveText(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replaceAll("(?i)(api[_-]?key|token|secret|password|authorization|bearer)(\\s*[:=]\\s*)[^\\s,;]+", "$1$2***")
            .replaceAll("(?i)(jdbc:)[^\\s]+", "$1***")
            .replaceAll("://([^\\s/@:]+):([^\\s/@]+)@", "://***:***@")
            .replaceAll("\\b[A-Za-z0-9_\\-]{32,}\\b", "***");
    }

    private List<Map<String, Object>> buildRankItems(List<CustomerEntity> customers) {
        List<Map<String, Object>> items = new ArrayList<>(customers == null ? 0 : customers.size());
        if (customers == null) {
            return items;
        }
        for (int index = 0; index < customers.size(); index++) {
            CustomerEntity customer = customers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", customer.getName(),
                "value", money(safeDouble(customer.getBalance())),
                "change_direction", "down"
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildCustomerSalesRank(List<Object[]> rows) {
        List<Map<String, Object>> items = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows == null) {
            return items;
        }
        for (int index = 0; index < rows.size(); index++) {
            Object[] row = rows.get(index);
            String name = row[1] == null ? "未命名客户" : String.valueOf(row[1]);
            double amount = row[3] instanceof Number number ? number.doubleValue() : 0;
            items.add(mapOf(
                "rank", index + 1,
                "name", name,
                "value", money(amount),
                "change_direction", "up"
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildSupplierPayableRank(List<SupplierEntity> suppliers) {
        List<Map<String, Object>> items = new ArrayList<>(suppliers == null ? 0 : suppliers.size());
        if (suppliers == null) {
            return items;
        }
        for (int index = 0; index < suppliers.size(); index++) {
            SupplierEntity supplier = suppliers.get(index);
            items.add(mapOf(
                "rank", index + 1,
                "name", supplier.getName(),
                "value", money(safeDouble(supplier.getBalance())),
                "change_direction", "up"
            ));
        }
        return items;
    }

    private V2AgentDtos.ResultBlockDto buildSalesTrendBlock(Long ownerUserId, long startAt, long endAt) {
        LocalDate endDate = Instant.ofEpochMilli(endAt).atZone(CHART_ZONE).toLocalDate();
        long chartStartAt = endDate.minusDays(6).atStartOfDay(CHART_ZONE).toInstant().toEpochMilli();
        List<Object[]> trendRows = saleOrderRepository.salesTrendBuckets(
            ownerUserId,
            chartStartAt,
            endAt,
            DAY_BUCKET_MILLIS,
            CANCELLED_SALE_ORDER_STATUS
        );
        Map<Long, Object[]> rowsByBucket = new LinkedHashMap<>();
        for (Object[] row : trendRows) {
            rowsByBucket.put(safeLong(row[0]), row);
        }

        List<String> labels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Double> paidData = new ArrayList<>();

        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = endDate.minusDays(offset);
            labels.add(date.format(MONTH_DAY_FORMATTER));
            Object[] row = rowsByBucket.get((long) (6 - offset));
            salesData.add(row == null ? 0D : safeDouble(row[1]));
            paidData.add(row == null ? 0D : safeDouble(row[3]));
        }

        return new V2AgentDtos.ResultBlockDto(
            "line_chart",
            "近7天销售趋势",
            toJsonNode(mapOf(
                "title", "近7天销售趋势",
                "labels", labels,
                "series", List.of(
                    mapOf("name", "销售额", "data", salesData, "color", "#005BBF"),
                    mapOf("name", "回款", "data", paidData, "color", "#34A853")
                )
            ))
        );
    }

    private String compactChartLabel(String value) {
        String text = safeText(value, "-").trim();
        return text.length() <= 8 ? text : text.substring(0, 8) + "...";
    }

    private String buildConversationTitle(String message) {
        String normalized = message.trim();
        if (normalized.length() <= 24) {
            return normalized;
        }
        return normalized.substring(0, 24);
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String trimSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private <T> List<T> limit(List<T> items, int size) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.size() <= size ? items : items.subList(0, size);
    }

    private double sumCustomerBalances(List<CustomerEntity> customers) {
        double total = 0D;
        if (customers == null) {
            return total;
        }
        for (CustomerEntity customer : customers) {
            total += safeDouble(customer.getBalance());
        }
        return total;
    }

    private double sumSupplierBalances(List<SupplierEntity> suppliers) {
        double total = 0D;
        if (suppliers == null) {
            return total;
        }
        for (SupplierEntity supplier : suppliers) {
            total += safeDouble(supplier.getBalance());
        }
        return total;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private long safeLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private int countMessages(Long ownerUserId, Long conversationId) {
        long count = agentMessageRepository.countByOwnerUserIdAndConversationId(ownerUserId, conversationId);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private V2AgentDtos.AgentTaskResponse toTaskResponse(AgentTaskEntity entity) {
        return new V2AgentDtos.AgentTaskResponse(
            entity.getId(),
            entity.getTaskType(),
            entity.getTitle(),
            entity.getTriggerSource(),
            entity.getStatus(),
            taskStatusLabel(entity.getStatus()),
            entity.getProgress(),
            entity.getInputText(),
            entity.getResultJson(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCompletedAt()
        );
    }

    private V2AgentDtos.AgentNotificationResponse toNotificationResponse(AgentNotificationEntity entity) {
        return new V2AgentDtos.AgentNotificationResponse(
            entity.getId(),
            entity.getTaskId(),
            entity.getTitle(),
            entity.getBody(),
            entity.getLevel(),
            entity.getIsRead(),
            entity.getIsDelivered(),
            entity.getCreatedAt()
        );
    }

    private String taskStatusLabel(String status) {
        return switch (status == null ? "" : status.toLowerCase(Locale.ROOT)) {
            case "queued", "pending" -> "排队中";
            case "running", "processing" -> "执行中";
            case "success", "succeeded", "completed" -> "已完成";
            case "failed", "error" -> "失败";
            case "cancelled", "canceled" -> "已取消";
            default -> safeText(status, "未知");
        };
    }

    private String money(double value) {
        return String.format(Locale.US, "¥%.2f", value);
    }

    private String formatNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001
            ? String.valueOf((long) Math.rint(value))
            : String.format(Locale.US, "%.2f", value);
    }

    private String saleOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已完成";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String purchaseOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已下单";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private String payOrderStatusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已取消";
            default -> "未知";
        };
    }

    private String paymentMethodLabel(Integer method) {
        return switch (method == null ? -1 : method) {
            case 0 -> "现金";
            case 1 -> "微信";
            case 2 -> "支付宝";
            case 3 -> "银行卡";
            default -> "其他";
        };
    }

    private String financeTypeLabel(Integer type) {
        return switch (type == null ? -1 : type) {
            case 1 -> "收入";
            case 2 -> "支出";
            default -> "未知";
        };
    }
}

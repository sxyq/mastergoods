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
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyDecision;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyGuard;
import com.zhihuiji.backend.application.service.v2.agent.component.SseStreamEmitter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String RULE_SUMMARY_NOTICE = "说明：本回答为真实数据查询后的规则摘要，当前未使用模型生成。";

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
        SafetyGuard safetyGuard
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
        if (toolResults == null || toolResults.isEmpty()) {
            return List.of();
        }
        Map<String, ToolExecutionResult> collapsed = new LinkedHashMap<>();
        for (ToolExecutionResult result : toolResults) {
            if (result == null) {
                continue;
            }
            collapsed.remove(result.toolName());
            collapsed.put(result.toolName(), result);
        }
        return new ArrayList<>(collapsed.values());
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
     *
     * <p>将用户问题与已执行工具的摘要拼成提示，由 LLM 决定是否追加工具调用。
     * 终止条件（返回 empty）：
     * <ul>
     *   <li>达到 {@link #MAX_AGENT_ITERATIONS} 上限</li>
     *   <li>LLM 未配置</li>
     *   <li>已有工具结果且 LLM 判断无需继续</li>
     *   <li>LLM 返回的 JSON 无有效工具</li>
     * </ul>
     *
     * @param message      用户原始问题
     * @param toolResults  已收集的工具结果
     * @param iteration    当前迭代轮次（从 2 开始）
     * @return 下一轮工具规划 Optional；无需继续时为 empty
     */
    private Optional<AgentToolPlan> planNextIteration(String message, List<ToolExecutionResult> toolResults, int iteration) {
        if (iteration > MAX_AGENT_ITERATIONS || !longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        String toolCatalog = toolRegistry.buildToolCatalogForLlm();
        if (toolCatalog.isBlank()) {
            return Optional.empty();
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (ToolExecutionResult result : toolResults) {
            contextBuilder.append("- ").append(result.toolName()).append("：").append(result.summary()).append('\n');
        }
        String executedContext = contextBuilder.length() > 0
            ? "已执行工具及结果摘要：\n" + contextBuilder
            : "上一轮工具未返回有效结果。\n";
        String systemPrompt = "你是智慧记的工具规划器。根据用户问题与已查询结果，判断是否需要补充查询其他工具。\n"
            + "可选工具：\n"
            + toolCatalog
            + "若已收集到足够信息回答用户问题，输出 {\"tools\":[]}。\n"
            + "若需要补充查询，输出 {\"tools\":[{\"name\":\"...\",\"params\":{...}}],\"rationale\":\"...\"}，最多 2 个补充工具。\n"
            + "只输出 JSON，不要输出 Markdown。";
        String userPrompt = "用户问题：" + message + "\n"
            + executedContext
            + "请判断是否需要补充查询。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt).flatMap(this::parseToolPlan);
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
        return planToolsWithLlm(message, history, conversationSummary)
            .orElseGet(() -> inferToolPlan(message, history, conversationSummary));
    }

    private Optional<AgentToolPlan> planToolsWithLlm(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (!longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        // 优先尝试原生 Function Calling（Anthropic Messages API 的 tool_use block）
        // 不支持或模型未返回 tool_use 时降级到 JSON 字符串解析路径
        Optional<AgentToolPlan> nativePlan = planToolsWithNativeFunctionCalling(message, history, conversationSummary);
        if (nativePlan.isPresent()) {
            return nativePlan;
        }
        // 降级路径：prompt + JSON 解析（兼容 Chat Completions / Responses API 及不支持 tool_use 的模型）
        // 工具清单优先从注册表动态生成；注册表为空时降级为旧硬编码白名单（渐进式迁移兼容）
        String toolCatalog = toolRegistry.buildToolCatalogForLlm();
        String systemPrompt;
        if (toolCatalog.isBlank()) {
            systemPrompt = """
                你是智慧记的工具规划器。你只能选择白名单中的只读数据查询工具，不允许生成 SQL，不允许访问其他账号数据，不允许执行写操作。
                可选工具：
                - inventory_low_stock_lookup：查询当前账号低库存、补货、缺货相关数据
                - customer_receivable_lookup：查询当前账号客户欠款、应收、回款优先级
                - sales_overview_lookup：查询当前账号近7天销售、回款、经营概览
                - product_catalog_lookup：查询当前账号商品、库存、价格、类别相关数据
                - supplier_payable_lookup：查询当前账号供应商欠款、应付与采购相关数据
                - sale_order_lookup：查询当前账号销售单、客户订单与收款情况
                - purchase_order_lookup：查询当前账号采购单、供应商采购与到货情况
                - pay_order_lookup：查询当前账号付款单、付款状态与金额
                - finance_record_lookup：查询当前账号收入支出流水、分类与近期开支
                只输出 JSON，不要输出 Markdown。
                """;
        } else {
            systemPrompt = "你是智慧记的工具规划器。你可以选择以下只读查询工具和创建类工具。\n"
                + "只读工具直接返回查询结果；创建类工具会生成草稿，需用户确认后才执行写入，不会直接修改数据。\n"
                + "不允许生成 SQL，不允许访问其他账号数据。\n"
                + "可选工具：\n"
                + toolCatalog
                + "只输出 JSON，不要输出 Markdown。\n";
        }
        String historyContext = formatHistoryContext(history);
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "会话摘要：" + conversationSummary + "\n"
            : "";
        String userPrompt = historyContext
            + summaryContext
            + "用户问题：" + message + "\n"
            + "请输出形如 {\"tools\":[{\"name\":\"sale_order_lookup\",\"params\":{\"keyword\":\"张三\"}}],\"rationale\":\"...\"} 的 JSON。"
            + "tools 最多 3 个，必须来自可选工具。params 为该工具的查询参数，根据用户问题提取，无参数时省略 params 字段。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt).flatMap(this::parseToolPlan);
    }

    /**
     * 原生 Function Calling 规划路径。
     *
     * <p>从 {@link ToolRegistry} 构建原生 {@code ToolDefinition} 列表，调用
     * {@link LongCatAnthropicClient#createMessageWithTools}；模型返回 {@code tool_use}
     * block 时直接转为 {@link AgentToolPlan}，无需正则提取 JSON。
     *
     * <p>仅 Anthropic Messages API 支持此路径；其他 wireApi 或模型未返回 tool_use 时返回 empty，
     * 由 {@link #planToolsWithLlm} 降级到 JSON 字符串解析。
     *
     * @param message 用户问题
     * @return 工具规划 Optional；无 tool_use 返回时为 empty
     */
    private Optional<AgentToolPlan> planToolsWithNativeFunctionCalling(
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = buildNativeToolDefinitions();
        if (nativeTools.isEmpty()) {
            return Optional.empty();
        }
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n当前会话摘要：" + conversationSummary
            : "";
        String systemPrompt = "你是智慧记的工具规划器。根据用户问题选择最相关的只读查询工具或创建类工具。\n"
            + "只读工具直接返回查询结果；创建类工具生成草稿，需用户确认后才执行写入。\n"
            + "不允许生成 SQL，不允许访问其他账号数据。最多选择 3 个工具。"
            + summaryContext;
        String planningMessage = formatHistoryContext(history) + "用户问题：" + message;
        Optional<LongCatAnthropicClient.ToolUseResponse> response =
            longCatAnthropicClient.createMessageWithTools(systemPrompt, planningMessage, nativeTools);
        if (response.isEmpty() || !response.get().hasToolUses()) {
            return Optional.empty();
        }
        List<String> tools = new ArrayList<>();
        Map<String, JsonNode> toolParams = new LinkedHashMap<>();
        for (LongCatAnthropicClient.ToolUseBlock toolUse : response.get().toolUses()) {
            String toolName = toolUse.name();
            if (!isAllowedTool(toolName) || tools.size() >= 3) {
                continue;
            }
            if (tools.contains(toolName)) {
                continue;
            }
            tools.add(toolName);
            JsonNode input = toolUse.input();
            if (input != null && input.isObject() && input.size() > 0) {
                toolParams.put(toolName, input);
            }
        }
        if (tools.isEmpty()) {
            return Optional.empty();
        }
        String rationale = response.get().text() != null && !response.get().text().isBlank()
            ? response.get().text()
            : "模型通过原生 Function Calling 选择工具";
        return Optional.of(new AgentToolPlan(tools, rationale, "native_tool_use", toolParams));
    }

    /**
     * 从 {@link ToolRegistry} 构建原生 Function Calling 的工具定义列表。
     *
     * <p>合并只读工具与创建类工具，将每个工具的 {@link AgentTool#parameterSchema()}（JsonNode）
     * 转为 {@code Map<String, Object>} 作为 {@code input_schema}。
     *
     * @return 工具定义列表；注册表为空时返回空列表
     */
    private List<LongCatAnthropicClient.ToolDefinition> buildNativeToolDefinitions() {
        List<AgentTool> allTools = new ArrayList<>();
        allTools.addAll(toolRegistry.listReadOnlyTools());
        allTools.addAll(toolRegistry.listCreateTools());
        if (allTools.isEmpty()) {
            return List.of();
        }
        List<LongCatAnthropicClient.ToolDefinition> nativeTools = new ArrayList<>(allTools.size());
        for (AgentTool tool : allTools) {
            Map<String, Object> inputSchema = convertSchemaToMap(tool.parameterSchema());
            nativeTools.add(new LongCatAnthropicClient.ToolDefinition(
                tool.name(),
                tool.description(),
                inputSchema
            ));
        }
        return nativeTools;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertSchemaToMap(JsonNode schema) {
        if (schema == null || schema.isNull() || !schema.isObject()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return objectMapper.convertValue(schema, Map.class);
        } catch (Exception ignored) {
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    private Optional<AgentToolPlan> parseToolPlan(String rawText) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawText));
            Set<String> tools = new LinkedHashSet<>();
            Map<String, JsonNode> toolParams = new LinkedHashMap<>();
            JsonNode toolsNode = root.get("tools");
            if (toolsNode != null && toolsNode.isArray()) {
                for (JsonNode item : toolsNode) {
                    String tool;
                    JsonNode params = null;
                    if (item.isObject()) {
                        tool = item.path("name").asText("");
                        params = item.get("params");
                    } else {
                        tool = item.asText("");
                    }
                    if (isAllowedTool(tool)) {
                        tools.add(tool);
                        if (params != null && params.isObject()) {
                            toolParams.put(tool, params);
                        }
                    }
                    if (tools.size() >= 3) {
                        break;
                    }
                }
            }
            if (tools.isEmpty()) {
                return Optional.empty();
            }
            String rationale = root.path("rationale").asText("模型选择了当前问题所需的只读查询工具");
            return Optional.of(new AgentToolPlan(new ArrayList<>(tools), rationale, toolParams));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String extractJsonObject(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawText.substring(start, end + 1);
        }
        return rawText;
    }

    private AgentToolPlan inferToolPlan(String message, List<AgentMessageEntity> history, String conversationSummary) {
        String normalized = message.toLowerCase(Locale.ROOT);
        Optional<AgentToolPlan> createPlan = inferCreateToolPlan(message, normalized, history, conversationSummary);
        if (createPlan.isPresent()) {
            return createPlan.get();
        }
        Set<String> tools = new LinkedHashSet<>();
        if (containsAny(normalized, "智能补货", "补货建议", "restock", "reorder", "replenishment")) {
            tools.add("smart_restock_lookup");
        }
        if (containsAny(normalized, "库存", "补货", "低库存", "缺货", "inventory", "stock", "low stock", "replenish")) {
            tools.add("inventory_low_stock_lookup");
        }
        if (containsAny(normalized, "库存全景", "库存健康", "库存周转", "panorama", "turnover")) {
            tools.add("inventory_panorama_lookup");
        }
        if (containsAny(normalized, "商品", "sku", "品类", "目录", "售价", "进价", "product", "catalog", "price")) {
            tools.add("product_catalog_lookup");
        }
        if (containsAny(normalized, "商品供应商", "供应商关联", "供货关系", "supplier relation", "vendor relation")) {
            tools.add("product_supplier_relation_lookup");
        }
        if (containsAny(normalized, "价格等级", "价目", "price level", "pricing tier")) {
            tools.add("product_price_level_lookup");
        }
        if (containsAny(normalized, "欠款", "应收", "客户", "回款", "receivable", "customer", "collection")) {
            tools.add("customer_receivable_lookup");
        }
        if (containsAny(normalized, "客户画像", "客户分析", "客户档案", "profile", "customer insights")) {
            tools.add("customer_profile_lookup");
        }
        if (containsAny(normalized, "供应商", "应付", "采购", "到货", "收货", "supplier", "payable", "purchase", "procurement")) {
            tools.add("supplier_payable_lookup");
            tools.add("purchase_order_lookup");
        }
        if (containsAny(normalized, "供应商对账", "对账单", "statement", "supplier statement")) {
            tools.add("supplier_statement_lookup");
        }
        if (containsAny(normalized, "采购跟踪", "采购进度", "入库退货", "tracking", "receipt flow")) {
            tools.add("purchase_tracking_lookup");
        }
        if (containsAny(normalized, "销售单", "订单", "成交", "收款", "付款情况", "sale order", "sales order", "order", "deal")) {
            tools.add("sale_order_lookup");
        }
        if (containsAny(normalized, "销售全链路", "销售链路", "退货收款", "full chain", "return flow")) {
            tools.add("sales_full_chain_lookup");
        }
        if (containsAny(normalized, "付款单", "已付款", "待付款", "payment", "paid", "unpaid")) {
            tools.add("pay_order_lookup");
        }
        if (containsAny(normalized, "账户健康", "收支比", "账户概览", "account health", "cash ratio")) {
            tools.add("account_health_lookup");
        }
        if (containsAny(normalized, "流水", "收入", "支出", "财务", "费用", "开支", "finance", "cashflow", "income", "expense")) {
            tools.add("finance_record_lookup");
        }
        if (containsAny(normalized, "交叉分析", "多维分析", "综合分析", "cross analysis", "multi dimension")) {
            tools.add("cross_analysis_lookup");
        }
        if (containsAny(normalized, "异常预警", "异常", "风险预警", "anomaly", "alert")) {
            tools.add("anomaly_alert_lookup");
        }
        if (containsAny(normalized, "应收应付", "往来对账", "对账汇总", "reconciliation", "receivable payable")) {
            tools.add("receivable_payable_lookup");
        }
        if (containsAny(normalized, "经营", "概览", "销售", "最近", "7天", "七天", "business", "overview", "sales", "recent", "7 days")) {
            tools.add("sales_overview_lookup");
        }
        if (containsAny(normalized, "导出", "导出数据", "下载csv", "下载json", "export")) {
            tools.add("data_export_tool");
        }
        if (containsAny(normalized, "门店", "店铺信息", "store info", "shop info")) {
            tools.add("store_info_lookup");
        }
        if (containsAny(normalized, "导入任务", "导入状态", "import job", "import status")) {
            tools.add("import_job_lookup");
        }
        if (containsAny(normalized, "同步状态", "同步任务", "sync status", "sync job")) {
            tools.add("sync_status_lookup");
        }
        List<String> deduplicated = new ArrayList<>();
        for (String tool : tools) {
            if (isAllowedTool(tool)) {
                deduplicated.add(tool);
            }
            if (deduplicated.size() >= 6) {
                break;
            }
        }
        Map<String, JsonNode> toolParams = inferKeywordFallbackToolParams(message, deduplicated, history, conversationSummary);
        return new AgentToolPlan(deduplicated, "根据问题关键词兜底选择已接入工具", "keyword_fallback", toolParams);
    }

    private Map<String, JsonNode> inferKeywordFallbackToolParams(
        String message,
        List<String> tools,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (tools == null || tools.isEmpty()) {
            return Map.of();
        }
        Map<String, JsonNode> toolParams = new LinkedHashMap<>();
        for (String tool : tools) {
            JsonNode params = switch (tool) {
                case "customer_receivable_lookup", "customer_profile_lookup" ->
                    buildCustomerKeywordParams(message, history, conversationSummary);
                case "inventory_panorama_lookup" -> buildProductKeywordParams(message, history, conversationSummary);
                case "purchase_tracking_lookup" -> buildPurchaseKeywordParams(message, history, conversationSummary);
                case "account_health_lookup" -> buildAccountKeywordParams(message, history, conversationSummary);
                default -> null;
            };
            if (params != null && !params.isEmpty()) {
                toolParams.put(tool, params);
            }
        }
        return toolParams;
    }

    private Optional<AgentToolPlan> inferCreateToolPlan(
        String message,
        String normalized,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        if (!looksLikeCreateIntent(normalized)) {
            return Optional.empty();
        }
        if (containsAny(normalized, "客户") && isAllowedTool("create_customer")) {
            JsonNode params = buildCreateCustomerParams(message, history, conversationSummary);
            if (hasTextParam(params, "name")) {
                return Optional.of(createOnlyPlan("create_customer", params, "根据自然语言兜底生成客户草稿"));
            }
        }
        if (containsAny(normalized, "供应商") && isAllowedTool("create_supplier")) {
            JsonNode params = buildCreateSupplierParams(message, history, conversationSummary);
            if (hasTextParam(params, "name")) {
                return Optional.of(createOnlyPlan("create_supplier", params, "根据自然语言兜底生成供应商草稿"));
            }
        }
        if (containsAny(normalized, "商品", "产品", "sku") && isAllowedTool("create_product")) {
            JsonNode params = buildCreateProductParams(message, history, conversationSummary);
            if (hasTextParam(params, "name") && hasTextParam(params, "code")) {
                return Optional.of(createOnlyPlan("create_product", params, "根据自然语言兜底生成商品草稿"));
            }
        }
        if (containsAny(normalized, "付款单", "付款") && isAllowedTool("create_pay_order")) {
            JsonNode params = buildCreatePayOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "supplier_name") && hasNumericParam(params, "amount")) {
                return Optional.of(createOnlyPlan("create_pay_order", params, "根据自然语言兜底生成付款单草稿"));
            }
        }
        if (containsAny(normalized, "流水", "记账", "记一笔", "收入", "支出", "报销", "开支", "费用", "finance", "expense", "income")
            && isAllowedTool("create_finance_record")) {
            JsonNode params = buildCreateFinanceRecordParams(message, normalized, history, conversationSummary);
            if (hasNumericParam(params, "amount") && hasNumericParam(params, "type")) {
                return Optional.of(createOnlyPlan("create_finance_record", params, "根据自然语言兜底生成资金流水草稿"));
            }
        }
        if (containsAny(normalized, "采购单", "采购订单") && isAllowedTool("create_purchase_order")) {
            JsonNode params = buildCreatePurchaseOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "supplier_name")) {
                return Optional.of(createOnlyPlan("create_purchase_order", params, "根据自然语言兜底生成采购单草稿"));
            }
        }
        if (containsAny(normalized, "销售单", "销售订单", "开单", "下单") && isAllowedTool("create_sale_order")) {
            JsonNode params = buildCreateSaleOrderParams(message, history, conversationSummary);
            if (hasTextParam(params, "customer_name")) {
                return Optional.of(createOnlyPlan("create_sale_order", params, "根据自然语言兜底生成销售单草稿"));
            }
        }
        return Optional.empty();
    }

    private AgentToolPlan createOnlyPlan(String toolName, JsonNode params, String rationale) {
        return new AgentToolPlan(
            List.of(toolName),
            rationale,
            "keyword_fallback",
            Map.of(toolName, params)
        );
    }

    private JsonNode buildCreateCustomerParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:客户名称|客户名|姓名|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)客户\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "客户\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            findRecentEntityHint(history, conversationSummary, "customer")
        ));
        putText(params, "phone", extractNamedValue(message, "(1\\d{10})"));
        putText(params, "remark", extractRemark(message));
        return params;
    }

    private JsonNode buildCreateSupplierParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)供应商\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "供应商\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "phone", extractNamedValue(message, "(1\\d{10})"));
        putText(params, "remark", extractRemark(message));
        return params;
    }

    private JsonNode buildCreateProductParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "code", firstNonBlank(
            extractNamedValue(message, "(?:编码|商品编码|code|CODE|sku|SKU)\\s*[:：]?\\s*([A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)商品\\s*([A-Za-z0-9_-]{2,32})\\s+([\\p{IsHan}A-Za-z0-9_-]{2,32})", 1)
        ));
        putText(params, "name", firstNonBlank(
            extractNamedValue(message, "(?:商品名称|产品名称|名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:新建|新增|创建|添加)商品\\s*[A-Za-z0-9_-]{2,32}\\s+([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "(?:商品|产品)\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            findRecentEntityHint(history, conversationSummary, "product")
        ));
        putDouble(params, "price", extractDecimal(message, "(?:售价|销售价|单价|价格)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        putDouble(params, "cost", extractDecimal(message, "(?:成本|成本价|进价|采购价)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        putDouble(params, "stock", extractDecimal(message, "(?:库存|期初库存|数量)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"));
        return params;
    }

    private JsonNode buildAccountKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        String keyword = firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:账户健康|账户概览|收支比)"),
            extractNamedValue(message, "(?:账户|资金账户)\\s*[:：]\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "account")
        );
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            boolean genericOnly = normalizedKeyword.equals("账户")
                || normalizedKeyword.equals("资金账户")
                || normalizedKeyword.equals("账户健康")
                || normalizedKeyword.equals("账户概览")
                || normalizedKeyword.equals("收支比");
            if (!genericOnly
                && !containsAny(normalizedKeyword, "健康", "收支比", "概览")) {
                putText(params, "keyword", keyword);
            }
        }
        Integer windowDays = extractInteger(message, "(\\d{1,3})\\s*(?:天|日)");
        if (windowDays != null) {
            params.put("window_days", Math.max(1, Math.min(180, windowDays)));
        }
        return params;
    }

    private JsonNode buildCreateSaleOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "customer_name", firstNonBlank(
            extractNamedValue(message, "(?:客户名称|客户名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})"),
            extractNamedValue(message, "帮\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            extractNamedValue(message, "为\\s*([\\p{IsHan}A-Za-z0-9_-]{2,32})\\s*(?:开|下|做).{0,8}(?:销售单|销售订单|订单)"),
            findRecentEntityHint(history, conversationSummary, "customer")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreatePurchaseOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "supplier_name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:下|开|做).{0,8}(?:采购单|采购订单)"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:下|开|做).{0,8}(?:采购单|采购订单)"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreatePayOrderParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "supplier_name", firstNonBlank(
            extractNamedValue(message, "(?:供应商名称|供应商名)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:付|付款|打款)"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:付|付款|打款)"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putDouble(params, "amount", firstNonNull(
            extractDecimal(message, "(?:金额|付款金额|支付金额)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"),
            extractDecimal(message, "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块)")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCreateFinanceRecordParams(
        String message,
        String normalized,
        List<AgentMessageEntity> history,
        String conversationSummary
    ) {
        var params = objectMapper.createObjectNode();
        Integer type = inferFinanceRecordType(normalized);
        if (type != null) {
            params.put("type", type);
        }
        putDouble(params, "amount", firstNonNull(
            extractDecimal(message, "(?:金额|支出|收入|报销|费用|开支)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)"),
            extractDecimal(message, "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块)")
        ));
        putText(params, "category", firstNonBlank(
            extractNamedValue(message, "(?:分类|类目)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,32})"),
            extractNamedValue(message, "记一笔\\s*([\\p{IsHan}]{2,16})(?:收入|支出|费用|开支|报销)"),
            extractNamedValue(message, "([\\p{IsHan}]{2,16})(?:收入|支出|费用|开支|报销)")
        ));
        putText(params, "partner_name", firstNonBlank(
            extractNamedValue(message, "(?:往来方|对方|对象)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "给\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:报销|付款|打款|转账)"),
            extractNamedValue(message, "向\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})\\s*(?:收款|收费|付款|打款|转账)"),
            findRecentEntityHint(history, conversationSummary, "customer"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        putText(params, "remark", firstNonBlank(
            extractRemark(message),
            message
        ));
        return params;
    }

    private JsonNode buildCustomerKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        String keyword = firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案)"),
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的催收建议"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案|催收建议|应收|欠款)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:客户画像|客户分析|客户档案|催收建议|应收|欠款)"),
            extractNamedValue(message, "(?:客户画像|客户分析|客户档案)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:客户|客户名|客户名称)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "customer")
        );
        if (StringUtils.hasText(keyword) && !isInvalidContextCarryKeyword(keyword) && !isGenericCustomerKeyword(keyword)) {
            putText(params, "keyword", keyword);
        } else {
            String contextKeyword = findRecentEntityHint(history, conversationSummary, "customer");
            if (StringUtils.hasText(contextKeyword) && !isGenericCustomerKeyword(contextKeyword)) {
                putText(params, "keyword", contextKeyword);
            }
        }
        return params;
    }

    private boolean isGenericCustomerKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (containsAny(normalizedKeyword, "供应商", "应付", "采购")) {
            return true;
        }
        if (normalizedKeyword.endsWith("情况")) {
            String base = normalizedKeyword.substring(0, normalizedKeyword.length() - 2);
            if (base.equals("客户应收")
                || base.equals("客户欠款")
                || base.equals("客户回款")
                || base.equals("应收")
                || base.equals("欠款")
                || base.equals("回款")
                || base.equals("客户画像")
                || base.equals("客户分析")
                || base.equals("客户档案")
                || base.equals("催收建议")) {
                return true;
            }
        }
        return normalizedKeyword.equals("客户")
            || normalizedKeyword.equals("客户名")
            || normalizedKeyword.equals("客户名称")
            || normalizedKeyword.equals("客户画像")
            || normalizedKeyword.equals("客户分析")
            || normalizedKeyword.equals("客户档案")
            || normalizedKeyword.equals("催收建议")
            || normalizedKeyword.equals("应收")
            || normalizedKeyword.equals("欠款")
            || normalizedKeyword.equals("回款")
            || normalizedKeyword.equals("客户应收")
            || normalizedKeyword.equals("客户欠款")
            || normalizedKeyword.equals("客户回款")
            || normalizedKeyword.equals("应收情况")
            || normalizedKeyword.equals("欠款情况")
            || normalizedKeyword.equals("回款情况")
            || normalizedKeyword.equals("客户应收情况")
            || normalizedKeyword.equals("客户欠款情况")
            || normalizedKeyword.equals("客户回款情况");
    }

    private boolean isInvalidContextCarryKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return normalizedKeyword.equals("那个客户")
            || normalizedKeyword.equals("刚才那个客户")
            || normalizedKeyword.equals("这个客户")
            || normalizedKeyword.equals("的欠款")
            || normalizedKeyword.equals("的欠款呢")
            || normalizedKeyword.equals("欠款呢")
            || normalizedKeyword.startsWith("的")
            || normalizedKeyword.startsWith("那个")
            || normalizedKeyword.startsWith("这个");
    }

    private JsonNode buildProductKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "keyword", firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:库存全景|库存健康|库存周转)"),
            extractNamedValue(message, "(?:库存全景|库存健康|库存周转)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:商品|货品|SKU|sku)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "product")
        ));
        return params;
    }

    private JsonNode buildPurchaseKeywordParams(String message, List<AgentMessageEntity> history, String conversationSummary) {
        var params = objectMapper.createObjectNode();
        putText(params, "keyword", firstNonBlank(
            extractNamedValue(message, "看下\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "查看\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "查询\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})的(?:采购跟踪|采购进度|入库退货)"),
            extractNamedValue(message, "(?:采购跟踪|采购进度|入库退货)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            extractNamedValue(message, "(?:采购单|采购订单|供应商)\\s*[:：]?\\s*([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})"),
            findRecentEntityHint(history, conversationSummary, "supplier")
        ));
        return params;
    }

    private Integer inferFinanceRecordType(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (containsAny(normalized, "收入", "收款", "入账", "到账", "income")) {
            return 1;
        }
        if (containsAny(normalized, "支出", "费用", "开支", "报销", "付款", "expense")) {
            return 2;
        }
        return null;
    }

    private boolean looksLikeCreateIntent(String normalized) {
        return containsAny(normalized, "新建", "新增", "创建", "添加", "生成草稿", "建一个", "建个")
            || containsAny(normalized, "记一笔", "记账", "记个", "报销一笔")
            || (containsAny(normalized, "开单", "开一张", "开个", "下一张", "下一笔")
            && containsAny(normalized, "销售单", "销售订单", "采购单", "采购订单", "付款单", "付款"));
    }

    private boolean hasTextParam(JsonNode params, String fieldName) {
        return params != null && StringUtils.hasText(params.path(fieldName).asText(null));
    }

    private boolean hasNumericParam(JsonNode params, String fieldName) {
        return params != null && params.hasNonNull(fieldName) && params.path(fieldName).isNumber();
    }

    private void putText(JsonNode params, String fieldName, String value) {
        if (params instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode && StringUtils.hasText(value)) {
            objectNode.put(fieldName, value.trim());
        }
    }

    private void putDouble(JsonNode params, String fieldName, Double value) {
        if (params instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode && value != null) {
            objectNode.put(fieldName, value);
        }
    }

    private String findRecentEntityHint(List<AgentMessageEntity> history, String conversationSummary, String entityKind) {
        String fromSummary = findEntityHintInText(conversationSummary, entityKind);
        if (StringUtils.hasText(fromSummary)) {
            return fromSummary;
        }
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int index = history.size() - 1; index >= 0; index--) {
            AgentMessageEntity message = history.get(index);
            if (message == null) {
                continue;
            }
            String hint = findEntityHintInText(message.getContent(), entityKind);
            if (StringUtils.hasText(hint)) {
                return hint;
            }
        }
        return null;
    }

    private String findEntityHintInText(String text, String entityKind) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(entityKind)) {
            return null;
        }
        return switch (entityKind) {
            case "customer" -> firstNonBlank(
                extractNamedValue(text, "客户[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:商贸|超市|门店|公司)")
            );
            case "supplier" -> firstNonBlank(
                extractNamedValue(text, "供应商[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:供货|批发|贸易)")
            );
            case "product" -> firstNonBlank(
                extractNamedValue(text, "(?:商品|货品|SKU)[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})(?:库存全景|库存健康|库存周转)")
            );
            case "account" -> firstNonBlank(
                extractNamedValue(text, "(?:账户|资金账户)[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?"),
                extractNamedValue(text, "默认账户[「“\\\"]?([\\p{IsHan}A-Za-z0-9_\\-()（）]{2,40})[」”\\\"]?")
            );
            default -> null;
        };
    }

    private String extractNamedValue(String message, String regex) {
        return extractNamedValue(message, regex, 1);
    }

    private String extractNamedValue(String message, String regex, int group) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return normalizeExtractedText(matcher.group(group));
    }

    private Double extractDecimal(String message, String regex) {
        String text = extractNamedValue(message, regex);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer extractInteger(String message, String regex) {
        String text = extractNamedValue(message, regex);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractRemark(String message) {
        return firstNonBlank(
            extractNamedValue(message, "(?:备注|说明)\\s*[:：]?\\s*([^，。,；;]+)"),
            extractNamedValue(message, "(?:备注|说明)\\s+(.+)$")
        );
    }

    private String normalizeExtractedText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("^[\\s:：,，。;；]+|[\\s,，。;；]+$", "").trim();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... candidates) {
        for (T candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isAllowedTool(String tool) {
        Optional<AgentTool> registered = toolRegistry.getTool(tool);
        if (registered.isPresent()) {
            return registered.get().type() == AgentTool.ToolType.READ_ONLY
                || registered.get().type() == AgentTool.ToolType.CREATE_ONLY;
        }
        // 注册表未命中时回退到内置只读工具白名单（兼容 ToolRegistry 为空的测试场景；
        // 生产环境 ToolRegistry 自动扫描 @Component 工具，不会走到此处）
        return ALLOWED_READONLY_TOOLS.contains(tool);
    }

    /** 内置只读工具白名单，用于注册表为空时的兜底鉴权。 */
    private static final java.util.Set<String> ALLOWED_READONLY_TOOLS = java.util.Set.of(
        "inventory_low_stock_lookup",
        "product_catalog_lookup",
        "customer_receivable_lookup",
        "supplier_payable_lookup",
        "purchase_order_lookup",
        "sale_order_lookup",
        "pay_order_lookup",
        "finance_record_lookup",
        "sales_overview_lookup",
        "sales_return_lookup",
        "purchase_receipt_lookup",
        "purchase_return_lookup",
        "inventory_ledger_lookup",
        "inventory_snapshot_lookup",
        "payment_lookup",
        "account_balance_lookup",
        "account_transfer_lookup",
        "cash_change_lookup",
        "product_category_lookup",
        "partner_group_lookup",
        "partner_contact_lookup",
        "sales_trend_lookup",
        "cashflow_summary_lookup",
        "inventory_adjustment_lookup",
        "report_query",
        "sales_full_chain_lookup",
        "purchase_tracking_lookup",
        "inventory_panorama_lookup",
        "customer_profile_lookup",
        "account_health_lookup",
        "receivable_payable_lookup",
        "supplier_statement_lookup",
        "product_supplier_relation_lookup",
        "product_price_level_lookup",
        "import_job_lookup",
        "sync_status_lookup",
        "smart_restock_lookup",
        "cross_analysis_lookup",
        "anomaly_alert_lookup",
        "data_export_tool",
        "store_info_lookup"
    );


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
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            return new FinalAnswer(appendFailureNotice(payload.answer(), payload.toolFailures()), "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        String synthesized = synthesizeAnswer(userMessage, effectiveToolResults, payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            effectiveToolResults
        );
        if (!longCatAnthropicClient.isConfigured()) {
            return new FinalAnswer(
                withRuleSummaryNotice(synthesizedWithFailures),
                "tool_query_rule_summary",
                longCatAnthropicClient.configurationStatus(),
                false
            );
        }
        String systemPrompt = finalAnswerSystemPrompt(conversationSummary);
        String prompt = finalAnswerUserPrompt(userMessage, payload, synthesizedWithFailures, history);
        Optional<String> refined = longCatAnthropicClient.createJsonMessage(systemPrompt, prompt);
        return refined
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(answer -> appendFailureNotice(answer, payload.toolFailures()))
            .map(answer -> appendQueryBoundaryNotice(answer, effectiveToolResults))
            .map(answer -> new FinalAnswer(answer, "tool_query_llm_synthesized", "available", true))
            .orElseGet(() -> new FinalAnswer(
                withRuleSummaryNotice(synthesizedWithFailures),
                "tool_query_rule_summary",
                "failed_or_empty",
                true
            ));
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
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            String failedAnswer = appendFailureNotice(payload.answer(), payload.toolFailures());
            emitDeterministicAnswerDeltas(emitter, runId, failedAnswer, "rule_summary", onFirstModelDelta);
            return new FinalAnswer(failedAnswer, "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            emitDeterministicAnswerDeltas(emitter, runId, payload.answer(), "rule_summary", onFirstModelDelta);
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        List<ToolExecutionResult> effectiveToolResults = collapseToolResultsForPresentation(payload.toolResults());
        String synthesized = synthesizeAnswer(userMessage, effectiveToolResults, payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            effectiveToolResults
        );
        if (!longCatAnthropicClient.isConfigured()) {
            String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
            emitDeterministicAnswerDeltas(emitter, runId, ruleSummaryAnswer, "rule_summary", onFirstModelDelta);
            return new FinalAnswer(
                ruleSummaryAnswer,
                "tool_query_rule_summary",
                longCatAnthropicClient.configurationStatus(),
                false
            );
        }
        if (!longCatAnthropicClient.supportsStreaming()) {
            String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
            emitDeterministicAnswerDeltas(emitter, runId, ruleSummaryAnswer, "rule_summary", onFirstModelDelta);
            return new FinalAnswer(
                ruleSummaryAnswer,
                "tool_query_rule_summary",
                longCatAnthropicClient.streamingUnavailableStatus(),
                false
            );
        }
        String systemPrompt = finalAnswerSystemPrompt(conversationSummary);
        String prompt = finalAnswerUserPrompt(userMessage, payload, synthesizedWithFailures, history);
        StringBuilder streamedAnswer = new StringBuilder();
        SseStreamEmitter.AnswerDeltaBatcher answerDeltaBatcher = sseStreamEmitter.newAnswerDeltaBatcher(emitter, runId);
        Optional<String> streamed = longCatAnthropicClient.streamTextMessage(systemPrompt, prompt, runId, delta -> {
            runAuditService.ensureRunActive(runId);
            streamedAnswer.append(delta);
            boolean emittedVisibleDelta = answerDeltaBatcher.accept(delta, "model_stream");
            if (emittedVisibleDelta) {
                onFirstModelDelta.run();
            }
        });
        answerDeltaBatcher.flush();
        return streamed
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(answer -> appendFailureNotice(answer, payload.toolFailures()))
            .map(answer -> appendQueryBoundaryNotice(answer, effectiveToolResults))
            .map(answer -> emitServerNoticeTailIfNeeded(emitter, runId, streamedAnswer.toString(), answer))
            .map(answer -> new FinalAnswer(answer, "tool_query_llm_streamed", "streaming", true))
            .orElseGet(() -> streamFallbackFinalAnswer(
                emitter,
                runId,
                streamedAnswer,
                synthesizedWithFailures,
                payload,
                onFirstModelDelta
            ));
    }

    private String emitServerNoticeTailIfNeeded(
        SseEmitter emitter,
        String runId,
        String streamedAnswer,
        String finalAnswer
    ) {
        if (!StringUtils.hasText(streamedAnswer) || !StringUtils.hasText(finalAnswer)) {
            return finalAnswer;
        }
        String visibleAnswer = streamedAnswer.trim();
        String normalizedFinalAnswer = finalAnswer.trim();
        if (normalizedFinalAnswer.length() <= visibleAnswer.length() || !normalizedFinalAnswer.startsWith(visibleAnswer)) {
            return finalAnswer;
        }
        String serverNoticeTail = normalizedFinalAnswer.substring(visibleAnswer.length());
        if (StringUtils.hasText(serverNoticeTail)) {
            sseStreamEmitter.emitAnswerDeltaUnchecked(emitter, runId, serverNoticeTail, "server_notice");
        }
        return finalAnswer;
    }

    private FinalAnswer streamFallbackFinalAnswer(
        SseEmitter emitter,
        String runId,
        StringBuilder streamedAnswer,
        String synthesizedWithFailures,
        ResponsePayload payload,
        Runnable onFirstVisibleDelta
    ) {
        if (streamedAnswer != null && StringUtils.hasText(streamedAnswer.toString())) {
            String partialAnswer = appendFailureNotice(streamedAnswer.toString().trim(), payload.toolFailures());
            return new FinalAnswer(partialAnswer, "tool_query_llm_stream_interrupted", "stream_interrupted", true);
        }
        String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
        emitDeterministicAnswerDeltas(emitter, runId, ruleSummaryAnswer, "rule_summary", onFirstVisibleDelta);
        return new FinalAnswer(ruleSummaryAnswer, "tool_query_rule_summary", "stream_failed_or_empty", true);
    }

    private String withRuleSummaryNotice(String answer) {
        String normalized = StringUtils.hasText(answer) ? answer.trim() : "";
        if (normalized.startsWith(RULE_SUMMARY_NOTICE)) {
            return normalized;
        }
        return RULE_SUMMARY_NOTICE + "\n\n" + normalized;
    }

    private void emitDeterministicAnswerDeltas(
        SseEmitter emitter,
        String runId,
        String answer,
        String deltaSource,
        Runnable onFirstVisibleDelta
    ) {
        if (emitter == null || runId == null || !StringUtils.hasText(answer)) {
            return;
        }
        boolean emittedVisibleDelta = false;
        for (String chunk : chunkAnswerForVisibleStream(answer)) {
            sseStreamEmitter.emitAnswerDeltaUnchecked(emitter, runId, chunk, deltaSource);
            if (!emittedVisibleDelta) {
                emittedVisibleDelta = true;
                onFirstVisibleDelta.run();
            }
        }
    }

    private List<String> chunkAnswerForVisibleStream(String answer) {
        if (!StringUtils.hasText(answer)) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < answer.length(); index++) {
            char ch = answer.charAt(index);
            current.append(ch);
            boolean hardBreak = ch == '\n' && current.length() > 0;
            boolean naturalBreak = "。！？；;!?".indexOf(ch) >= 0;
            boolean softLimitReached = current.length() >= 48 && Character.isWhitespace(ch);
            boolean maxLimitReached = current.length() >= 72;
            if (hardBreak || naturalBreak || softLimitReached || maxLimitReached) {
                addChunk(chunks, current);
            }
        }
        addChunk(chunks, current);
        return chunks;
    }

    private void addChunk(List<String> chunks, StringBuilder current) {
        String chunk = current.toString();
        current.setLength(0);
        if (!StringUtils.hasText(chunk)) {
            return;
        }
        chunks.add(chunk);
    }

    private String finalAnswerSystemPrompt(String conversationSummary) {
        String summaryContext = StringUtils.hasText(conversationSummary)
            ? "\n会话摘要：" + conversationSummary + "\n"
            : "";
        return """
            你是智慧记的 agentic AI 助手。你不能编造数据，只能基于服务端白名单工具返回的事实回答。
            回答要求：
            1. 先直接回答用户问题。
            2. 明确引用本轮查询到的关键数据。
            3. 给出 1-3 条可执行建议。
            4. 如果有工具查询失败，必须明确说明哪些查询失败，且不要用模拟数据替代。
            5. 不要输出 Markdown 表格；不要声称查询了未列出的数据。
            6. 结合历史对话上下文理解用户指代（如"他""刚才那个"等）。
            """ + summaryContext;
    }

    private String finalAnswerUserPrompt(String userMessage, ResponsePayload payload,
                                          String synthesizedWithFailures, List<AgentMessageEntity> history) {
        String historyContext = formatHistoryContext(history);
        return historyContext
            + "用户问题：" + userMessage + "\n"
            + "已执行工具结果 JSON：" + serializeToolResults(payload.toolResults()) + "\n"
            + "失败工具 JSON：" + serializeToolFailures(payload.toolFailures()) + "\n"
            + "基于事实的初稿：" + synthesizedWithFailures;
    }

    private List<AgentMessageEntity> loadRecentHistory(Long ownerUserId, Long conversationId, int limit) {
        if (conversationId == null || limit <= 0) {
            return List.of();
        }
        List<AgentMessageEntity> desc = agentMessageRepository
            .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
                ownerUserId, conversationId, PageRequest.of(0, limit)
            );
        return desc.reversed();
    }

    private String formatHistoryContext(List<AgentMessageEntity> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("历史对话：\n");
        for (AgentMessageEntity msg : history) {
            String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "AI" : "用户";
            String content = msg.getContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append(role).append("：").append(content).append('\n');
        }
        return sb.toString() + "\n";
    }

    private String buildUnsupportedIntentAnswer() {
        return "这个问题当前版本还没有接入对应的真实查询工具，所以我不会用不相关数据冒充答案。"
            + "你可以改问：低库存商品、商品库存/价格、客户应收、供应商应付、销售单、采购单、付款单、资金流水或近 7 天经营概览。";
    }

    private String buildAllToolsFailedAnswer(List<ToolFailureResult> toolFailures) {
        return "本轮请求匹配到了真实查询工具，但查询过程中失败了。"
            + "我没有使用模拟数据替代，因此不能给出确定结论。"
            + "\n" + formatFailureNotice(toolFailures);
    }

    private String serializeToolResults(List<ToolExecutionResult> toolResults) {
        try {
            return objectMapper.writeValueAsString(toolResults);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String serializeToolFailures(List<ToolFailureResult> toolFailures) {
        try {
            return objectMapper.writeValueAsString(toolFailures == null ? List.of() : toolFailures);
        } catch (JsonProcessingException e) {
            return "[]";
        }
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

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
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

    private String appendFailureNotice(String answer, List<ToolFailureResult> toolFailures) {
        if (toolFailures == null || toolFailures.isEmpty()) {
            return answer;
        }
        String notice = formatFailureNotice(toolFailures);
        if (StringUtils.hasText(answer) && answer.contains(notice)) {
            return answer;
        }
        return (StringUtils.hasText(answer) ? answer.trim() + "\n" : "") + notice;
    }

    private String formatFailureNotice(List<ToolFailureResult> toolFailures) {
        StringBuilder builder = new StringBuilder("部分查询失败：");
        for (int i = 0; i < toolFailures.size(); i++) {
            ToolFailureResult failure = toolFailures.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append(failure.toolName()).append("（").append(failure.safeMessage()).append("）");
        }
        builder.append("。失败部分未使用模拟数据替代。");
        return builder.toString();
    }

    private String appendQueryBoundaryNotice(String answer, List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return answer;
        }
        String normalized = StringUtils.hasText(answer) ? answer.trim() : "";
        if (normalized.contains("查询边界：")) {
            return normalized;
        }
        Set<String> notices = new LinkedHashSet<>();
        for (ToolExecutionResult result : toolResults) {
            for (String notice : queryBoundaryNotices(result)) {
                notices.add(notice);
            }
        }
        if (notices.isEmpty()) {
            return normalized;
        }
        return normalized + "\n查询边界：" + String.join("；", notices) + "。";
    }

    private List<String> queryBoundaryNotices(ToolExecutionResult result) {
        if (result == null) {
            return List.of();
        }
        List<String> notices = new ArrayList<>();
        Map<String, Object> audit = result.queryAudit();
        Integer limit = asInteger(audit.get("limit"));
        Integer returnedCount = asInteger(audit.get("returned_count"));
        Integer totalCount = asInteger(audit.get("total_count"));
        Boolean truncated = asBoolean(audit.get("is_truncated"));
        String label = toolDisplayName(result.toolName());
        if (Boolean.TRUE.equals(truncated)) {
            String totalText = totalCount == null ? "" : " / 已知 " + totalCount + " 条";
            notices.add(label + "仅返回前 " + safeInt(returnedCount) + totalText + " 条，不能视为全量结论");
        } else if (limit != null && returnedCount != null) {
            notices.add(label + "最多查询 " + limit + " 条，实际返回 " + returnedCount + " 条");
        }
        long windowDays = result.facts() == null ? 0L : result.facts().path("window_days").asLong(0L);
        if (windowDays > 0L) {
            notices.add(label + "窗口为近 " + windowDays + " 天");
        }
        return notices;
    }

    private String toolDisplayName(String toolName) {
        return switch (safeText(toolName, "")) {
            case "inventory_low_stock_lookup" -> "低库存查询";
            case "product_catalog_lookup" -> "商品查询";
            case "customer_receivable_lookup" -> "客户应收查询";
            case "supplier_payable_lookup" -> "供应商应付查询";
            case "sales_overview_lookup" -> "经营概览查询";
            case "sale_order_lookup" -> "销售单查询";
            case "purchase_order_lookup" -> "采购单查询";
            case "pay_order_lookup" -> "付款单查询";
            case "finance_record_lookup" -> "资金流水查询";
            default -> safeText(toolName, "工具查询");
        };
    }

    private String synthesizeAnswer(String userMessage, List<ToolExecutionResult> toolResults, String fallbackAnswer) {
        if (toolResults == null || toolResults.isEmpty()) {
            return fallbackAnswer;
        }
        List<String> findings = new ArrayList<>();
        Set<String> actions = new LinkedHashSet<>();

        for (ToolExecutionResult toolResult : toolResults) {
            switch (toolResult.toolName()) {
                case "inventory_low_stock_lookup" -> {
                    Integer count = factInt(toolResult, "low_stock_count");
                    String countText = factText(toolResult, "low_stock_count", "低库存商品数量");
                    findings.add(count != null && count == 0
                        ? "库存侧暂时没有发现低于安全库存的商品。"
                        : "库存侧共发现 " + countText + " 个低库存商品，需要优先补货。");
                    if (count != null && count > 0) {
                        actions.add("优先处理前 3 个低库存商品，避免影响接单和销售。");
                    }
                }
                case "product_catalog_lookup" -> {
                    String count = factText(toolResult, "product_count", "商品数量");
                    String stockTotal = factText(toolResult, "stock_total", "库存总计");
                    String lowStockCount = factText(toolResult, "low_stock_count", "低库存商品数量");
                    findings.add("商品侧商品总数 " + count + " 个，库存总计 " + stockTotal + "，低库存商品 " + lowStockCount + " 个。");
                }
                case "inventory_panorama_lookup" -> {
                    String productName = factText(toolResult, "product_name", "商品名称");
                    String currentStock = factText(toolResult, "current_stock", "当前库存");
                    String safeStock = factText(toolResult, "safe_stock", "安全库存");
                    String recentSalesQuantity = factText(toolResult, "recent_sales_quantity", "近30天销量");
                    String turnoverDays = factText(toolResult, "turnover_days", "周转天数");
                    String suggestedRestock = factText(toolResult, "suggested_restock", "建议补货量");
                    findings.add("商品「" + productName + "」当前库存 " + currentStock
                        + "，安全库存 " + safeStock
                        + "，近30天销量 " + recentSalesQuantity
                        + "，周转天数 " + turnoverDays
                        + "，建议补货量 " + suggestedRestock + "。");
                    actions.add("优先复核「" + productName + "」的安全库存和补货节奏。");
                }
                case "purchase_tracking_lookup" -> {
                    String orderNo = factText(toolResult, "order_no", "采购单号");
                    String supplierName = factText(toolResult, "supplier_name", "供应商");
                    String totalAmount = factText(toolResult, "total_amount", "采购总额");
                    String receivedAmount = factText(toolResult, "received_amount", "已到货");
                    String outstandingAmount = factText(toolResult, "outstanding_amount", "待付款");
                    String receiptCount = factText(toolResult, "receipt_count", "入库单数");
                    String returnCount = factText(toolResult, "return_count", "退货单数");
                    findings.add("采购单「" + orderNo + "」供应商为 " + supplierName
                        + "，采购总额 " + totalAmount
                        + "，已到货 " + receivedAmount
                        + "，待付款 " + outstandingAmount
                        + "；关联入库 " + receiptCount + " 条，退货 " + returnCount + " 条。");
                    actions.add("继续核对「" + orderNo + "」的到货、退货和付款闭环。");
                }
                case "account_health_lookup" -> {
                    String accountCount = factText(toolResult, "account_count", "账户总数");
                    String totalBalance = factText(toolResult, "total_balance", "账户总余额");
                    String ratio = factText(toolResult, "income_expense_ratio", "收支比");
                    String lowBalanceCount = factText(toolResult, "low_balance_count", "低余额账户数");
                    String transferCount = factText(toolResult, "transfer_count", "近期转账数");
                    String defaultAccountName = factText(toolResult, "default_account_name", "默认账户");
                    findings.add("账户侧共 " + accountCount + " 个账户，账户总余额 " + totalBalance
                        + "，收支比 " + ratio
                        + "，低余额账户 " + lowBalanceCount + " 个，近期转账 " + transferCount + " 条。");
                    actions.add("优先复核默认账户「" + defaultAccountName + "」及低余额账户的资金调度。");
                }
                case "customer_receivable_lookup" -> {
                    Integer count = factInt(toolResult, "customer_count");
                    String countText = factText(toolResult, "customer_count", "客户数量");
                    String total = factText(toolResult, "total_receivable", "应收总额");
                    findings.add(count != null && count == 0
                        ? "客户侧没有明显应收欠款压力。"
                        : "客户侧欠款客户总数 " + countText + " 个，应收总额 " + total + "。");
                    if (count != null && count > 0) {
                        actions.add("先跟进欠款最高的 2 到 3 位客户，缩短回款周期。");
                    }
                }
                case "customer_profile_lookup" -> {
                    String customerName = factText(toolResult, "customer_name", "客户名称");
                    String totalSalesAmount = factText(toolResult, "total_sales_amount", "累计销售额");
                    String balance = factText(toolResult, "balance", "当前欠款");
                    String paymentHabit = factText(toolResult, "payment_habit", "付款习惯");
                    String collectionSuggestion = factText(toolResult, "collection_suggestion", "催收建议");
                    findings.add("客户「" + customerName + "」累计销售额 " + totalSalesAmount
                        + "，当前欠款 " + balance + "，付款习惯偏" + paymentHabit + "。");
                    actions.add(collectionSuggestion);
                }
                case "supplier_payable_lookup" -> {
                    Integer count = factInt(toolResult, "supplier_count");
                    String countText = factText(toolResult, "supplier_count", "供应商数量");
                    String total = factText(toolResult, "total_payable", "应付总额");
                    findings.add(count != null && count == 0
                        ? "供应商侧暂时没有突出的应付压力。"
                        : "供应商侧应付供应商总数 " + countText + " 个，应付总额 " + total + "。");
                    if (count != null && count > 0) {
                        actions.add("结合回款节奏安排供应商付款，避免现金流过度前置。");
                    }
                }
                case "sales_overview_lookup" -> {
                    String salesAmount = factText(toolResult, "sales_amount", "销售额");
                    String paidAmount = factText(toolResult, "paid_amount", "回款金额");
                    String salesCount = factText(toolResult, "sales_count", "销售笔数");
                    findings.add("近 7 天销售 " + salesCount + " 笔，销售额 " + salesAmount + "，回款 " + paidAmount + "。");
                }
                case "sale_order_lookup" -> {
                    String count = factText(toolResult, "order_count", "销售单数量");
                    String unpaidCount = factText(toolResult, "unpaid_count", "未收清数量");
                    String total = factText(toolResult, "recent_total_amount", "销售额");
                    String firstOrderNo = factArrayItemText(toolResult, "recent_orders", 0, "order_no");
                    String firstCustomerName = factArrayItemText(toolResult, "recent_orders", 0, "customer_name");
                    String leadOrder = StringUtils.hasText(firstOrderNo)
                        ? "，例如订单 " + firstOrderNo
                            + (StringUtils.hasText(firstCustomerName) ? "（" + firstCustomerName + "）" : "")
                        : "";
                    findings.add("销售单侧最近查询 " + count + " 条，查询销售额 " + total + "，未收清 " + unpaidCount + " 条" + leadOrder + "。");
                }
                case "purchase_order_lookup" -> {
                    String count = factText(toolResult, "order_count", "采购单数量");
                    String total = factText(toolResult, "recent_total_amount", "采购额");
                    findings.add("采购单侧最近查询 " + count + " 条，采购额 " + total + "。");
                }
                case "pay_order_lookup" -> {
                    String count = factText(toolResult, "pay_order_count", "付款单数量");
                    String pendingCount = factText(toolResult, "pending_count", "待付款数量");
                    String total = factText(toolResult, "recent_total_amount", "付款额");
                    findings.add("付款单侧最近查询 " + count + " 条，付款额 " + total + "，待付款 " + pendingCount + " 条。");
                }
                case "finance_record_lookup" -> {
                    String income = factText(toolResult, "recent_income", "收入");
                    String expense = factText(toolResult, "recent_expense", "支出");
                    String count = factText(toolResult, "record_count", "资金流水数量");
                    findings.add("资金流水侧最近查询 " + count + " 条，收入 " + income + "，支出 " + expense + "。");
                }
                default -> findings.add(toolResult.summary());
            }
        }

        if (findings.isEmpty()) {
            return fallbackAnswer;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("针对“").append(userMessage.trim()).append("”，我已基于当前登录账号下的真实经营数据完成查询。");
        builder.append("\n");
        for (int i = 0; i < findings.size(); i++) {
            builder.append(i + 1).append(". ").append(findings.get(i));
            if (i < findings.size() - 1) {
                builder.append("\n");
            }
        }

        List<String> dedupedActions = new ArrayList<>(Math.min(actions.size(), 3));
        for (String action : actions) {
            dedupedActions.add(action);
            if (dedupedActions.size() >= 3) {
                break;
            }
        }
        if (!dedupedActions.isEmpty()) {
            builder.append("\n建议：");
            for (int i = 0; i < dedupedActions.size(); i++) {
                builder.append("\n").append(i + 1).append(". ").append(dedupedActions.get(i));
            }
        }
        return builder.toString();
    }

    private String factText(ToolExecutionResult toolResult, String fieldName, String displayName) {
        if (toolResult == null || toolResult.facts() == null || !toolResult.facts().has(fieldName) || toolResult.facts().path(fieldName).isNull()) {
            return "后端未返回" + displayName;
        }
        String value = toolResult.facts().path(fieldName).asText();
        return StringUtils.hasText(value) ? value : "后端未返回" + displayName;
    }

    private Integer factInt(ToolExecutionResult toolResult, String fieldName) {
        if (toolResult == null || toolResult.facts() == null || !toolResult.facts().has(fieldName) || !toolResult.facts().path(fieldName).canConvertToInt()) {
            return null;
        }
        return Math.max(0, toolResult.facts().path(fieldName).asInt());
    }

    private String factArrayItemText(ToolExecutionResult toolResult, String arrayFieldName, int index, String childFieldName) {
        if (toolResult == null || toolResult.facts() == null) {
            return null;
        }
        JsonNode arrayNode = toolResult.facts().path(arrayFieldName);
        if (!arrayNode.isArray() || index < 0 || index >= arrayNode.size()) {
            return null;
        }
        JsonNode childNode = arrayNode.path(index).path(childFieldName);
        if (childNode.isMissingNode() || childNode.isNull()) {
            return null;
        }
        String value = childNode.asText();
        return StringUtils.hasText(value) ? value : null;
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

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private record ResponsePayload(
        String answer,
        List<V2AgentDtos.ResultBlockDto> blocks,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        AgentToolPlan plan
    ) {
        private ResponsePayload(
            String answer,
            List<V2AgentDtos.ResultBlockDto> blocks,
            List<ToolExecutionResult> toolResults
        ) {
            this(answer, blocks, toolResults, List.of(), new AgentToolPlan(List.of(), "工具直接返回", "tool", Map.of()));
        }

        private String planSource() {
            return plan == null ? "tool" : plan.source();
        }

        private String planSummary() {
            if (plan == null) {
                return "工具直接返回";
            }
            if (plan.tools() == null || plan.tools().isEmpty()) {
                return plan.rationale() + "：未匹配到已接入的真实查询工具";
            }
            return plan.rationale() + "：" + String.join("、", plan.tools());
        }

        private long toolDurationMs() {
            long total = 0L;
            if (toolResults != null) {
                for (ToolExecutionResult result : toolResults) {
                    total += Math.max(0L, result.durationMs());
                }
            }
            return total;
        }
    }

    private record AgentToolPlan(List<String> tools, String rationale, String source, Map<String, JsonNode> toolParams) {
        private AgentToolPlan(List<String> tools, String rationale) {
            this(tools, rationale, "llm", Map.of());
        }
        private AgentToolPlan(List<String> tools, String rationale, Map<String, JsonNode> toolParams) {
            this(tools, rationale, "llm", toolParams);
        }
    }

    private record FinalAnswer(String answer, String mode, String llmStatus, boolean modelAttempted) {}

    private record ToolExecutionResult(String toolName, String summary, JsonNode facts, boolean insufficient) {
        private String toolCallId() {
            return "tool:" + toolName;
        }

        private Map<String, Object> queryAudit() {
            Map<String, Object> audit = new LinkedHashMap<>();
            JsonNode auditNode = facts == null ? null : facts.path("query_audit");
            if (auditNode == null || auditNode.isMissingNode() || !auditNode.isObject()) {
                return audit;
            }
            auditNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isNumber()) {
                    audit.put(entry.getKey(), value.numberValue());
                } else if (value.isBoolean()) {
                    audit.put(entry.getKey(), value.booleanValue());
                } else if (value.isTextual()) {
                    audit.put(entry.getKey(), value.asText());
                } else {
                    audit.put(entry.getKey(), value);
                }
            });
            return audit;
        }

        private long durationMs() {
            Object value = queryAudit().get("duration_ms");
            return value instanceof Number number ? number.longValue() : 0L;
        }
    }

    private record ToolFailureResult(String toolName, String safeMessage) {}
}

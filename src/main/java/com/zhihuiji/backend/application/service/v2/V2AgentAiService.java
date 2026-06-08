package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Map<String, ActiveAgentRun> activeRuns = new ConcurrentHashMap<>();
    private final ExecutorService streamExecutor = Executors.newFixedThreadPool(4, namedThreadFactory("agent-sse-stream"));

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
        LongCatAnthropicClient longCatAnthropicClient
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
    }

    @PreDestroy
    public void shutdownStreamExecutor() {
        streamExecutor.shutdownNow();
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
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
        return rows.stream().map(this::toNotificationResponse).toList();
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
        createRunAudit(ownerUserId, conversation.getId(), runId, runStartedAt);
        SafetyDecision safetyDecision = evaluateSafety(message);
        String auditId = auditIdFor(runId);
        String traceId = traceIdFor(runId);
        if (!safetyDecision.passed()) {
            String blockedAnswer = "这个请求涉及越权或高风险操作，我不能直接执行。你可以改成只查询当前账号下的合规数据范围。";
            persistAssistantResponse(ownerUserId, conversation, blockedAnswer, List.of(), now);
            long blockedCompletedAt = System.currentTimeMillis();
            finishRunAudit(
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
                observabilityFor(runId, auditId, traceId)
            );
        }

        ResponsePayload payload = buildResponse(ownerUserId, message, null, runId);
        long modelStartedAt = System.currentTimeMillis();
        FinalAnswer finalAnswer = buildFinalAnswer(message, payload);
        long completedAt = System.currentTimeMillis();
        long modelDurationMs = finalAnswer.modelAttempted()
            ? Math.max(0L, completedAt - modelStartedAt)
            : 0L;
        persistAssistantResponse(ownerUserId, conversation, finalAnswer.answer(), payload.blocks(), System.currentTimeMillis());
        finishRunAudit(
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
            observabilityFor(runId, auditId, traceId)
        );
    }

    public SseEmitter chatStream(V2AgentDtos.AgentChatRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        String message = normalizeRequired(request.message(), "message 不能为空");
        AgentConversationEntity conversation = resolveConversation(request.conversationId(), ownerUserId, message, now);
        saveMessage(ownerUserId, conversation.getId(), "user", "text", message, null, now);
        String runId = UUID.randomUUID().toString();
        createRunAudit(ownerUserId, conversation.getId(), runId, now);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        ActiveAgentRun activeRun = new ActiveAgentRun(ownerUserId, runId, conversation.getId(), emitter);
        activeRuns.put(runId, activeRun);
        emitter.onCompletion(() -> activeRuns.remove(runId));
        emitter.onTimeout(() -> activeRuns.remove(runId));
        emitter.onError(ignored -> activeRuns.remove(runId));
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                runChatStream(ownerUserId, conversation, message, runId, emitter);
            } catch (AgentRunCancelledException ignored) {
                // cancelRun 已向客户端发送 run_cancelled；worker 负责收尾关闭 emitter。
                activeRun.complete();
            } catch (Exception ex) {
                finishRunAudit(
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
                    sendEvent(emitter, eventMap("error", Map.of(
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
                activeRuns.remove(runId);
            }
        }, streamExecutor);
        activeRun.attachFuture(future);
        return emitter;
    }

    public V2AgentDtos.AgentRunCancelResponse cancelRun(String runId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedRunId = normalizeRequired(runId, "run_id 不能为空");
        ActiveAgentRun activeRun = activeRuns.get(normalizedRunId);
        if (activeRun == null || !activeRun.ownerUserId().equals(ownerUserId)) {
            return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "not_found", false);
        }
        if (activeRun.cancelled()) {
            return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "already_cancelled", true);
        }
        activeRun.cancel();
        emitRunCancelled(activeRun.emitter(), normalizedRunId, "用户已停止生成");
        finishRunAudit(
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
            activeRuns.remove(normalizedRunId);
        }
        return new V2AgentDtos.AgentRunCancelResponse(normalizedRunId, "cancelled", true);
    }

    public V2AgentDtos.AgentRunAuditResponse getRunAudit(String runId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedRunId = normalizeRequired(runId, "run_id 不能为空");
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
        boolean registeredForDirectRun = activeRuns.putIfAbsent(
            runId,
            new ActiveAgentRun(ownerUserId, runId, conversation.getId(), emitter)
        ) == null;
        ensureRunAuditStarted(ownerUserId, conversation.getId(), runId, System.currentTimeMillis());
        try {
            String auditId = auditIdFor(runId);
            String traceId = traceIdFor(runId);
            sendEvent(emitter, eventMap("run_started", Map.of(
                "run_id", runId,
                "conversation_id", conversation.getId(),
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
            ensureRunActive(runId);

            sendEvent(emitter, eventMap("safety_check_started", Map.of(
                "run_id", runId,
                "timestamp", System.currentTimeMillis()
            )));
            ensureRunActive(runId);

            SafetyDecision safetyDecision = evaluateSafety(message);
            if (!safetyDecision.passed()) {
                sendEvent(emitter, eventMap("safety_check_blocked", mapOf(
                    "run_id", runId,
                    "reason", safetyDecision.reason(),
                    "suggested_action", "改成仅查询当前登录账号可见的数据",
                    "timestamp", System.currentTimeMillis()
                )));
                String blockedAnswer = "这个请求涉及越权或高风险操作，我不能直接执行。";
                emitAnswerCompleted(emitter, runId, blockedAnswer, "blocked", "not_requested");
                persistAssistantResponse(ownerUserId, conversation, blockedAnswer, List.of(), System.currentTimeMillis());
                sendEvent(emitter, eventMap("run_completed", mapOf(
                    "run_id", runId,
                    "final_answer", blockedAnswer,
                    "mode", "blocked",
                    "llm_status", "not_requested",
                    "plan_source", "safety",
                    "audit_id", auditId,
                    "trace_id", traceId,
                    "observability", observabilityFor(runId, auditId, traceId),
                    "timestamp", System.currentTimeMillis()
                )));
                finishRunAudit(
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

            sendEvent(emitter, eventMap("safety_check_passed", Map.of(
                "run_id", runId,
                "timestamp", System.currentTimeMillis()
            )));
            ensureRunActive(runId);
            ResponsePayload payload = buildResponse(ownerUserId, message, emitter, runId);
            ensureRunActive(runId);
            FinalAnswer finalAnswer = buildFinalAnswerForStream(message, payload, emitter, runId);
            ensureRunActive(runId);
            emitAnswerCompleted(emitter, runId, finalAnswer.answer(), finalAnswer.mode(), finalAnswer.llmStatus());
            persistAssistantResponse(ownerUserId, conversation, finalAnswer.answer(), payload.blocks(), System.currentTimeMillis());
            sendEvent(emitter, eventMap("run_completed", mapOf(
                "run_id", runId,
                "final_answer", finalAnswer.answer(),
                "mode", finalAnswer.mode(),
                "llm_status", finalAnswer.llmStatus(),
                "plan_source", payload.planSource(),
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
            finishRunAudit(
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
                activeRuns.remove(runId);
            }
        }
    }

    private ResponsePayload buildResponse(Long ownerUserId, String message) {
        return buildResponse(ownerUserId, message, null, null);
    }

    private ResponsePayload buildResponse(Long ownerUserId, String message, SseEmitter emitter, String runId) {
        AgentToolPlan plan = planTools(message);
        emitPlan(emitter, runId, plan);

        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        List<ToolExecutionResult> toolResults = new ArrayList<>();
        List<ToolFailureResult> toolFailures = new ArrayList<>();
        for (String tool : plan.tools()) {
            long startedAt = System.currentTimeMillis();
            try {
                ensureRunActive(runId);
                ResponsePayload payload = executePlannedTool(ownerUserId, emitter, runId, tool);
                ensureRunActive(runId);
                if (payload != null) {
                    answers.add(payload.answer());
                    blocks.addAll(payload.blocks());
                    toolResults.addAll(payload.toolResults());
                }
            } catch (AgentRunCancelledException ex) {
                throw ex;
            } catch (Exception ex) {
                if (isStreamEmissionFailure(ex)) {
                    if (ex instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new IllegalStateException("stream emission failed", ex);
                }
                String errorSummary = safeToolErrorSummary(ex);
                toolFailures.add(new ToolFailureResult(tool, errorSummary));
                emitToolFailed(
                    emitter,
                    runId,
                    tool,
                    errorSummary,
                    System.currentTimeMillis() - startedAt,
                    startedAt,
                    toolInputFor(tool)
                );
            }
        }

        if (!toolResults.isEmpty()) {
            ensureRunActive(runId);
            V2AgentDtos.ResultBlockDto evidenceBlock = buildEvidenceBlock(runId, toolResults);
            blocks.add(evidenceBlock);
            emitBlocks(emitter, runId, List.of(evidenceBlock));
        }

        if (answers.isEmpty()) {
            answers.add(toolFailures.isEmpty() ? buildUnsupportedIntentAnswer() : buildAllToolsFailedAnswer(toolFailures));
        }

        return new ResponsePayload(String.join("\n", answers), blocks, toolResults, toolFailures, plan);
    }

    private ResponsePayload executePlannedTool(Long ownerUserId, SseEmitter emitter, String runId, String tool) {
        return switch (tool) {
            case "inventory_low_stock_lookup" -> buildInventoryResponse(ownerUserId, emitter, runId);
            case "product_catalog_lookup" -> buildProductCatalogResponse(ownerUserId, emitter, runId);
            case "customer_receivable_lookup" -> buildReceivableResponse(ownerUserId, emitter, runId);
            case "supplier_payable_lookup" -> buildSupplierPayableResponse(ownerUserId, emitter, runId);
            case "sale_order_lookup" -> buildSaleOrderResponse(ownerUserId, emitter, runId);
            case "purchase_order_lookup" -> buildPurchaseOrderResponse(ownerUserId, emitter, runId);
            case "pay_order_lookup" -> buildPayOrderResponse(ownerUserId, emitter, runId);
            case "finance_record_lookup" -> buildFinanceRecordResponse(ownerUserId, emitter, runId);
            case "sales_overview_lookup" -> buildOverviewResponse(ownerUserId, emitter, runId);
            default -> null;
        };
    }

    private Map<String, Object> toolInputFor(String toolName) {
        return switch (toolName) {
            case "inventory_low_stock_lookup", "product_catalog_lookup", "customer_receivable_lookup",
                "supplier_payable_lookup", "sale_order_lookup", "purchase_order_lookup",
                "pay_order_lookup", "finance_record_lookup" -> Map.of("limit", DEFAULT_TOOL_LIMIT);
            case "sales_overview_lookup" -> Map.of(
                "window_days", 7,
                "rank_limit", OVERVIEW_SIGNAL_LIMIT,
                "low_stock_limit", OVERVIEW_SIGNAL_LIMIT
            );
            default -> Map.of();
        };
    }

    private AgentToolPlan planTools(String message) {
        return planToolsWithLlm(message).orElseGet(() -> inferToolPlan(message));
    }

    private Optional<AgentToolPlan> planToolsWithLlm(String message) {
        if (!longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        String systemPrompt = """
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
        String userPrompt = "用户问题：" + message + "\n"
            + "请输出形如 {\"tools\":[\"sales_overview_lookup\"],\"rationale\":\"...\"} 的 JSON。"
            + "tools 最多 3 个，必须来自白名单。";
        return longCatAnthropicClient.createJsonMessage(systemPrompt, userPrompt).flatMap(this::parseToolPlan);
    }

    private Optional<AgentToolPlan> parseToolPlan(String rawText) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawText));
            List<String> tools = new ArrayList<>();
            JsonNode toolsNode = root.get("tools");
            if (toolsNode != null && toolsNode.isArray()) {
                for (JsonNode item : toolsNode) {
                    String tool = item.asText("");
                    if (isAllowedTool(tool) && !tools.contains(tool)) {
                        tools.add(tool);
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
            return Optional.of(new AgentToolPlan(tools, rationale));
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

    private AgentToolPlan inferToolPlan(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        List<String> tools = new ArrayList<>();
        if (containsAny(normalized, "库存", "补货", "低库存", "缺货", "inventory", "stock", "low stock", "replenish")) {
            tools.add("inventory_low_stock_lookup");
        }
        if (containsAny(normalized, "商品", "sku", "品类", "目录", "售价", "进价", "product", "catalog", "price")) {
            tools.add("product_catalog_lookup");
        }
        if (containsAny(normalized, "欠款", "应收", "客户", "回款", "receivable", "customer", "collection")) {
            tools.add("customer_receivable_lookup");
        }
        if (containsAny(normalized, "供应商", "应付", "采购", "到货", "收货", "supplier", "payable", "purchase", "procurement")) {
            tools.add("supplier_payable_lookup");
            tools.add("purchase_order_lookup");
        }
        if (containsAny(normalized, "销售单", "订单", "成交", "收款", "付款情况", "sale order", "sales order", "order", "deal")) {
            tools.add("sale_order_lookup");
        }
        if (containsAny(normalized, "付款单", "已付款", "待付款", "payment", "paid", "unpaid")) {
            tools.add("pay_order_lookup");
        }
        if (containsAny(normalized, "流水", "收入", "支出", "财务", "费用", "开支", "finance", "cashflow", "income", "expense")) {
            tools.add("finance_record_lookup");
        }
        if (containsAny(normalized, "经营", "概览", "销售", "最近", "7天", "七天", "business", "overview", "sales", "recent", "7 days")) {
            tools.add("sales_overview_lookup");
        }
        List<String> deduplicated = new ArrayList<>();
        for (String tool : tools) {
            if (isAllowedTool(tool) && !deduplicated.contains(tool)) {
                deduplicated.add(tool);
            }
            if (deduplicated.size() >= 4) {
                break;
            }
        }
        return new AgentToolPlan(deduplicated, "根据问题关键词兜底选择只读查询工具", "keyword_fallback");
    }

    private boolean isAllowedTool(String tool) {
        return "inventory_low_stock_lookup".equals(tool)
            || "customer_receivable_lookup".equals(tool)
            || "sales_overview_lookup".equals(tool)
            || "product_catalog_lookup".equals(tool)
            || "supplier_payable_lookup".equals(tool)
            || "sale_order_lookup".equals(tool)
            || "purchase_order_lookup".equals(tool)
            || "pay_order_lookup".equals(tool)
            || "finance_record_lookup".equals(tool);
    }

    private ResponsePayload buildInventoryResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "inventory_low_stock_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<ProductEntity> products = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        audit.markLimitedResult(products.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(emitter, runId, "inventory_low_stock_lookup", "命中 " + products.size() + " 个低库存商品", audit);

        List<String> affectedItems = products.stream()
            .limit(5)
            .map(ProductEntity::getName)
            .toList();
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "库存风险",
            toJsonNode(mapOf(
                "level", products.isEmpty() ? "low" : "high",
                "title", products.isEmpty() ? "暂无低库存风险" : "检测到低库存商品",
                "description", products.isEmpty() ? "当前库存没有低于安全库存的商品。" : "建议优先补货，避免影响销售。",
                "affected_items", affectedItems,
                "suggested_action", products.isEmpty() ? "保持当前补货节奏" : "先处理前 3 个低库存商品"
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "低库存商品列表",
            toJsonNode(mapOf(
                "headers", List.of("商品", "编码", "当前库存", "安全库存", "销售价"),
                "rows", products.stream().map(item -> List.of(
                    item.getName(),
                    item.getCode(),
                    formatNumber(item.getStock()),
                    formatNumber(item.getSafeStock()),
                    money(safeDouble(item.getSalePrice()))
                )).toList(),
                "row_count", products.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(riskBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = products.isEmpty()
            ? "我已经检查了当前账号下的库存，暂时没有发现低于安全库存的商品。"
            : "我已经找出当前账号下最需要关注的低库存商品，建议先处理排在前面的补货项。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "inventory_low_stock_lookup",
            "低库存商品 " + products.size() + " 个",
            toJsonNode(mapOf(
                "low_stock_count", products.size(),
                "query_audit", audit.facts(),
                "top_items", products.stream().limit(5).map(item -> mapOf(
                    "name", item.getName(),
                    "code", item.getCode(),
                    "stock", formatNumber(item.getStock()),
                    "safe_stock", formatNumber(item.getSafeStock()),
                    "sale_price", money(safeDouble(item.getSalePrice()))
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildProductCatalogResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "product_catalog_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<ProductEntity> products = productRepository.findAllByOwnerUserIdOrderByNameAsc(ownerUserId, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        audit.markLimitedResult(products.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(emitter, runId, "product_catalog_lookup", "命中 " + products.size() + " 个商品", audit);

        double totalStock = products.stream().mapToDouble(item -> safeDouble(item.getStock())).sum();
        long lowStockCount = products.stream().filter(item -> safeDouble(item.getStock()) <= safeDouble(item.getSafeStock())).count();
        ProductEntity maxStockProduct = products.stream()
            .max(Comparator.comparingDouble(item -> safeDouble(item.getStock())))
            .orElse(null);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "商品概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "商品数", "value", String.valueOf(products.size()), "trend_direction", products.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询库存合计", "value", formatNumber(totalStock), "trend_direction", totalStock > 0 ? "up" : "flat"),
                    mapOf("label", "低库存商品", "value", String.valueOf(lowStockCount), "trend_direction", lowStockCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "商品列表",
            toJsonNode(mapOf(
                "headers", List.of("商品", "编码", "分类", "库存", "售价"),
                "rows", products.stream().map(item -> List.of(
                    item.getName(),
                    item.getCode(),
                    safeText(item.getCategory(), "-"),
                    formatNumber(safeDouble(item.getStock())),
                    money(safeDouble(item.getSalePrice()))
                )).toList(),
                "row_count", products.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = products.isEmpty()
            ? "当前账号下还没有商品数据。"
            : "当前账号下已查询到 " + products.size() + " 个商品，"
                + "查询库存合计 " + formatNumber(totalStock) + "，低库存商品 " + lowStockCount + " 个。"
                + (maxStockProduct == null ? "" : "当前库存最高的是 " + maxStockProduct.getName() + "。");
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "product_catalog_lookup",
            "商品查询 " + products.size() + " 个，低库存 " + lowStockCount + " 个",
            toJsonNode(mapOf(
                "product_count", products.size(),
                "queried_stock_total", formatNumber(totalStock),
                "low_stock_count", lowStockCount,
                "query_audit", audit.facts(),
                "top_products", products.stream().limit(5).map(item -> mapOf(
                    "name", item.getName(),
                    "code", item.getCode(),
                    "category", safeText(item.getCategory(), "-"),
                    "stock", formatNumber(safeDouble(item.getStock())),
                    "sale_price", money(safeDouble(item.getSalePrice()))
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildReceivableResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "customer_receivable_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<CustomerEntity> customers = customerRepository
            .findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(ownerUserId, 0.0, PageRequest.of(0, DEFAULT_TOOL_LIMIT));
        audit.markLimitedResult(customers.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(emitter, runId, "customer_receivable_lookup", "命中 " + customers.size() + " 个欠款客户", audit);

        double totalReceivable = customers.stream().mapToDouble(item -> safeDouble(item.getBalance())).sum();
        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "应收概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "欠款客户数", "value", String.valueOf(customers.size()), "trend_direction", customers.isEmpty() ? "flat" : "up"),
                    mapOf("label", "Top10 应收合计", "value", money(totalReceivable), "trend_direction", totalReceivable > 0 ? "down" : "flat"),
                    mapOf("label", "最高单户欠款", "value", customers.isEmpty() ? money(0) : money(safeDouble(customers.get(0).getBalance())), "trend_direction", customers.isEmpty() ? "flat" : "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "欠款客户排行",
            toJsonNode(mapOf(
                "items", buildRankItems(customers)
            ))
        );
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        blocks.add(kpiBlock);
        if (!customers.isEmpty()) {
            V2AgentDtos.ResultBlockDto barBlock = new V2AgentDtos.ResultBlockDto(
                "bar_chart",
                "Top 客户应收柱状图",
                toJsonNode(mapOf(
                    "title", "Top 客户应收柱状图",
                    "labels", customers.stream().limit(6).map(item -> compactChartLabel(item.getName())).toList(),
                    "series", List.of(mapOf(
                        "name", "应收余额",
                        "data", customers.stream().limit(6).map(item -> safeDouble(item.getBalance())).toList(),
                        "color", "#005BBF"
                    ))
                ))
            );
            blocks.add(barBlock);
        }
        blocks.add(rankBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = customers.isEmpty()
            ? "当前账号下没有查询到欠款客户，应收风险比较低。"
            : "我已经按欠款金额从高到低整理出客户排行，可以先跟进前几位客户的回款。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "customer_receivable_lookup",
            "欠款客户 " + customers.size() + " 个，Top10 应收合计 " + money(totalReceivable),
            toJsonNode(mapOf(
                "customer_count", customers.size(),
                "top10_receivable_total", money(totalReceivable),
                "query_audit", audit.facts(),
                "top_customers", customers.stream().limit(5).map(item -> mapOf(
                    "name", item.getName(),
                    "balance", money(safeDouble(item.getBalance())),
                    "level", item.getLevel()
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildSupplierPayableResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(
            emitter,
            runId,
            "supplier_payable_lookup",
            Map.of("limit", DEFAULT_TOOL_LIMIT)
        );
        List<SupplierEntity> topPayables = supplierRepository.findByOwnerUserIdAndBalanceGreaterThanOrderByBalanceDesc(
            ownerUserId,
            0.0,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        double totalPayable = topPayables.stream().mapToDouble(item -> safeDouble(item.getBalance())).sum();
        audit.markLimitedResult(topPayables.size(), DEFAULT_TOOL_LIMIT);
        emitToolCompleted(emitter, runId, "supplier_payable_lookup", "命中 " + topPayables.size() + " 个应付供应商", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "应付概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "应付供应商数", "value", String.valueOf(topPayables.size()), "trend_direction", topPayables.isEmpty() ? "flat" : "up"),
                    mapOf("label", "Top10 应付合计", "value", money(totalPayable), "trend_direction", totalPayable > 0 ? "up" : "flat"),
                    mapOf("label", "最高单户应付", "value", topPayables.isEmpty() ? money(0) : money(safeDouble(topPayables.get(0).getBalance())), "trend_direction", topPayables.isEmpty() ? "flat" : "up")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "供应商应付排行",
            toJsonNode(mapOf("items", buildSupplierPayableRank(topPayables)))
        );
        List<V2AgentDtos.ResultBlockDto> blocks = new ArrayList<>();
        blocks.add(kpiBlock);
        if (!topPayables.isEmpty()) {
            V2AgentDtos.ResultBlockDto barBlock = new V2AgentDtos.ResultBlockDto(
                "bar_chart",
                "Top 供应商应付柱状图",
                toJsonNode(mapOf(
                    "title", "Top 供应商应付柱状图",
                    "labels", topPayables.stream().limit(6).map(item -> compactChartLabel(item.getName())).toList(),
                    "series", List.of(mapOf(
                        "name", "应付余额",
                        "data", topPayables.stream().limit(6).map(item -> safeDouble(item.getBalance())).toList(),
                        "color", "#FB8C00"
                    ))
                ))
            );
            blocks.add(barBlock);
        }
        blocks.add(rankBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = topPayables.isEmpty()
            ? "当前账号下没有明显的供应商应付欠款。"
            : "当前账号下共有 " + topPayables.size() + " 个重点应付供应商，Top10 应付合计 "
                + money(totalPayable) + "，最高单户应付 "
                + money(safeDouble(topPayables.get(0).getBalance())) + "。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "supplier_payable_lookup",
            "重点应付供应商 " + topPayables.size() + " 个，Top10 应付合计 " + money(totalPayable),
            toJsonNode(mapOf(
                "supplier_count", topPayables.size(),
                "top10_payable_total", money(totalPayable),
                "query_audit", audit.facts(),
                "top_suppliers", topPayables.stream().limit(5).map(item -> mapOf(
                    "name", item.getName(),
                    "balance", money(safeDouble(item.getBalance())),
                    "phone", safeText(item.getPhone(), "-")
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildOverviewResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - SEVEN_DAYS_MS;

        ToolAudit audit = startToolAudit(
            emitter,
            runId,
            "sales_overview_lookup",
            Map.of("window_days", 7, "rank_limit", OVERVIEW_SIGNAL_LIMIT, "low_stock_limit", OVERVIEW_SIGNAL_LIMIT)
        );
        double salesAmount = safeDouble(saleOrderRepository.sumTotalAmountBetween(ownerUserId, sevenDaysAgo, now));
        double paidAmount = safeDouble(saleOrderRepository.sumPaidAmountBetween(ownerUserId, sevenDaysAgo, now));
        long salesCount = safeLong(saleOrderRepository.countNonCancelledBetween(ownerUserId, sevenDaysAgo, now));
        List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(ownerUserId, PageRequest.of(0, OVERVIEW_SIGNAL_LIMIT));
        double receivable = safeDouble(customerRepository.sumPositiveBalance(ownerUserId));
        List<Object[]> customerSales = saleOrderRepository.customerSales(
            ownerUserId,
            sevenDaysAgo,
            now,
            CANCELLED_SALE_ORDER_STATUS,
            PageRequest.of(0, OVERVIEW_SIGNAL_LIMIT)
        );
        audit.markReturned(Math.max(lowStockProducts.size(), customerSales.size()));
        emitToolCompleted(emitter, runId, "sales_overview_lookup", "已汇总近7天销售、应收和库存信号", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "经营概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "近7天销售额", "value", money(salesAmount), "trend_direction", salesAmount > 0 ? "up" : "flat"),
                    mapOf("label", "近7天回款", "value", money(paidAmount), "trend_direction", paidAmount > 0 ? "up" : "flat"),
                    mapOf("label", "销售单数", "value", String.valueOf(salesCount), "trend_direction", salesCount > 0 ? "up" : "flat"),
                    mapOf("label", "当前应收", "value", money(receivable), "trend_direction", receivable > 0 ? "down" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto rankBlock = new V2AgentDtos.ResultBlockDto(
            "rank_list",
            "客户销售排行",
            toJsonNode(mapOf("items", buildCustomerSalesRank(customerSales)))
        );
        V2AgentDtos.ResultBlockDto trendBlock = buildSalesTrendBlock(ownerUserId, sevenDaysAgo, now);
        V2AgentDtos.ResultBlockDto amountBlock = new V2AgentDtos.ResultBlockDto(
            "bar_chart",
            "经营金额对比",
            toJsonNode(mapOf(
                "title", "经营金额对比",
                "labels", List.of("销售额", "回款", "当前应收"),
                "series", List.of(mapOf(
                    "name", "金额",
                    "data", List.of(salesAmount, paidAmount, receivable),
                    "color", "#005BBF"
                ))
            ))
        );
        V2AgentDtos.ResultBlockDto riskBlock = new V2AgentDtos.ResultBlockDto(
            "risk_card",
            "经营提醒",
            toJsonNode(mapOf(
                "level", lowStockProducts.isEmpty() ? "low" : "medium",
                "title", lowStockProducts.isEmpty() ? "暂无显著库存风险" : "存在低库存商品",
                "description", lowStockProducts.isEmpty()
                    ? "近7天经营数据已汇总，当前库存预警不明显。"
                    : "建议同步关注补货与回款，避免销售增长带来缺货。",
                "affected_items", lowStockProducts.stream().limit(3).map(ProductEntity::getName).toList(),
                "suggested_action", lowStockProducts.isEmpty() ? "继续观察趋势" : "优先处理低库存商品并跟进重点客户回款"
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, trendBlock, amountBlock, rankBlock, riskBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = "我已经把当前账号近7天的销售、回款、应收和库存风险汇总好了，可以先从重点客户回款和低库存商品两条线并行处理。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "sales_overview_lookup",
            "近7天销售 " + salesCount + " 笔，销售额 " + money(salesAmount) + "，回款 " + money(paidAmount),
            toJsonNode(mapOf(
                "window_days", 7,
                "sales_amount", money(salesAmount),
                "paid_amount", money(paidAmount),
                "sales_count", salesCount,
                "current_receivable", money(receivable),
                "low_stock_count", lowStockProducts.size(),
                "query_audit", audit.facts(),
                "top_customer_sales", buildCustomerSalesRank(customerSales)
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildSaleOrderResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "sale_order_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<SaleOrderEntity> recentOrders = saleOrderRepository.search(
            ownerUserId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        long unpaidCount = recentOrders.stream()
            .filter(item -> safeDouble(item.getPaidAmount()) + 0.000001 < safeDouble(item.getTotalAmount()))
            .count();
        double recentTotal = recentOrders.stream().mapToDouble(item -> safeDouble(item.getTotalAmount())).sum();
        double recentPaid = recentOrders.stream().mapToDouble(item -> safeDouble(item.getPaidAmount())).sum();
        emitToolCompleted(emitter, runId, "sale_order_lookup", "命中 " + recentOrders.size() + " 条销售单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "销售单概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "最近销售单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询销售额", "value", money(recentTotal), "trend_direction", recentTotal > 0 ? "up" : "flat"),
                    mapOf("label", "未收清单", "value", String.valueOf(unpaidCount), "trend_direction", unpaidCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近销售单",
            toJsonNode(mapOf(
                "headers", List.of("单号", "客户", "总额", "已收", "状态"),
                "rows", recentOrders.stream().map(item -> List.of(
                    safeText(item.getOrderNo(), "-"),
                    safeText(item.getCustomerName(), "-"),
                    money(safeDouble(item.getTotalAmount())),
                    money(safeDouble(item.getPaidAmount())),
                    saleOrderStatusLabel(item.getStatus())
                )).toList(),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有销售单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条销售单，查询销售额 "
                + money(recentTotal) + "，已收 " + money(recentPaid) + "，还有 "
                + unpaidCount + " 条未收清。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "sale_order_lookup",
            "最近销售单 " + recentOrders.size() + " 条，未收清 " + unpaidCount + " 条",
            toJsonNode(mapOf(
                "order_count", recentOrders.size(),
                "recent_total_amount", money(recentTotal),
                "recent_paid_amount", money(recentPaid),
                "unpaid_count", unpaidCount,
                "query_audit", audit.facts(),
                "recent_orders", recentOrders.stream().limit(5).map(item -> mapOf(
                    "order_no", safeText(item.getOrderNo(), "-"),
                    "customer_name", safeText(item.getCustomerName(), "-"),
                    "total_amount", money(safeDouble(item.getTotalAmount())),
                    "paid_amount", money(safeDouble(item.getPaidAmount())),
                    "status", saleOrderStatusLabel(item.getStatus())
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildPurchaseOrderResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "purchase_order_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<PurchaseOrderEntity> recentOrders = purchaseOrderRepository.search(
            ownerUserId,
            null,
            null,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = recentOrders.stream().mapToDouble(item -> safeDouble(item.getTotalAmount())).sum();
        double receivedAmount = recentOrders.stream().mapToDouble(item -> safeDouble(item.getReceivedAmount())).sum();
        emitToolCompleted(emitter, runId, "purchase_order_lookup", "命中 " + recentOrders.size() + " 条采购单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "采购单概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "最近采购单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询采购额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "查询已到货", "value", money(receivedAmount), "trend_direction", receivedAmount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近采购单",
            toJsonNode(mapOf(
                "headers", List.of("单号", "供应商", "总额", "已付", "状态"),
                "rows", recentOrders.stream().map(item -> List.of(
                    safeText(item.getOrderNo(), "-"),
                    safeText(item.getSupplierName(), "-"),
                    money(safeDouble(item.getTotalAmount())),
                    money(safeDouble(item.getPaidAmount())),
                    purchaseOrderStatusLabel(item.getStatus())
                )).toList(),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有采购单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条采购单，查询采购额 "
                + money(totalAmount) + "，查询已到货金额 " + money(receivedAmount) + "。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "purchase_order_lookup",
            "最近采购单 " + recentOrders.size() + " 条，查询采购额 " + money(totalAmount),
            toJsonNode(mapOf(
                "order_count", recentOrders.size(),
                "recent_total_amount", money(totalAmount),
                "recent_received_amount", money(receivedAmount),
                "query_audit", audit.facts(),
                "recent_orders", recentOrders.stream().limit(5).map(item -> mapOf(
                    "order_no", safeText(item.getOrderNo(), "-"),
                    "supplier_name", safeText(item.getSupplierName(), "-"),
                    "total_amount", money(safeDouble(item.getTotalAmount())),
                    "paid_amount", money(safeDouble(item.getPaidAmount())),
                    "status", purchaseOrderStatusLabel(item.getStatus())
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildPayOrderResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "pay_order_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<PayOrderEntity> recentOrders = payOrderRepository.search(
            ownerUserId,
            null,
            null,
            null,
            null,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        audit.markLimitedResult(recentOrders.size(), DEFAULT_TOOL_LIMIT);
        double totalAmount = recentOrders.stream().mapToDouble(item -> safeDouble(item.getAmount())).sum();
        long pendingCount = recentOrders.stream().filter(item -> item.getStatus() != null && item.getStatus() == 0).count();
        emitToolCompleted(emitter, runId, "pay_order_lookup", "命中 " + recentOrders.size() + " 条付款单", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "付款单概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "最近付款单", "value", String.valueOf(recentOrders.size()), "trend_direction", recentOrders.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询付款额", "value", money(totalAmount), "trend_direction", totalAmount > 0 ? "up" : "flat"),
                    mapOf("label", "待付款单", "value", String.valueOf(pendingCount), "trend_direction", pendingCount > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近付款单",
            toJsonNode(mapOf(
                "headers", List.of("单号", "供应商", "金额", "方式", "状态"),
                "rows", recentOrders.stream().map(item -> List.of(
                    safeText(item.getOrderNo(), "-"),
                    safeText(item.getSupplierName(), "-"),
                    money(safeDouble(item.getAmount())),
                    paymentMethodLabel(item.getMethod()),
                    payOrderStatusLabel(item.getStatus())
                )).toList(),
                "row_count", recentOrders.size()
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = recentOrders.isEmpty()
            ? "当前账号下还没有付款单数据。"
            : "我查到了最近 " + recentOrders.size() + " 条付款单，查询付款额 "
                + money(totalAmount) + "，其中待付款单 " + pendingCount + " 条。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "pay_order_lookup",
            "最近付款单 " + recentOrders.size() + " 条，待付款 " + pendingCount + " 条",
            toJsonNode(mapOf(
                "pay_order_count", recentOrders.size(),
                "recent_total_amount", money(totalAmount),
                "pending_count", pendingCount,
                "query_audit", audit.facts(),
                "recent_orders", recentOrders.stream().limit(5).map(item -> mapOf(
                    "order_no", safeText(item.getOrderNo(), "-"),
                    "supplier_name", safeText(item.getSupplierName(), "-"),
                    "amount", money(safeDouble(item.getAmount())),
                    "method", paymentMethodLabel(item.getMethod()),
                    "status", payOrderStatusLabel(item.getStatus())
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private ResponsePayload buildFinanceRecordResponse(Long ownerUserId, SseEmitter emitter, String runId) {
        ToolAudit audit = startToolAudit(emitter, runId, "finance_record_lookup", Map.of("limit", DEFAULT_TOOL_LIMIT));
        List<FinanceRecordEntity> recentRecords = financeRecordRepository.search(
            ownerUserId,
            null,
            null,
            null,
            null,
            PageRequest.of(0, DEFAULT_TOOL_LIMIT)
        );
        audit.markLimitedResult(recentRecords.size(), DEFAULT_TOOL_LIMIT);
        double income = recentRecords.stream()
            .filter(item -> financeTypeLabel(item.getType()).equals("收入"))
            .mapToDouble(item -> safeDouble(item.getAmount()))
            .sum();
        double expense = recentRecords.stream()
            .filter(item -> financeTypeLabel(item.getType()).equals("支出"))
            .mapToDouble(item -> safeDouble(item.getAmount()))
            .sum();
        emitToolCompleted(emitter, runId, "finance_record_lookup", "命中 " + recentRecords.size() + " 条资金流水", audit);

        V2AgentDtos.ResultBlockDto kpiBlock = new V2AgentDtos.ResultBlockDto(
            "kpi_grid",
            "资金流水概览",
            toJsonNode(mapOf(
                "kpis", List.of(
                    mapOf("label", "最近流水", "value", String.valueOf(recentRecords.size()), "trend_direction", recentRecords.isEmpty() ? "flat" : "up"),
                    mapOf("label", "查询收入", "value", money(income), "trend_direction", income > 0 ? "up" : "flat"),
                    mapOf("label", "查询支出", "value", money(expense), "trend_direction", expense > 0 ? "up" : "flat")
                )
            ))
        );
        V2AgentDtos.ResultBlockDto tableBlock = new V2AgentDtos.ResultBlockDto(
            "table",
            "最近流水",
            toJsonNode(mapOf(
                "headers", List.of("单号", "类型", "分类", "金额", "往来方"),
                "rows", recentRecords.stream().map(item -> List.of(
                    safeText(item.getRecordNo(), "-"),
                    financeTypeLabel(item.getType()),
                    safeText(item.getCategory(), "-"),
                    money(safeDouble(item.getAmount())),
                    safeText(item.getPartnerName(), "-")
                )).toList(),
                "row_count", recentRecords.size()
            ))
        );
        V2AgentDtos.ResultBlockDto donutBlock = new V2AgentDtos.ResultBlockDto(
            "donut_chart",
            "收入支出占比",
            toJsonNode(mapOf(
                "title", "收入支出占比",
                "segments", List.of(
                    mapOf("name", "收入", "value", income, "color", "#34A853"),
                    mapOf("name", "支出", "value", expense, "color", "#FB8C00")
                )
            ))
        );

        List<V2AgentDtos.ResultBlockDto> blocks = List.of(kpiBlock, donutBlock, tableBlock);
        emitBlocks(emitter, runId, blocks);

        String answer = recentRecords.isEmpty()
            ? "当前账号下还没有资金流水数据。"
            : "我查到了最近 " + recentRecords.size() + " 条资金流水，查询收入 "
                + money(income) + "，查询支出 " + money(expense) + "。";
        ToolExecutionResult toolResult = new ToolExecutionResult(
            "finance_record_lookup",
            "最近流水 " + recentRecords.size() + " 条，收入 " + money(income) + "，支出 " + money(expense),
            toJsonNode(mapOf(
                "record_count", recentRecords.size(),
                "recent_income", money(income),
                "recent_expense", money(expense),
                "query_audit", audit.facts(),
                "recent_records", recentRecords.stream().limit(5).map(item -> mapOf(
                    "record_no", safeText(item.getRecordNo(), "-"),
                    "type", financeTypeLabel(item.getType()),
                    "category", safeText(item.getCategory(), "-"),
                    "amount", money(safeDouble(item.getAmount())),
                    "partner_name", safeText(item.getPartnerName(), "-")
                )).toList()
            ))
        );
        return new ResponsePayload(answer, blocks, List.of(toolResult));
    }

    private FinalAnswer buildFinalAnswer(String userMessage, ResponsePayload payload) {
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            return new FinalAnswer(appendFailureNotice(payload.answer(), payload.toolFailures()), "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        String synthesized = synthesizeAnswer(userMessage, payload.toolResults(), payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            payload.toolResults()
        );
        if (!longCatAnthropicClient.isConfigured()) {
            return new FinalAnswer(
                withRuleSummaryNotice(synthesizedWithFailures),
                "tool_query_rule_summary",
                longCatAnthropicClient.configurationStatus(),
                false
            );
        }
        String systemPrompt = """
            你是智慧记的 agentic AI 助手。你不能编造数据，只能基于服务端白名单工具返回的事实回答。
            回答要求：
            1. 先直接回答用户问题。
            2. 明确引用本轮查询到的关键数据。
            3. 给出 1-3 条可执行建议。
            4. 如果有工具查询失败，必须明确说明哪些查询失败，且不要用模拟数据替代。
            5. 不要输出 Markdown 表格；不要声称查询了未列出的数据。
            """;
        String toolFactsJson = serializeToolResults(payload.toolResults());
        String prompt = "用户问题：" + userMessage + "\n"
            + "已执行工具结果 JSON：" + toolFactsJson + "\n"
            + "失败工具 JSON：" + serializeToolFailures(payload.toolFailures()) + "\n"
            + "基于事实的初稿：" + synthesizedWithFailures;
        Optional<String> refined = longCatAnthropicClient.createJsonMessage(systemPrompt, prompt);
        return refined
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(answer -> appendFailureNotice(answer, payload.toolFailures()))
            .map(answer -> appendQueryBoundaryNotice(answer, payload.toolResults()))
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
        String runId
    ) {
        if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()
            && (payload.toolResults() == null || payload.toolResults().isEmpty())) {
            String failedAnswer = appendFailureNotice(payload.answer(), payload.toolFailures());
            return new FinalAnswer(failedAnswer, "tool_query_failed", "not_requested", false);
        }
        if (payload.toolResults() == null || payload.toolResults().isEmpty()) {
            return new FinalAnswer(payload.answer(), "unsupported_intent", "not_requested", false);
        }
        String synthesized = synthesizeAnswer(userMessage, payload.toolResults(), payload.answer());
        String synthesizedWithFailures = appendQueryBoundaryNotice(
            appendFailureNotice(synthesized, payload.toolFailures()),
            payload.toolResults()
        );
        if (!longCatAnthropicClient.isConfigured()) {
            String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
            return new FinalAnswer(
                ruleSummaryAnswer,
                "tool_query_rule_summary",
                longCatAnthropicClient.configurationStatus(),
                false
            );
        }
        if (!longCatAnthropicClient.supportsStreaming()) {
            String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
            return new FinalAnswer(
                ruleSummaryAnswer,
                "tool_query_rule_summary",
                longCatAnthropicClient.streamingUnavailableStatus(),
                false
            );
        }
        String systemPrompt = finalAnswerSystemPrompt();
        String prompt = finalAnswerUserPrompt(userMessage, payload, synthesizedWithFailures);
        StringBuilder streamedAnswer = new StringBuilder();
        Optional<String> streamed = longCatAnthropicClient.streamTextMessage(systemPrompt, prompt, delta -> {
            ensureRunActive(runId);
            streamedAnswer.append(delta);
            emitAnswerDeltaUnchecked(emitter, runId, delta, "model_stream");
        });
        return streamed
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(answer -> appendFailureNotice(answer, payload.toolFailures()))
            .map(answer -> appendQueryBoundaryNotice(answer, payload.toolResults()))
            .map(answer -> new FinalAnswer(answer, "tool_query_llm_streamed", "streaming", true))
            .orElseGet(() -> streamFallbackFinalAnswer(emitter, runId, streamedAnswer, synthesizedWithFailures, payload));
    }

    private FinalAnswer streamFallbackFinalAnswer(
        SseEmitter emitter,
        String runId,
        StringBuilder streamedAnswer,
        String synthesizedWithFailures,
        ResponsePayload payload
    ) {
        if (streamedAnswer != null && StringUtils.hasText(streamedAnswer.toString())) {
            String partialAnswer = appendFailureNotice(streamedAnswer.toString().trim(), payload.toolFailures());
            return new FinalAnswer(partialAnswer, "tool_query_llm_stream_interrupted", "stream_interrupted", true);
        }
        String ruleSummaryAnswer = withRuleSummaryNotice(synthesizedWithFailures);
        return new FinalAnswer(ruleSummaryAnswer, "tool_query_rule_summary", "stream_failed_or_empty", true);
    }

    private String withRuleSummaryNotice(String answer) {
        String normalized = StringUtils.hasText(answer) ? answer.trim() : "";
        if (normalized.startsWith(RULE_SUMMARY_NOTICE)) {
            return normalized;
        }
        return RULE_SUMMARY_NOTICE + "\n\n" + normalized;
    }

    private String finalAnswerSystemPrompt() {
        return """
            你是智慧记的 agentic AI 助手。你不能编造数据，只能基于服务端白名单工具返回的事实回答。
            回答要求：
            1. 先直接回答用户问题。
            2. 明确引用本轮查询到的关键数据。
            3. 给出 1-3 条可执行建议。
            4. 如果有工具查询失败，必须明确说明哪些查询失败，且不要用模拟数据替代。
            5. 不要输出 Markdown 表格；不要声称查询了未列出的数据。
            """;
    }

    private String finalAnswerUserPrompt(String userMessage, ResponsePayload payload, String synthesizedWithFailures) {
        return "用户问题：" + userMessage + "\n"
            + "已执行工具结果 JSON：" + serializeToolResults(payload.toolResults()) + "\n"
            + "失败工具 JSON：" + serializeToolFailures(payload.toolFailures()) + "\n"
            + "基于事实的初稿：" + synthesizedWithFailures;
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
        List<Map<String, Object>> items = new ArrayList<>();
        if (toolResults != null) {
            for (ToolExecutionResult result : toolResults) {
                Map<String, Object> audit = result.queryAudit();
                items.add(mapOf(
                    "label", result.toolName(),
                    "value", result.summary(),
                    "source", "tool:" + result.toolName(),
                    "tool_call_id", toolCallId(runId, result.toolName()),
                    "query_window", audit,
                    "is_truncated", Boolean.TRUE.equals(audit.get("is_truncated"))
                ));
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
        List<V2AgentDtos.AgentToolCallDto> calls = new ArrayList<>();
        if (payload.toolResults() != null) {
            for (ToolExecutionResult result : payload.toolResults()) {
                Map<String, Object> audit = result.queryAudit();
                calls.add(new V2AgentDtos.AgentToolCallDto(
                    toolCallId(runId, result.toolName()),
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
        }
        if (payload.toolFailures() != null) {
            for (ToolFailureResult failure : payload.toolFailures()) {
                calls.add(new V2AgentDtos.AgentToolCallDto(
                    toolCallId(runId, failure.toolName()),
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
        List<V2AgentDtos.AgentEvidenceRefDto> refs = new ArrayList<>();
        if (payload.toolResults() == null) {
            return refs;
        }
        int index = 1;
        for (ToolExecutionResult result : payload.toolResults()) {
            Map<String, Object> audit = result.queryAudit();
            List<Map<String, String>> evidenceItems = evidenceItemsFor(result);
            if (evidenceItems.isEmpty()) {
                refs.add(new V2AgentDtos.AgentEvidenceRefDto(
                    "evidence-" + index++,
                    toolCallId(runId, result.toolName()),
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
                    toolCallId(runId, result.toolName()),
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
        List<Map<String, String>> items = new ArrayList<>();
        switch (result.toolName()) {
            case "inventory_low_stock_lookup" -> addEvidenceItem(items, result, "低库存商品数", "low_stock_count", "个");
            case "product_catalog_lookup" -> {
                addEvidenceItem(items, result, "商品数", "product_count", "个");
                addEvidenceItem(items, result, "查询库存合计", "queried_stock_total", null);
                addEvidenceItem(items, result, "低库存商品数", "low_stock_count", "个");
            }
            case "customer_receivable_lookup" -> {
                addEvidenceItem(items, result, "欠款客户数", "customer_count", "个");
                addEvidenceItem(items, result, "Top10 应收合计", "top10_receivable_total", null);
            }
            case "supplier_payable_lookup" -> {
                addEvidenceItem(items, result, "应付供应商数", "supplier_count", "个");
                addEvidenceItem(items, result, "Top10 应付合计", "top10_payable_total", null);
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
        String text = value.isTextual() ? value.asText() : value.asText("");
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

    private String toolCallId(String runId, String toolName) {
        return (StringUtils.hasText(runId) ? runId : "run") + ":" + safeText(toolName, "tool");
    }

    private String auditIdFor(String runId) {
        return toolCallId(runId, "audit");
    }

    private String traceIdFor(String runId) {
        return toolCallId(runId, "trace");
    }

    private V2AgentDtos.AgentObservabilityDto observabilityFor(String runId, String auditId, String traceId) {
        String safeRunId = safeText(runId, "run");
        return new V2AgentDtos.AgentObservabilityDto(
            safeRunId,
            safeRunId,
            traceId,
            auditId,
            "agent-run:" + safeRunId
        );
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
        List<String> notices = new ArrayList<>();
        for (ToolExecutionResult result : toolResults) {
            for (String notice : queryBoundaryNotices(result)) {
                if (!notices.contains(notice)) {
                    notices.add(notice);
                }
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
        List<String> actions = new ArrayList<>();

        for (ToolExecutionResult toolResult : toolResults) {
            switch (toolResult.toolName()) {
                case "inventory_low_stock_lookup" -> {
                    int count = toolResult.facts().path("low_stock_count").asInt(0);
                    findings.add(count == 0
                        ? "库存侧暂时没有发现低于安全库存的商品。"
                        : "库存侧共发现 " + count + " 个低库存商品，需要优先补货。");
                    if (count > 0) {
                        actions.add("优先处理前 3 个低库存商品，避免影响接单和销售。");
                    }
                }
                case "product_catalog_lookup" -> {
                    int count = toolResult.facts().path("product_count").asInt(0);
                    String stockTotal = toolResult.facts().path("queried_stock_total").asText("0");
                    findings.add("商品侧查询到 " + count + " 个，库存合计 " + stockTotal + "。");
                }
                case "customer_receivable_lookup" -> {
                    int count = toolResult.facts().path("customer_count").asInt(0);
                    String total = toolResult.facts().path("top10_receivable_total").asText(money(0));
                    findings.add(count == 0
                        ? "客户侧没有明显应收欠款压力。"
                        : "客户侧重点欠款客户 " + count + " 个，Top10 应收合计 " + total + "。");
                    if (count > 0) {
                        actions.add("先跟进欠款最高的 2 到 3 位客户，缩短回款周期。");
                    }
                }
                case "supplier_payable_lookup" -> {
                    int count = toolResult.facts().path("supplier_count").asInt(0);
                    String total = toolResult.facts().path("top10_payable_total").asText(money(0));
                    findings.add(count == 0
                        ? "供应商侧暂时没有突出的应付压力。"
                        : "供应商侧重点应付 " + count + " 个，Top10 应付合计 " + total + "。");
                    if (count > 0) {
                        actions.add("结合回款节奏安排供应商付款，避免现金流过度前置。");
                    }
                }
                case "sales_overview_lookup" -> {
                    String salesAmount = toolResult.facts().path("sales_amount").asText(money(0));
                    String paidAmount = toolResult.facts().path("paid_amount").asText(money(0));
                    long salesCount = toolResult.facts().path("sales_count").asLong(0);
                    findings.add("近 7 天销售 " + salesCount + " 笔，销售额 " + salesAmount + "，回款 " + paidAmount + "。");
                }
                case "sale_order_lookup" -> {
                    int count = toolResult.facts().path("order_count").asInt(0);
                    int unpaidCount = toolResult.facts().path("unpaid_count").asInt(0);
                    String total = toolResult.facts().path("recent_total_amount").asText(money(0));
                    findings.add("销售单侧最近查询 " + count + " 条，销售额 " + total + "，未收清 " + unpaidCount + " 条。");
                }
                case "purchase_order_lookup" -> {
                    int count = toolResult.facts().path("order_count").asInt(0);
                    String total = toolResult.facts().path("recent_total_amount").asText(money(0));
                    findings.add("采购单侧最近查询 " + count + " 条，采购额 " + total + "。");
                }
                case "pay_order_lookup" -> {
                    int count = toolResult.facts().path("pay_order_count").asInt(0);
                    int pendingCount = toolResult.facts().path("pending_count").asInt(0);
                    String total = toolResult.facts().path("recent_total_amount").asText(money(0));
                    findings.add("付款单侧最近查询 " + count + " 条，付款额 " + total + "，待付款 " + pendingCount + " 条。");
                }
                case "finance_record_lookup" -> {
                    String income = toolResult.facts().path("recent_income").asText(money(0));
                    String expense = toolResult.facts().path("recent_expense").asText(money(0));
                    int count = toolResult.facts().path("record_count").asInt(0);
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

        List<String> dedupedActions = new ArrayList<>();
        for (String action : actions) {
            if (!dedupedActions.contains(action)) {
                dedupedActions.add(action);
            }
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

    private void emitBlocks(SseEmitter emitter, String runId, List<V2AgentDtos.ResultBlockDto> blocks) {
        if (emitter == null || runId == null) {
            return;
        }
        for (V2AgentDtos.ResultBlockDto block : blocks) {
            try {
                sendEvent(emitter, eventMap("result_block", mapOf(
                    "run_id", runId,
                    "block", block,
                    "timestamp", System.currentTimeMillis()
                )));
            } catch (IOException ex) {
                throw new IllegalStateException("发送 result_block 失败", ex);
            }
        }
    }

    private void emitPlan(SseEmitter emitter, String runId, AgentToolPlan plan) {
        if (emitter == null || runId == null || plan == null) {
            return;
        }
        try {
            String content = plan.tools().isEmpty()
                ? plan.rationale() + "：当前问题未匹配到已接入的真实查询工具"
                : plan.rationale() + "：" + String.join("、", plan.tools());
            sendEvent(emitter, eventMap("plan_delta", mapOf(
                "run_id", runId,
                "plan_source", plan.source(),
                "content", content,
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 plan_delta 失败", ex);
        }
    }

    private void emitToolStarted(SseEmitter emitter, String runId, String toolName, Map<String, Object> toolInput) {
        if (emitter == null || runId == null) {
            return;
        }
        try {
            sendEvent(emitter, eventMap("tool_started", mapOf(
                "run_id", runId,
                "tool_call_id", toolCallId(runId, toolName),
                "tool_name", toolName,
                "input_summary", toolInputSummary(toolName, toolInput),
                "query_window", queryWindowFor(toolInput),
                "tool_input", toolInput,
                "started_at", System.currentTimeMillis(),
                "audit_id", auditIdFor(runId),
                "trace_id", traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_started 失败", ex);
        }
    }

    private ToolAudit startToolAudit(
        SseEmitter emitter,
        String runId,
        String toolName,
        Map<String, Object> toolInput
    ) {
        ToolAudit audit = new ToolAudit(toolName, toolInput, System.currentTimeMillis());
        emitToolStarted(emitter, runId, toolName, toolInput);
        return audit;
    }

    private void emitToolCompleted(SseEmitter emitter, String runId, String toolName, String resultSummary) {
        emitToolCompleted(emitter, runId, toolName, resultSummary, null);
    }

    private void emitToolCompleted(
        SseEmitter emitter,
        String runId,
        String toolName,
        String resultSummary,
        ToolAudit audit
    ) {
        if (emitter == null || runId == null) {
            return;
        }
        try {
            Map<String, Object> payload = mapOf(
                "run_id", runId,
                "tool_call_id", toolCallId(runId, toolName),
                "tool_name", toolName,
                "result_summary", resultSummary,
                "audit_id", auditIdFor(runId),
                "trace_id", traceIdFor(runId),
                "timestamp", System.currentTimeMillis()
            );
            if (audit != null) {
                payload.putAll(audit.eventFields());
            }
            sendEvent(emitter, eventMap("tool_completed", payload));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_completed 失败", ex);
        }
    }

    private void emitToolFailed(SseEmitter emitter, String runId, String toolName, String safeMessage, long durationMs) {
        emitToolFailed(emitter, runId, toolName, safeMessage, durationMs, System.currentTimeMillis(), Map.of());
    }

    private void emitToolFailed(
        SseEmitter emitter,
        String runId,
        String toolName,
        String safeMessage,
        long durationMs,
        long startedAt,
        Map<String, Object> toolInput
    ) {
        if (emitter == null || runId == null) {
            return;
        }
        long completedAt = System.currentTimeMillis();
        try {
            sendEvent(emitter, eventMap("tool_failed", mapOf(
                "run_id", runId,
                "tool_call_id", toolCallId(runId, toolName),
                "tool_name", toolName,
                "input_summary", toolInputSummary(toolName, toolInput),
                "query_window", queryWindowFor(toolInput),
                "error_code", "TOOL_QUERY_FAILED",
                "safe_message", safeMessage,
                "error_summary", safeMessage,
                "duration_ms", Math.max(0L, durationMs),
                "started_at", startedAt,
                "completed_at", completedAt,
                "audit_id", auditIdFor(runId),
                "trace_id", traceIdFor(runId),
                "timestamp", completedAt
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 tool_failed 失败", ex);
        }
    }

    private static String toolInputSummary(String toolName, Map<String, Object> toolInput) {
        String label = switch (toolName) {
            case "inventory_low_stock_lookup" -> "查询当前账号低库存商品";
            case "product_catalog_lookup" -> "查询当前账号商品目录";
            case "customer_receivable_lookup" -> "查询当前账号客户应收余额";
            case "supplier_payable_lookup" -> "查询当前账号供应商应付余额";
            case "sales_overview_lookup" -> "汇总当前账号近 7 天经营信号";
            case "sale_order_lookup" -> "查询当前账号最近销售单";
            case "purchase_order_lookup" -> "查询当前账号最近采购单";
            case "pay_order_lookup" -> "查询当前账号最近付款单";
            case "finance_record_lookup" -> "查询当前账号最近资金流水";
            default -> "执行当前账号只读查询";
        };
        Map<String, Object> safeInput = toolInput == null ? Map.of() : toolInput;
        if (safeInput.isEmpty()) {
            return label;
        }
        return label + "，参数 " + safeInput;
    }

    private static Map<String, Object> queryWindowFor(Map<String, Object> toolInput) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("owner_scope", "current_owner");
        if (toolInput != null) {
            window.putAll(toolInput);
        }
        return window;
    }

    private void emitAnswerCompleted(
        SseEmitter emitter,
        String runId,
        String answer,
        String mode,
        String llmStatus
    ) throws IOException {
        if (!StringUtils.hasText(answer)) {
            return;
        }
        String auditId = auditIdFor(runId);
        String traceId = traceIdFor(runId);
        sendEvent(emitter, eventMap("answer_completed", mapOf(
            "run_id", runId,
            "answer", answer,
            "mode", mode,
            "llm_status", llmStatus,
            "audit_id", auditId,
            "trace_id", traceId,
            "observability", observabilityFor(runId, auditId, traceId),
            "timestamp", System.currentTimeMillis()
        )));
    }

    private void emitAnswerDeltaUnchecked(
        SseEmitter emitter,
        String runId,
        String delta,
        String deltaSource
    ) {
        if (!StringUtils.hasText(delta)) {
            return;
        }
        try {
            String auditId = auditIdFor(runId);
            String traceId = traceIdFor(runId);
            sendEvent(emitter, eventMap("answer_delta", mapOf(
                "run_id", runId,
                "delta", delta,
                "delta_source", deltaSource,
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 answer_delta 失败", ex);
        }
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        Object runId = payload.get("run_id");
        boolean cancellationEvent = "run_cancelled".equals(payload.get("event_type"));
        if (runId instanceof String runIdText) {
            if (!cancellationEvent) {
                ensureRunActive(runIdText);
            }
            ActiveAgentRun activeRun = activeRuns.get(runIdText);
            if (activeRun != null) {
                payload.putIfAbsent("conversation_id", activeRun.conversationId());
                payload.putIfAbsent("seq", activeRun.nextSeq());
                payload.putIfAbsent("event_id", runIdText + ":" + payload.get("seq"));
            }
        }
        payload.putIfAbsent("timestamp", System.currentTimeMillis());
        String payloadJson = objectMapper.writeValueAsString(payload);
        emitter.send(SseEmitter.event().data(payloadJson));
        if (runId instanceof String runIdText) {
            persistRunAuditEvent(runIdText, payload, payloadJson);
            incrementRunAuditEventCount(runIdText);
        }
    }

    private void createRunAudit(Long ownerUserId, Long conversationId, String runId, long startedAt) {
        AgentRunAuditEntity entity = new AgentRunAuditEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(conversationId);
        entity.setRunId(runId);
        entity.setAuditId(auditIdFor(runId));
        entity.setTraceId(traceIdFor(runId));
        entity.setStatus("running");
        entity.setStartedAt(startedAt);
        entity.setUpdatedAt(startedAt);
        entity.setEventCount(0);
        agentRunAuditRepository.save(entity);
    }

    private void ensureRunAuditStarted(Long ownerUserId, Long conversationId, String runId, long startedAt) {
        if (agentRunAuditRepository.findByRunId(runId).isEmpty()) {
            createRunAudit(ownerUserId, conversationId, runId, startedAt);
        }
    }

    private void finishRunAudit(
        Long ownerUserId,
        String runId,
        String status,
        String mode,
        String llmStatus,
        String planSource,
        Integer toolCount,
        String errorCode,
        String errorMessage,
        long completedAt
    ) {
        agentRunAuditRepository.findByRunIdAndOwnerUserId(runId, ownerUserId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setMode(mode);
            entity.setLlmStatus(llmStatus);
            entity.setPlanSource(planSource);
            entity.setToolCount(toolCount);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(truncate(errorMessage, 1000));
            entity.setCompletedAt(completedAt);
            entity.setUpdatedAt(completedAt);
            agentRunAuditRepository.save(entity);
        });
    }

    private void incrementRunAuditEventCount(String runId) {
        agentRunAuditRepository.findByRunId(runId)
            .ifPresent(entity -> {
                entity.setEventCount(Math.max(0, entity.getEventCount() == null ? 0 : entity.getEventCount()) + 1);
                entity.setUpdatedAt(System.currentTimeMillis());
                agentRunAuditRepository.save(entity);
            });
    }

    private void persistRunAuditEvent(String runId, Map<String, Object> payload, String payloadJson) {
        Object eventId = payload.get("event_id");
        Object seq = payload.get("seq");
        Object eventType = payload.get("event_type");
        if (!(eventId instanceof String eventIdText) || !(seq instanceof Number seqNumber) || !(eventType instanceof String eventTypeText)) {
            return;
        }
        AgentRunAuditEventEntity entity = new AgentRunAuditEventEntity();
        entity.setRunId(runId);
        entity.setEventId(eventIdText);
        entity.setSeq(seqNumber.intValue());
        entity.setEventType(eventTypeText);
        entity.setPayloadJson(payloadJson);
        entity.setCreatedAt(System.currentTimeMillis());
        agentRunAuditEventRepository.save(entity);
    }

    private V2AgentDtos.AgentRunAuditEventResponse toRunAuditEventResponse(AgentRunAuditEventEntity entity) {
        return new V2AgentDtos.AgentRunAuditEventResponse(
            entity.getEventId(),
            entity.getSeq(),
            entity.getEventType(),
            parseAuditPayload(entity.getPayloadJson()),
            entity.getCreatedAt()
        );
    }

    private JsonNode parseAuditPayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException ex) {
            return toJsonNode(mapOf(
                "parse_error", true,
                "raw", truncate(payloadJson, 1000)
            ));
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }

    private void ensureRunActive(String runId) {
        if (runId == null) {
            return;
        }
        ActiveAgentRun activeRun = activeRuns.get(runId);
        if (activeRun != null && activeRun.cancelled()) {
            throw new AgentRunCancelledException(runId);
        }
    }

    private void emitRunCancelled(SseEmitter emitter, String runId, String reason) {
        try {
            String auditId = auditIdFor(runId);
            String traceId = traceIdFor(runId);
            sendEvent(emitter, eventMap("run_cancelled", mapOf(
                "run_id", runId,
                "reason", reason,
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
        } catch (Exception ignored) {
            // 客户端可能已经断开；取消状态仍以 API 返回为准。
        }
    }

    private Map<String, Object> eventMap(String eventType, Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event_type", eventType);
        result.putAll(payload);
        return result;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private boolean isStreamEmissionFailure(Throwable ex) {
        if (ex == null) {
            return false;
        }
        String message = ex.getMessage();
        if (message != null && message.startsWith("发送 ")) {
            return true;
        }
        return isStreamEmissionFailure(ex.getCause());
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
        List<Map<String, Object>> items = new ArrayList<>();
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
        List<Map<String, Object>> items = new ArrayList<>();
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
        List<Map<String, Object>> items = new ArrayList<>();
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

    private SafetyDecision evaluateSafety(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if ((normalized.contains("别人的") || normalized.contains("其他账号") || normalized.contains("越权"))
            && containsAny(normalized, "数据", "订单", "客户", "库存")) {
            return new SafetyDecision(false, "请求疑似越权访问其他账号的数据");
        }
        if ((normalized.contains("drop table") || normalized.contains("delete from") || normalized.contains("truncate"))
            || (normalized.contains("清空") && normalized.contains("数据库"))
            || (normalized.contains("删除") && normalized.contains("所有数据"))) {
            return new SafetyDecision(false, "请求包含高风险破坏性数据库指令");
        }
        if (containsAny(normalized, "sql", "select *", "绕过", "token", "密码", "管理员")) {
            return modelSafetyReview(message)
                .filter(decision -> !decision.passed())
                .orElse(new SafetyDecision(false, "请求包含敏感查询或权限绕过风险"));
        }
        Optional<SafetyDecision> modelDecision = modelSafetyReview(message);
        if (modelDecision.isPresent() && !modelDecision.get().passed()) {
            return modelDecision.get();
        }
        return new SafetyDecision(true, null);
    }

    private Optional<SafetyDecision> modelSafetyReview(String message) {
        if (!longCatAnthropicClient.isConfigured()) {
            return Optional.empty();
        }
        String systemPrompt = """
            你是智慧记 AI 助手的安全审查器。只判断用户请求是否允许。
            允许：查询当前登录账号自己的经营数据、库存、客户欠款、销售概览。
            拦截：访问其他账号/其他租户数据、要求绕过权限、导出敏感凭据、直接执行 SQL、破坏性数据库操作、未确认的写操作。
            只输出 JSON：{"allowed":true,"reason":"..."}。
            """;
        return longCatAnthropicClient.createJsonMessage(systemPrompt, "用户请求：" + message)
            .flatMap(raw -> {
                try {
                    JsonNode root = objectMapper.readTree(extractJsonObject(raw));
                    boolean allowed = root.path("allowed").asBoolean(false);
                    String reason = root.path("reason").asText(allowed ? null : "模型安全审查未通过");
                    return Optional.of(new SafetyDecision(allowed, allowed ? null : reason));
                } catch (Exception ignored) {
                    return Optional.empty();
                }
            });
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

    private record SafetyDecision(boolean passed, String reason) {}

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
            this(answer, blocks, toolResults, List.of(), new AgentToolPlan(List.of(), "工具直接返回", "tool"));
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

    private record AgentToolPlan(List<String> tools, String rationale, String source) {
        private AgentToolPlan(List<String> tools, String rationale) {
            this(tools, rationale, "llm");
        }
    }

    private record FinalAnswer(String answer, String mode, String llmStatus, boolean modelAttempted) {}

    private record ToolExecutionResult(String toolName, String summary, JsonNode facts) {
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

    private static final class ActiveAgentRun {
        private final Long ownerUserId;
        private final String runId;
        private final Long conversationId;
        private final SseEmitter emitter;
        private final AtomicInteger eventSequence = new AtomicInteger(1);
        private volatile boolean cancelled;
        private volatile CompletableFuture<?> future;

        private ActiveAgentRun(Long ownerUserId, String runId, Long conversationId, SseEmitter emitter) {
            this.ownerUserId = ownerUserId;
            this.runId = runId;
            this.conversationId = conversationId;
            this.emitter = emitter;
        }

        private Long ownerUserId() {
            return ownerUserId;
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private Long conversationId() {
            return conversationId;
        }

        private int nextSeq() {
            return eventSequence.getAndIncrement();
        }

        private boolean cancelled() {
            return cancelled;
        }

        private void cancel() {
            this.cancelled = true;
        }

        private void attachFuture(CompletableFuture<?> future) {
            this.future = future;
            if (cancelled && future != null) {
                future.cancel(true);
            }
        }

        private boolean cancelFutureIfNotStarted() {
            CompletableFuture<?> currentFuture = future;
            if (currentFuture != null) {
                return currentFuture.cancel(true);
            }
            return false;
        }

        private void complete() {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 已断开的 SSE 不需要额外处理。
            }
        }
    }

    private static final class AgentRunCancelledException extends RuntimeException {
        private AgentRunCancelledException(String runId) {
            super("Agent run cancelled: " + runId);
        }
    }

    private static final class ToolAudit {
        private final String toolName;
        private final Map<String, Object> toolInput;
        private final long startedAt;
        private Integer returnedCount;
        private Integer totalCount;
        private Integer limit;
        private boolean truncated;

        private ToolAudit(String toolName, Map<String, Object> toolInput, long startedAt) {
            this.toolName = toolName;
            this.toolInput = toolInput == null ? Map.of() : toolInput;
            this.startedAt = startedAt;
        }

        private void markReturned(int returnedCount) {
            this.returnedCount = Math.max(0, returnedCount);
        }

        private void markLimitedResult(int returnedCount, int limit) {
            markLimitedResult(returnedCount, limit, returnedCount >= limit);
        }

        private void markLimitedResult(int returnedCount, int limit, boolean maybeTruncated) {
            this.returnedCount = Math.max(0, returnedCount);
            this.limit = Math.max(0, limit);
            this.truncated = maybeTruncated;
        }

        private void markListResult(List<?> sourceRows, int returnedCount, int limit) {
            this.returnedCount = Math.max(0, returnedCount);
            this.totalCount = sourceRows == null ? 0 : sourceRows.size();
            this.limit = Math.max(0, limit);
            this.truncated = this.totalCount > this.returnedCount;
        }

        private Map<String, Object> eventFields() {
            Map<String, Object> fields = new LinkedHashMap<>();
            long completedAt = System.currentTimeMillis();
            fields.put("input_summary", toolInputSummary(toolName, toolInput));
            fields.put("query_window", queryWindowFor(toolInput));
            fields.put("started_at", startedAt);
            fields.put("completed_at", completedAt);
            fields.put("duration_ms", Math.max(0L, completedAt - startedAt));
            if (returnedCount != null) {
                fields.put("returned_count", returnedCount);
            }
            if (totalCount != null) {
                fields.put("total_count", totalCount);
            }
            if (limit != null) {
                fields.put("limit", limit);
            }
            fields.put("is_truncated", truncated);
            fields.put("next_cursor", nextCursor());
            fields.put("evidence", evidenceSummary());
            return fields;
        }

        private Map<String, Object> facts() {
            Map<String, Object> fields = eventFields();
            fields.put("tool_name", toolName);
            fields.put("tool_input", toolInput);
            return fields;
        }

        private String nextCursor() {
            if (!truncated || limit == null || returnedCount == null) {
                return null;
            }
            return "offset:" + returnedCount + ":limit:" + limit;
        }

        private Map<String, Object> evidenceSummary() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "tool:" + toolName);
            evidence.put("scope", "current_owner");
            if (returnedCount != null) {
                evidence.put("returned_count", returnedCount);
            }
            if (totalCount != null) {
                evidence.put("total_count", totalCount);
            }
            evidence.put("is_truncated", truncated);
            return evidence;
        }
    }
}

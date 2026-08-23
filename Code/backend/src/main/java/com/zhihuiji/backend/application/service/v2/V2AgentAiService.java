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
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.repository.AgentConversationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentToolPlan;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.NativeToolCallBlock;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.FinalAnswer;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ResponsePayload;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolExecutionResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.ToolFailureResult;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentIterationPolicy;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentPromptCatalog;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentRunState;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTerminalStatus;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentTypes.AgentRunOutcome;
import com.zhihuiji.backend.application.service.v2.agent.component.AnswerSynthesizer;
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyDecision;
import com.zhihuiji.backend.application.service.v2.agent.component.SafetyGuard;
import com.zhihuiji.backend.application.service.v2.agent.component.SseStreamEmitter;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolInvocationIdentity;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilder;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionService;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionService.CompactionResult;
import com.zhihuiji.backend.application.service.v2.agent.memory.AgentMemoryService;
import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolArgumentsValidator;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolContext;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolExecutor;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolRegistry;
import com.zhihuiji.backend.application.service.v2.agent.tool.ToolResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
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
import jakarta.annotation.PreDestroy;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class V2AgentAiService {
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    // Keep the SSE lifetime longer than the provider read timeout so a slow
    // model response can finish or be cancelled without a false transport error.
    private static final long STREAM_TIMEOUT_MS = 180_000L;
    // One initial model decision plus at most two bounded continuations.
    // A malformed provider response must not turn into an unbounded database scan.
    private static final int MAX_TOOL_CALLS_PER_RUN = 12;
    private static final int SUPPLIER_SCAN_LIMIT = 50;
    private static final int OVERVIEW_SIGNAL_LIMIT = 5;
    private static final String RESULT_VISUALIZATION_TOOL = "result_visualization";
    private static final Set<String> ALWAYS_VISIBLE_BLOCK_TYPES = Set.of("draft", "draft_card");
    private static final Set<String> TABLE_BLOCK_TYPES = Set.of("table", "rank_list");
    private static final Set<String> CHART_BLOCK_TYPES = Set.of(
        "line_chart", "area_chart", "trend_chart", "bar_chart", "column_chart", "horizontal_bar_chart", "pie_chart"
    );
    private static final Set<String> TIMELINE_BLOCK_TYPES = Set.of("line_chart", "area_chart", "trend_chart", "timeline");

    private final CurrentOwnerService currentOwnerService;
    private final AgentConversationRepository agentConversationRepository;
    private final AgentMessageRepository agentMessageRepository;
    private final AgentDraftRepository agentDraftRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentNotificationRepository agentNotificationRepository;
    private final AgentRunAuditRepository agentRunAuditRepository;
    private final AgentRunAuditEventRepository agentRunAuditEventRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorageService mediaStorageService;
    private final ObjectMapper objectMapper;
    private final LongCatAnthropicClient longCatAnthropicClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RunAuditService runAuditService;
    private final SseStreamEmitter sseStreamEmitter;
    private final SafetyGuard safetyGuard;
    private final ToolPlanner toolPlanner;
    private final AnswerSynthesizer answerSynthesizer;
    private final ContextBuilder contextBuilder;
    private final ContextCompactionService contextCompactionService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 长期记忆服务（可选注入）：请求开始时按 owner/store 召回历史记忆注入上下文，
     * 回答完成后异步提取候选记忆。字段注入避免改动构造器，保持既有测试与调用方兼容。
     */
    @Autowired(required = false)
    private AgentMemoryService agentMemoryService;

    public V2AgentAiService(
        CurrentOwnerService currentOwnerService,
        AgentConversationRepository agentConversationRepository,
        AgentMessageRepository agentMessageRepository,
        AgentDraftRepository agentDraftRepository,
        AgentTaskRepository agentTaskRepository,
        AgentNotificationRepository agentNotificationRepository,
        AgentRunAuditRepository agentRunAuditRepository,
        AgentRunAuditEventRepository agentRunAuditEventRepository,
        MediaAssetRepository mediaAssetRepository,
        MediaStorageService mediaStorageService,
        ObjectMapper objectMapper,
        LongCatAnthropicClient longCatAnthropicClient,
        ToolRegistry toolRegistry,
        ToolExecutor toolExecutor,
        RunAuditService runAuditService,
        SseStreamEmitter sseStreamEmitter,
        SafetyGuard safetyGuard,
        ToolPlanner toolPlanner,
        AnswerSynthesizer answerSynthesizer,
        ContextBuilder contextBuilder,
        ContextCompactionService contextCompactionService
    ) {
        this.currentOwnerService = currentOwnerService;
        this.agentConversationRepository = agentConversationRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.agentDraftRepository = agentDraftRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentNotificationRepository = agentNotificationRepository;
        this.agentRunAuditRepository = agentRunAuditRepository;
        this.agentRunAuditEventRepository = agentRunAuditEventRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaStorageService = mediaStorageService;
        this.objectMapper = objectMapper;
        this.longCatAnthropicClient = longCatAnthropicClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.runAuditService = runAuditService;
        this.sseStreamEmitter = sseStreamEmitter;
        this.safetyGuard = safetyGuard;
        this.toolPlanner = toolPlanner;
        this.answerSynthesizer = answerSynthesizer;
        this.contextBuilder = contextBuilder;
        this.contextCompactionService = contextCompactionService;
    }

    @PreDestroy
    public void shutdownStreamExecutor() {
        streamExecutor.shutdownNow();
    }

    @Transactional(readOnly = true)
    public V2AgentDtos.AgentWorkbenchResponse getWorkbench() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentConversationEntity> conversations =
            agentConversationRepository.findAllWithMessagesByOwnerUserIdOrderByUpdatedAtDescIdDesc(
                ownerUserId,
                PageRequest.of(0, 5)
            );
        Map<Long, Integer> messageCounts = messageCountsByConversation(ownerUserId, conversations);
        List<V2AgentDtos.RecentConversationItem> recentConversations = conversations.stream()
                .map(conversation -> {
                    return new V2AgentDtos.RecentConversationItem(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getLastMessageAt() != null
                            ? conversation.getLastMessageAt()
                            : conversation.getUpdatedAt(),
                        messageCounts.getOrDefault(conversation.getId(), 0),
                        conversation.getLatestSummary()
                    );
                })
                .toList();

        return new V2AgentDtos.AgentWorkbenchResponse(
            "",
            List.of(),
            List.of(),
            recentConversations,
            List.of(),
            List.of(),
            null,
            "clean_entry_ready",
            null,
            List.of(),
            List.of()
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

    // Model/tool orchestration can run for tens of seconds. Keep it outside one
    // transaction so a failed read query cannot roll back the run audit parent
    // or make asynchronous audit events violate their foreign key.
    public V2AgentDtos.AgentChatResponse chat(V2AgentDtos.AgentChatRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        long runStartedAt = now;
        String message = normalizeRequired(request.message(), "message 不能为空");
        List<LongCatAnthropicClient.ImageInput> imageInputs = resolveImageInputs(ownerUserId, request.imageAssetIds());
        AgentConversationEntity conversation = resolveConversation(request.conversationId(), ownerUserId, message, now);
        String runId = UUID.randomUUID().toString();
        AgentMessageEntity userMessageEntity = saveMessage(
            ownerUserId,
            conversation.getId(),
            runId,
            "user",
            "text",
            buildStoredUserMessage(message, imageInputs.size()),
            null,
            now
        );
        Long userMessageId = userMessageEntity == null ? null : userMessageEntity.getId();

        runAuditService.createRunAudit(ownerUserId, conversation.getId(), runId, runStartedAt);
        RunAuditService.ActiveAgentRun auditRun = new RunAuditService.ActiveAgentRun(
            ownerUserId, runId, conversation.getId(), null
        );
        runAuditService.registerRun(auditRun);
        ResponsePayload payload = new ResponsePayload(
            List.of(),
            List.of(),
            List.of(),
            new AgentToolPlan(List.of(), "图片直接分析", "multimodal_direct", Map.of())
        );
        try {
            sseStreamEmitter.sendEvent(null, SseStreamEmitter.eventMap("run_started", mapOf(
                "run_id", runId,
                "conversation_id", conversation.getId(),
                "prompt", message,
                "audit_id", RunAuditService.auditIdFor(runId),
                "trace_id", RunAuditService.traceIdFor(runId),
                "observability", SseStreamEmitter.observabilityFor(
                    runId,
                    RunAuditService.auditIdFor(runId),
                    RunAuditService.traceIdFor(runId)
                ),
                "timestamp", System.currentTimeMillis()
            )));
        SafetyDecision safetyDecision = safetyGuard.evaluateSafety(message);
        String auditId = RunAuditService.auditIdFor(runId);
        String traceId = RunAuditService.traceIdFor(runId);
        if (!safetyDecision.passed()) {
            long blockedCompletedAt = System.currentTimeMillis();
            persistTerminalAssistantMessage(
                ownerUserId,
                conversation.getId(),
                runId,
                "blocked",
                safetyBlockedMessage(),
                blockedCompletedAt
            );
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
                null,
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

        FinalAnswer finalAnswer;
        long modelStartedAt = System.currentTimeMillis();
        AgentRunOutcome outcome;
        if (!imageInputs.isEmpty()) {
            finalAnswer = buildMultimodalDirectAnswer(message, imageInputs);
            outcome = new AgentRunOutcome(payload, AgentTerminalStatus.COMPLETED);
        } else {
            ContextBuilder.ContextPackage contextPackage = contextBuilder.build(
                ownerUserId,
                conversation.getId(),
                message,
                buildToolCatalogForCurrentRequest(),
                buildScopeDescriptionWithMemory(ownerUserId, conversation.getId(), message)
            );
            CompactionResult compaction = contextCompactionService.compactIfNeeded(contextPackage);
            // 压缩发生后，当前请求必须只使用新检查点边界之后的原始消息，
            // 不能继续使用压缩前的完整消息列表（否则压缩对本次请求无效）。
            List<AgentMessageEntity> history = compaction.occurred()
                ? afterBoundary(contextPackage.messagesAfterBoundary(), compaction.boundaryMessageId())
                : contextPackage.messagesAfterBoundary();
            String effectiveSummary = compaction.occurred()
                ? compaction.summaryPreview()
                : (StringUtils.hasText(contextPackage.checkpointSummary())
                    ? contextPackage.checkpointSummary()
                    : conversation.getLatestSummary());
            outcome = buildResponse(ownerUserId, conversation.getId(), message, history, effectiveSummary, null, runId);
            payload = outcome.payload();
            finalAnswer = buildFinalAnswer(message, payload, history, effectiveSummary);
            finalAnswer = applyTerminalAnswerSuffix(finalAnswer, outcome.terminalStatus(), outcome);
        }
        long completedAt = System.currentTimeMillis();
        long modelDurationMs = finalAnswer.modelAttempted()
            ? Math.max(0L, completedAt - modelStartedAt)
            : 0L;
        boolean llmFailed = isLlmFailure(finalAnswer);
        AgentTerminalStatus terminalStatus = llmFailed ? AgentTerminalStatus.FAILED : outcome.terminalStatus();
        if (llmFailed) {
            persistTerminalAssistantMessage(
                ownerUserId,
                conversation.getId(),
                runId,
                "failed",
                llmFailureMessage(finalAnswer),
                completedAt
            );
        } else if (StringUtils.hasText(finalAnswer.answer())) {
            persistAssistantResponse(
                ownerUserId,
                conversation,
                runId,
                finalAnswer.answer(),
                payload.blocks(),
                System.currentTimeMillis()
            );
        }
        // 回答完成后异步提取候选记忆（自动学习关闭时跳过；失败不阻塞主回答）。
        if (!llmFailed && StringUtils.hasText(finalAnswer.answer())) {
            extractMemoriesAfterAnswer(
                ownerUserId, conversation.getId(), userMessageId, message, finalAnswer.answer()
            );
        }
        runAuditService.finishRunAudit(
            ownerUserId,
            runId,
            terminalStatus.auditStatus(),
            finalAnswer.mode(),
            finalAnswer.llmStatus(),
            payload.planSource(),
            payload.toolResults().size(),
            llmFailed ? finalAnswer.mode().toUpperCase(java.util.Locale.ROOT) : outcome.errorCode(),
            llmFailed ? llmFailureMessage(finalAnswer) : outcome.safeMessage(),
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
            SseStreamEmitter.observabilityFor(runId, auditId, traceId),
            terminalStatus.name(),
            llmFailed ? finalAnswer.mode().toUpperCase(java.util.Locale.ROOT) : outcome.errorCode(),
            llmFailed ? llmFailureMessage(finalAnswer) : outcome.safeMessage(),
            outcome.completedTools(),
            outcome.missingTargetTools()
        );
        } catch (IOException ex) {
            IllegalStateException failure = new IllegalStateException("记录 Agent run_started 失败", ex);
            finalizeFailedNonStreamingRun(ownerUserId, conversation, runId, payload, failure);
            throw failure;
        } catch (RuntimeException ex) {
            finalizeFailedNonStreamingRun(ownerUserId, conversation, runId, payload, ex);
            throw ex;
        } finally {
            runAuditService.removeRun(runId);
        }
    }

    /**
     * 按终态给正式回答追加确定性的状态说明，保证 CONFIRMATION_PENDING 不被表述为
     * 已写入、EXHAUSTED 明确列出已完成与未完成目标。
     */
    private FinalAnswer applyTerminalAnswerSuffix(FinalAnswer finalAnswer, AgentTerminalStatus terminalStatus, AgentRunOutcome outcome) {
        if (finalAnswer == null || !StringUtils.hasText(finalAnswer.answer())) {
            return finalAnswer;
        }
        String suffix = switch (terminalStatus) {
            case CONFIRMATION_PENDING ->
                "\n\n[状态] 草稿已生成，等待你确认后才会写入正式业务数据；当前未创建任何正式单据。";
            case EXHAUSTED -> {
                StringBuilder text = new StringBuilder("\n\n[状态] 本次运行已达轮次预算上限");
                if (outcome != null && !outcome.completedTools().isEmpty()) {
                    text.append("；已完成：").append(String.join("、", outcome.completedTools()));
                }
                if (outcome != null && !outcome.missingTargetTools().isEmpty()) {
                    text.append("；未完成：").append(String.join("、", outcome.missingTargetTools()));
                }
                text.append("。未写入任何正式业务数据。");
                yield text.toString();
            }
            default -> null;
        };
        if (suffix == null) {
            return finalAnswer;
        }
        return new FinalAnswer(finalAnswer.answer() + suffix, finalAnswer.mode(), finalAnswer.llmStatus(), finalAnswer.modelAttempted());
    }

    public SseEmitter chatStream(V2AgentDtos.AgentChatRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        String message = normalizeRequired(request.message(), "message 不能为空");
        List<LongCatAnthropicClient.ImageInput> imageInputs = resolveImageInputs(ownerUserId, request.imageAssetIds());
        AgentConversationEntity conversation = resolveConversation(request.conversationId(), ownerUserId, message, now);
        String runId = UUID.randomUUID().toString();
        saveMessage(
            ownerUserId,
            conversation.getId(),
            runId,
            "user",
            "text",
            buildStoredUserMessage(message, imageInputs.size()),
            null,
            now
        );
        runAuditService.createRunAudit(ownerUserId, conversation.getId(), runId, now);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        RunAuditService.ActiveAgentRun activeRun = new RunAuditService.ActiveAgentRun(ownerUserId, runId, conversation.getId(), emitter);
        runAuditService.registerRun(activeRun);
        emitter.onCompletion(() -> runAuditService.removeRun(runId));
        emitter.onTimeout(() -> runAuditService.removeRun(runId));
        emitter.onError(ignored -> runAuditService.removeRun(runId));
        SecurityContext capturedSecurityContext = SecurityContextHolder.createEmptyContext();
        capturedSecurityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(capturedSecurityContext);
                runChatStream(ownerUserId, conversation, message, imageInputs, runId, emitter);
            } catch (RunAuditService.AgentRunCancelledException ignored) {
                // cancelRun 已向客户端发送 run_cancelled；worker 负责收尾关闭 emitter。
                activeRun.complete();
            } catch (Exception ex) {
                if (activeRun.cancelled()) {
                    // Cancellation owns the terminal state. Do not overwrite a
                    // cancelled audit with STREAM_ERROR when the provider call
                    // is interrupted at the same time.
                    activeRun.complete();
                    return;
                }
                runAuditService.finishRunAudit(
                    ownerUserId,
                    runId,
                    "failed",
                    null,
                    null,
                    null,
                    null,
                    "STREAM_ERROR",
                    streamFailureMessage(ex),
                    System.currentTimeMillis()
                );
                persistTerminalAssistantMessage(
                    ownerUserId,
                    conversation.getId(),
                    runId,
                    "failed",
                    streamFailureMessage(ex),
                    System.currentTimeMillis()
                );
                try {
                    sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("error", Map.of(
                        "run_id", runId,
                        "code", "STREAM_ERROR",
                        "message", streamFailureMessage(ex),
                        "timestamp", System.currentTimeMillis()
                    )));
                } catch (IOException ignored) {
                    // ignore
                }
                emitter.completeWithError(ex);
            } finally {
                SecurityContextHolder.clearContext();
                if (previousSecurityContext != null) {
                    SecurityContextHolder.setContext(previousSecurityContext);
                }
                runAuditService.removeRun(runId);
            }
        }, streamExecutor);
        future.whenComplete((ignored, error) -> {
            if (error == null) {
                activeRun.complete();
            }
        });
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
        persistTerminalAssistantMessage(
            ownerUserId,
            activeRun.conversationId(),
            normalizedRunId,
            "cancelled",
            "本次生成已取消，未完成的操作没有继续执行。",
            System.currentTimeMillis()
        );
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
            .findAllByRunIdAndOwnerUserIdOrderBySeqAsc(normalizedRunId, ownerUserId)
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
        List<LongCatAnthropicClient.ImageInput> imageInputs,
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
                "prompt", message,
                "audit_id", auditId,
                "trace_id", traceId,
                "observability", SseStreamEmitter.observabilityFor(runId, auditId, traceId),
                "timestamp", System.currentTimeMillis()
            )));
            runAuditService.ensureRunActive(runId);

            SafetyDecision safetyDecision = safetyGuard.evaluateSafety(ownerUserId, message);
            if (!safetyDecision.passed()) {
                String blockedMessage = safetyBlockedMessage();
                persistTerminalAssistantMessage(
                    ownerUserId,
                    conversation.getId(),
                    runId,
                    "blocked",
                    blockedMessage,
                    System.currentTimeMillis()
                );
                sseStreamEmitter.emitTerminalEvent(
                    emitter, runId, AgentTerminalStatus.BLOCKED,
                    null, "blocked", "not_requested", "safety",
                    "SAFETY_BLOCKED", blockedMessage, List.of(), List.of()
                );
                runAuditService.finishRunAudit(
                    ownerUserId,
                    runId,
                    AgentTerminalStatus.BLOCKED.auditStatus(),
                    "blocked",
                    "not_requested",
                    "safety",
                    0,
                    "SAFETY_BLOCKED",
                    null,
                    System.currentTimeMillis()
                );
                emitter.complete();
                return;
            }

            ResponsePayload payload;
            AgentRunOutcome outcome;
            FinalAnswer finalAnswer;
            final ResponsePayload[] payloadRef = new ResponsePayload[1];
            AtomicBoolean blocksEmitted = new AtomicBoolean(false);
            Runnable emitBlocksAfterVisibleAnswer = () -> {
                if (blocksEmitted.compareAndSet(false, true)) {
                    sseStreamEmitter.emitBlocks(emitter, runId, payloadRef[0].blocks());
                    runAuditService.ensureRunActive(runId);
                }
            };
            if (!imageInputs.isEmpty()) {
                payload = new ResponsePayload(
                    List.of(),
                    List.of(),
                    List.of(),
                    new AgentToolPlan(List.of(), "图片直接分析", "multimodal_direct", Map.of())
                );
                payloadRef[0] = payload;
                outcome = new AgentRunOutcome(payload, AgentTerminalStatus.COMPLETED);
                finalAnswer = buildMultimodalDirectAnswerForStream(message, imageInputs, emitter, runId, emitBlocksAfterVisibleAnswer);
            } else {
                ContextBuilder.ContextPackage contextPackage = contextBuilder.build(
                    ownerUserId,
                    conversation.getId(),
                    message,
                    buildToolCatalogForCurrentRequest(),
                    buildScopeDescriptionWithMemory(ownerUserId, conversation.getId(), message)
                );
                CompactionResult compaction = contextCompactionService.compactIfNeeded(contextPackage);
                // 流式路径同样只使用压缩后边界之后的原始消息，避免压缩结果不生效。
                List<AgentMessageEntity> history = compaction.occurred()
                    ? afterBoundary(contextPackage.messagesAfterBoundary(), compaction.boundaryMessageId())
                    : contextPackage.messagesAfterBoundary();
                String effectiveSummary = compaction.occurred()
                    ? compaction.summaryPreview()
                    : (StringUtils.hasText(contextPackage.checkpointSummary())
                        ? contextPackage.checkpointSummary()
                        : conversation.getLatestSummary());
                if (compaction.occurred() && StringUtils.hasText(runId)) {
                    sseStreamEmitter.emitContextCompacted(
                        emitter,
                        runId,
                        compaction.checkpoint() == null ? null : compaction.checkpoint().getId(),
                        compaction.boundaryMessageId(),
                        compaction.compactedCount(),
                        compaction.summaryPreview(),
                        compaction.inputTokenEstimate(),
                        compaction.outputTokenEstimate(),
                        compaction.reason(),
                        compaction.reused()
                    );
                }
                outcome = buildResponse(ownerUserId, conversation.getId(), message, history, effectiveSummary, emitter, runId);
                payload = outcome.payload();
                payloadRef[0] = payload;
                runAuditService.ensureRunActive(runId);
                finalAnswer = buildFinalAnswerForStream(
                    message,
                    payload,
                    emitter,
                    runId,
                    emitBlocksAfterVisibleAnswer,
                    history,
                    effectiveSummary
                );
                finalAnswer = applyTerminalAnswerSuffix(finalAnswer, outcome.terminalStatus(), outcome);
            }
            runAuditService.ensureRunActive(runId);
            if (isLlmFailure(finalAnswer)) {
                String failureCode = finalAnswer.mode().toUpperCase(java.util.Locale.ROOT);
                String failureMessage = llmFailureMessage(finalAnswer);
                persistTerminalAssistantMessage(
                    ownerUserId,
                    conversation.getId(),
                    runId,
                    "failed",
                    failureMessage,
                    System.currentTimeMillis()
                );
                runAuditService.finishRunAudit(
                    ownerUserId,
                    runId,
                    AgentTerminalStatus.FAILED.auditStatus(),
                    finalAnswer.mode(),
                    finalAnswer.llmStatus(),
                    payloadRef[0].planSource(),
                    payloadRef[0].toolResults().size(),
                    failureCode,
                    failureMessage,
                    System.currentTimeMillis()
                );
                sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("error", mapOf(
                    "run_id", runId,
                    "code", failureCode,
                    "safe_message", failureMessage,
                    "message", failureMessage,
                    "timestamp", System.currentTimeMillis()
                )));
                sseStreamEmitter.emitTerminalEvent(
                    emitter, runId, AgentTerminalStatus.FAILED,
                    null, finalAnswer.mode(), finalAnswer.llmStatus(), payloadRef[0].planSource(),
                    failureCode, failureMessage,
                    List.of(), List.of()
                );
                emitter.complete();
                return;
            }
            sseStreamEmitter.emitAnswerCompleted(
                emitter,
                runId,
                finalAnswer.answer(),
                finalAnswer.mode(),
                finalAnswer.llmStatus(),
                payloadRef[0].planSource()
            );
            emitBlocksAfterVisibleAnswer.run();
            persistAssistantResponse(
                ownerUserId,
                conversation,
                runId,
                finalAnswer.answer(),
                payloadRef[0].blocks(),
                System.currentTimeMillis()
            );
            // 流式回答完成后异步提取候选记忆（尽力而为；失败不阻塞）。
            if (!isLlmFailure(finalAnswer) && StringUtils.hasText(finalAnswer.answer())) {
                extractMemoriesAfterAnswer(
                    ownerUserId,
                    conversation.getId(),
                    resolveLatestUserMessageId(ownerUserId, conversation.getId(), runId),
                    message,
                    finalAnswer.answer()
                );
            }
            // 统一终态事件：run_completed（COMPLETED/CONFIRMATION_PENDING）/
            // run_exhausted；每个 run 只出现一次终态事件。
            sseStreamEmitter.emitTerminalEvent(
                emitter,
                runId,
                outcome.terminalStatus(),
                finalAnswer.answer(),
                finalAnswer.mode(),
                finalAnswer.llmStatus(),
                payloadRef[0].planSource(),
                outcome.errorCode(),
                outcome.safeMessage(),
                outcome.completedTools(),
                outcome.missingTargetTools()
            );
            runAuditService.finishRunAudit(
                ownerUserId,
                runId,
                outcome.terminalStatus().auditStatus(),
                finalAnswer.mode(),
                finalAnswer.llmStatus(),
                payloadRef[0].planSource(),
                payloadRef[0].toolResults().size(),
                outcome.errorCode(),
                outcome.safeMessage(),
                System.currentTimeMillis()
            );
            emitter.complete();
        } finally {
            if (registeredForDirectRun) {
                runAuditService.removeRun(runId);
            }
        }
    }

    private ResponsePayload buildResponse(Long ownerUserId, Long conversationId, String message) {
        return buildResponse(ownerUserId, conversationId, message, List.of(), null, null, null).payload();
    }

    private ResponsePayload buildResponse(Long ownerUserId, Long conversationId, String message, SseEmitter emitter, String runId) {
        return buildResponse(ownerUserId, conversationId, message, List.of(), null, emitter, runId).payload();
    }

    private AgentRunOutcome buildResponse(
        Long ownerUserId,
        Long conversationId,
        String message,
        List<AgentMessageEntity> history,
        String conversationSummary,
        SseEmitter emitter,
        String runId
    ) {
        AgentToolPlan plan = planTools(message, history, conversationSummary);
        emitPlan(emitter, runId, plan);
        boolean createIntentPlan = containsCreateOnlyTool(plan);

        // 复杂度轮次预算：按目标创建工具与依赖数量计算，受硬上限约束。
        int iterationBudget = AgentIterationPolicy.budgetFor(message, plan, toolRegistry);
        AgentRunState runState = new AgentRunState(
            runId, conversationId, ownerUserId, currentStoreIdOrNull(), iterationBudget
        );
        registerRequiredTargetTools(runState, message, plan);

        List<V2AgentDtos.ResultBlockDto> candidateBlocks = new ArrayList<>();
        List<ToolExecutionResult> toolResults = new ArrayList<>();
        List<ToolFailureResult> toolFailures = new ArrayList<>();
        Set<String> executedInvocationKeys = new LinkedHashSet<>();
        ToolExecutionBudget toolBudget = new ToolExecutionBudget(MAX_TOOL_CALLS_PER_RUN);
        int initialResultCount = toolResults.size();
        int initialFailureCount = toolFailures.size();
        executeToolPlan(ownerUserId, conversationId, emitter, runId, plan, candidateBlocks, toolResults, toolFailures,
            executedInvocationKeys, toolBudget, runState);
        recordTranscriptRound(runState, plan, initialResultCount, initialFailureCount, toolResults, toolFailures);
        emitDraftCreatedEvents(emitter, runId, toolResults.subList(initialResultCount, toolResults.size()));
        if (plan != null && "model_tool_selection_failed".equals(plan.source())) {
            return new AgentRunOutcome(
                new ResponsePayload(List.of(), List.of(), List.of(), plan),
                AgentTerminalStatus.FAILED,
                "MODEL_TOOL_SELECTION_FAILED",
                "模型未能选择有效工具",
                List.copyOf(runState.completedToolNames()),
                runState.missingTargetTools()
            );
        }

        // ReAct 多轮迭代：每轮工具执行后，由 LLM 判断是否需要补充查询。
        // 轮次受复杂度预算约束；预算耗尽且目标工具未完成时进入 EXHAUSTED。
        while (runState.hasIterationLeft()) {
            if (createIntentPlan && hasCompletedCreateTool(toolResults)) {
                break;
            }
            if (requestedResultVisualization(toolResults)) {
                break;
            }
            if (!longCatAnthropicClient.isConfigured()) {
                break;
            }
            int nextIteration = runState.iteration() + 1;
            Optional<AgentToolPlan> nextPlan = toolPlanner.planNextIteration(
                message, plan, toolResults, toolFailures, nextIteration);
            if (nextPlan.isEmpty()) {
                break;
            }
            AgentToolPlan iterationPlan = nextPlan.get();
            runState.advanceIteration();
            if (iterationPlan.tools() == null || iterationPlan.tools().isEmpty()) {
                // The model returned a terminal text-only continuation. Do not
                // send another tool-selection request after it decided that
                // the real facts are sufficient. Preserve the model text so
                // the answer layer does not call the provider a second time.
                plan = iterationPlan;
                break;
            }
            emitPlan(emitter, runId, iterationPlan);
            int iterationResultCount = toolResults.size();
            int iterationFailureCount = toolFailures.size();
            registerRequiredTargetTools(runState, message, iterationPlan);
            executeToolPlan(ownerUserId, conversationId, emitter, runId, iterationPlan, candidateBlocks, toolResults, toolFailures,
                executedInvocationKeys, toolBudget, runState);
            recordTranscriptRound(runState, iterationPlan, iterationResultCount, iterationFailureCount, toolResults, toolFailures);
            emitDraftCreatedEvents(emitter, runId, toolResults.subList(iterationResultCount, toolResults.size()));
            if (toolResults.size() == iterationResultCount
                && toolFailures.size() == iterationFailureCount) {
                // Do not spin when a provider repeats an invalid or already
                // completed call and the executor produces no new evidence.
                break;
            }
            plan = new AgentToolPlan(
                mergeTools(plan.tools(), iterationPlan.tools()),
                plan.rationale() + " + 迭代补充(" + runState.iteration() + ")",
                iterationPlan.source(),
                mergeToolParams(plan.toolParams(), iterationPlan.toolParams()),
                iterationPlan.nativeResponseId(),
                iterationPlan.nativeToolCallBlocks()
            );
            if (containsCreateOnlyTool(iterationPlan)) {
                createIntentPlan = true;
                if (hasCompletedCreateTool(toolResults)) {
                    break;
                }
            }
        }

        List<V2AgentDtos.ResultBlockDto> blocks = selectVisibleResultBlocks(toolResults, candidateBlocks);
        ResponsePayload payload = new ResponsePayload(blocks, toolResults, toolFailures, plan);
        return resolveRunOutcome(runState, message, createIntentPlan, toolResults, payload);
    }

    /**
     * 依据运行状态、目标工具完成情况与模型回答计算统一终态。
     *
     * <p>创建类任务：目标 CREATE_ONLY 工具成功生成草稿 → CONFIRMATION_PENDING；
     * 目标未完成且预算耗尽 → EXHAUSTED。查询类任务满足完成策略 → COMPLETED。
     */
    private AgentRunOutcome resolveRunOutcome(
        AgentRunState runState,
        String message,
        boolean createIntentPlan,
        List<ToolExecutionResult> toolResults,
        ResponsePayload payload
    ) {
        boolean hasCompletedCreateTool = hasCompletedCreateTool(toolResults);
        boolean writeTargetPending = !runState.missingTargetTools().isEmpty();
        if (createIntentPlan || writeTargetPending) {
            if (hasCompletedCreateTool || runState.anyTargetToolCompleted()) {
                return new AgentRunOutcome(
                    payload,
                    AgentTerminalStatus.CONFIRMATION_PENDING,
                    null,
                    "草稿已生成，等待用户确认后才会写入正式业务数据。",
                    List.copyOf(runState.completedToolNames()),
                    runState.missingTargetTools()
                );
            }
            // 查询完成但目标工具未完成：不返回成功语义。
            return new AgentRunOutcome(
                payload,
                AgentTerminalStatus.EXHAUSTED,
                "AGENT_ITERATION_EXHAUSTED",
                exhaustedSafeMessage(runState),
                List.copyOf(runState.completedToolNames()),
                runState.missingTargetTools()
            );
        }
        return new AgentRunOutcome(payload, AgentTerminalStatus.COMPLETED);
    }

    private String exhaustedSafeMessage(AgentRunState runState) {
        Set<String> completed = runState.completedToolNames();
        List<String> missing = runState.missingTargetTools();
        StringBuilder message = new StringBuilder("本次运行已达轮次预算上限。");
        if (!completed.isEmpty()) {
            message.append("已完成工具：").append(String.join("、", completed)).append("。");
        } else {
            message.append("未完成任何工具。");
        }
        if (!missing.isEmpty()) {
            message.append("未完成目标：").append(String.join("、", missing)).append("。");
        }
        message.append("未写入任何正式业务数据。");
        return message.toString();
    }

    /** 登记完成任务所需的目标工具（写目标 + 本轮计划中的 CREATE_ONLY 工具 + 海报目标）。 */
    private void registerRequiredTargetTools(AgentRunState runState, String message, AgentToolPlan plan) {
        String writeTarget = AgentPromptCatalog.targetWriteTool(message);
        if (StringUtils.hasText(writeTarget)) {
            runState.requireTargetTools(Set.of(writeTarget));
        }
        if (message != null && message.contains("海报")) {
            runState.requireTargetTools(Set.of("generate_poster_prompt"));
        }
        if (plan != null && plan.tools() != null) {
            for (String toolName : plan.tools()) {
                toolRegistry.getTool(toolName)
                    .filter(tool -> tool.type() == AgentTool.ToolType.CREATE_ONLY)
                    .ifPresent(tool -> runState.requireTargetTools(Set.of(tool.name())));
            }
        }
    }

    /**
     * 追加一轮原生 transcript：assistant tool call 与 tool result 按 call_id 配对。
     *
     * <p>本轮新增的 toolResults（fromResultIndex 起）与 toolFailures（fromFailureIndex 起）
     * 与计划中的 native tool call 块配对；缺少结果的调用记录为结构化失败，不静默丢弃。
     */
    private void recordTranscriptRound(
        AgentRunState runState,
        AgentToolPlan plan,
        int fromResultIndex,
        int fromFailureIndex,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures
    ) {
        if (runState == null || plan == null) {
            return;
        }
        List<AgentRunState.AssistantToolCall> assistantCalls = new ArrayList<>();
        if (plan.nativeToolCallBlocks() != null) {
            for (NativeToolCallBlock block : plan.nativeToolCallBlocks()) {
                if (block == null || !StringUtils.hasText(block.toolName())) {
                    continue;
                }
                assistantCalls.add(new AgentRunState.AssistantToolCall(
                    block.toolCallId(), block.toolName(), block.arguments()
                ));
            }
        }
        if (plan.tools() != null && assistantCalls.isEmpty()) {
            // JSON 兼容计划没有原生 call_id；按本轮执行顺序生成运行内身份。
            for (String toolName : plan.tools()) {
                if (!StringUtils.hasText(toolName)) {
                    continue;
                }
                assistantCalls.add(new AgentRunState.AssistantToolCall(null, toolName, null));
            }
        }
        List<AgentRunState.ToolMessage> toolMessages = new ArrayList<>();
        Set<String> pairedCallIds = new LinkedHashSet<>();
        if (toolResults != null) {
            for (int i = Math.max(0, fromResultIndex); i < toolResults.size(); i++) {
                ToolExecutionResult result = toolResults.get(i);
                if (result == null) {
                    continue;
                }
                toolMessages.add(new AgentRunState.ToolMessage(
                    result.toolCallId(), result.toolName(), "completed", result.summary()
                ));
                if (StringUtils.hasText(result.toolCallId())) {
                    pairedCallIds.add(result.toolCallId());
                }
            }
        }
        if (toolFailures != null) {
            for (int i = Math.max(0, fromFailureIndex); i < toolFailures.size(); i++) {
                ToolFailureResult failure = toolFailures.get(i);
                if (failure == null) {
                    continue;
                }
                toolMessages.add(new AgentRunState.ToolMessage(
                    failure.toolCallId(), failure.toolName(), "failed", failure.safeMessage()
                ));
                if (StringUtils.hasText(failure.toolCallId())) {
                    pairedCallIds.add(failure.toolCallId());
                }
            }
        }
        // 缺少配对结果的原生调用记录为结构化失败，不静默丢弃。
        for (AgentRunState.AssistantToolCall call : assistantCalls) {
            if (call.callId() != null && !pairedCallIds.contains(call.callId())) {
                toolMessages.add(new AgentRunState.ToolMessage(
                    call.callId(), call.toolName(), "missing_output", "调用缺少配对结果"
                ));
            }
        }
        runState.appendTranscriptRound(AgentRunState.TranscriptRound.of(
            runState.iteration(), assistantCalls, toolMessages
        ));
    }

    private Long currentStoreIdOrNull() {
        try {
            return currentOwnerService.findCurrentStoreId().orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * 构建当前请求的工具目录文本，用于 ContextBuilder 系统规则部分。
     *
     * <p>只读取已注册工具的 description 和用户语言提示；不包含凭据、价格或
     * 客户隐私字段。工具实际执行由 ToolExecutor 在受控边界内进行。
     */
    private String buildToolCatalogForCurrentRequest() {
        List<AgentTool> tools = new ArrayList<>();
        tools.addAll(toolRegistry.listReadOnlyTools());
        tools.addAll(toolRegistry.listCreateTools());
        if (tools.isEmpty()) {
            return "";
        }
        return com.zhihuiji.backend.application.service.v2.agent.component.AgentPromptCatalog.buildCatalog(tools, false);
    }

    /**
     * 构建 owner/store 作用域说明，用于 ContextBuilder 上下文包 B 部分。
     *
     * <p>每次请求重新从认证上下文构建；不含手机号、地址、凭据或完整认证载荷。
     */
    private String buildScopeDescription(Long ownerUserId) {
        Long storeId = currentStoreIdOrNull();
        StringBuilder sb = new StringBuilder();
        sb.append("当前 owner_user_id：").append(ownerUserId == null ? "unknown" : ownerUserId).append('\n');
        if (storeId != null) {
            sb.append("当前 store_id：").append(storeId).append('\n');
        }
        sb.append("数据作用域：仅当前账号和当前门店；不允许跨账号查询。\n");
        return sb.toString();
    }

    /**
     * 构建作用域说明并附加召回的历史记忆（标记为历史记忆，不可与实时业务查询混淆）。
     *
     * <p>召回失败只影响记忆块，不阻塞主回答（AgentMemoryService 内部已保证 owner 隔离）。
     */
    private String buildScopeDescriptionWithMemory(
        Long ownerUserId, Long conversationId, String message
    ) {
        String scope = buildScopeDescription(ownerUserId);
        if (agentMemoryService == null) {
            return scope;
        }
        try {
            List<AgentMemoryService.RecalledMemory> memories = agentMemoryService.recallMemories(
                ownerUserId, currentStoreIdOrNull(), message, 3
            );
            if (memories == null || memories.isEmpty()) {
                return scope;
            }
            StringBuilder sb = new StringBuilder(scope);
            sb.append("历史记忆（来自之前会话，仅供参考，不是当前实时业务数据）：\n");
            int shown = 0;
            for (AgentMemoryService.RecalledMemory memory : memories) {
                if (memory == null || memory.summary() == null || memory.summary().isBlank()) {
                    continue;
                }
                if (shown >= 3) {
                    break;
                }
                sb.append("- ").append(memory.summary()).append('\n');
                shown++;
            }
            return sb.toString();
        } catch (RuntimeException ex) {
            // 召回失败不能阻塞主回答：只丢弃记忆块。
            return scope;
        }
    }

    /**
     * 回答完成后异步提取候选记忆（自动学习关闭或服务未注入时跳过）。
     */
    private void extractMemoriesAfterAnswer(
        Long ownerUserId, Long conversationId, Long userMessageId,
        String userQuestion, String answer
    ) {
        if (agentMemoryService == null || userMessageId == null
            || !StringUtils.hasText(userQuestion)) {
            return;
        }
        try {
            agentMemoryService.extractMemoriesAsync(
                ownerUserId,
                currentStoreIdOrNull(),
                conversationId,
                userMessageId,
                userQuestion,
                answer,
                null
            );
        } catch (RuntimeException ex) {
            // 提取失败不影响主回答（服务内部已是异步 + 异常兜底）。
        }
    }

    /**
     * 解析最近一条属于当前运行的 user 消息 ID（用于流式路径回答后的记忆提取）。
     * 尽力而为：查询不到时返回 null（提取跳过）。
     */
    private Long resolveLatestUserMessageId(
        Long ownerUserId, Long conversationId, String runId
    ) {
        if (!StringUtils.hasText(runId)) {
            return null;
        }
        try {
            List<AgentMessageEntity> recent = agentMessageRepository
                .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
                    ownerUserId, conversationId, PageRequest.of(0, 30)
                );
            for (AgentMessageEntity message : recent) {
                if (message != null && "user".equalsIgnoreCase(message.getRole())
                    && runId.equals(message.getRunId())) {
                    return message.getId();
                }
            }
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
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
            if (tool.isPresent()
                && tool.get().type() == AgentTool.ToolType.CREATE_ONLY
                && toolRegistry.hasAllRequiredParameters(toolName, plan.toolParams().get(toolName))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompletedCreateTool(List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return false;
        }
        return toolResults.stream().anyMatch(result ->
            result != null
                && toolRegistry.getTool(result.toolName())
                    .map(tool -> tool.type() == AgentTool.ToolType.CREATE_ONLY)
                    .orElse(false)
        );
    }

    private List<V2AgentDtos.ResultBlockDto> selectVisibleResultBlocks(
        List<ToolExecutionResult> toolResults,
        List<V2AgentDtos.ResultBlockDto> candidateBlocks
    ) {
        if (candidateBlocks == null || candidateBlocks.isEmpty()) {
            return List.of();
        }
        boolean showDataVisualization = requestedResultVisualization(toolResults);
        String visualizationMode = requestedVisualizationMode(toolResults);
        List<V2AgentDtos.ResultBlockDto> visible = new ArrayList<>(candidateBlocks.size());
        Set<String> emittedVisualizationKinds = new LinkedHashSet<>();
        for (V2AgentDtos.ResultBlockDto block : candidateBlocks) {
            if (block == null || block.blockType() == null) {
                continue;
            }
            if (ALWAYS_VISIBLE_BLOCK_TYPES.contains(block.blockType())
                || (showDataVisualization && matchesVisualizationMode(block.blockType(), visualizationMode))) {
                if (showDataVisualization
                    && CHART_BLOCK_TYPES.contains(block.blockType())
                    && !hasMeaningfulChartData(block.data())) {
                    continue;
                }
                if (showDataVisualization) {
                    // Several real-data tools may return overlapping table/chart
                    // candidates. Keep one adaptive block for the requested kind
                    // instead of rendering duplicate facts in the client.
                    String visualizationKind = visualizationKind(block.blockType(), visualizationMode);
                    if (!emittedVisualizationKinds.add(visualizationKind)) {
                        continue;
                    }
                }
                visible.add(block);
            }
        }
        return visible;
    }

    private boolean hasMeaningfulChartData(JsonNode data) {
        if (data == null || data.isNull() || data.isMissingNode()) {
            return false;
        }
        if (data.isNumber()) {
            return Math.abs(data.asDouble()) > 0.0000001D;
        }
        if (data.isArray()) {
            for (JsonNode item : data) {
                if (hasMeaningfulChartData(item)) {
                    return true;
                }
            }
            return false;
        }
        if (data.isObject()) {
            var fields = data.fields();
            while (fields.hasNext()) {
                if (hasMeaningfulChartData(fields.next().getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String visualizationKind(String blockType, String mode) {
        if ("table".equals(mode) || "timeline".equals(mode)) {
            return mode;
        }
        if (TABLE_BLOCK_TYPES.contains(blockType)) {
            return "table";
        }
        if (CHART_BLOCK_TYPES.contains(blockType)) {
            return "chart";
        }
        if ("kpi_grid".equals(blockType)) {
            return "kpi";
        }
        if (TIMELINE_BLOCK_TYPES.contains(blockType)) {
            return "timeline";
        }
        return blockType;
    }

    private boolean matchesVisualizationMode(String blockType, String mode) {
        if ("table".equals(mode)) {
            return TABLE_BLOCK_TYPES.contains(blockType);
        }
        if ("chart".equals(mode)) {
            return CHART_BLOCK_TYPES.contains(blockType);
        }
        if ("kpi".equals(mode)) {
            return "kpi_grid".equals(blockType);
        }
        if ("timeline".equals(mode)) {
            return TIMELINE_BLOCK_TYPES.contains(blockType);
        }
        return true;
    }

    private boolean requestedResultVisualization(List<ToolExecutionResult> toolResults) {
        if (!hasSuccessfulRealDataQuery(toolResults) || toolResults == null) {
            return false;
        }
        for (ToolExecutionResult result : toolResults) {
            if (result != null && RESULT_VISUALIZATION_TOOL.equals(result.toolName())
                && result.facts() != null && result.facts().path("visualization_enabled").asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    private String requestedVisualizationMode(List<ToolExecutionResult> toolResults) {
        if (toolResults == null) {
            return "auto";
        }
        for (ToolExecutionResult result : toolResults) {
            if (result != null && RESULT_VISUALIZATION_TOOL.equals(result.toolName())
                && result.facts() != null && result.facts().path("visualization_enabled").asBoolean(false)) {
                String mode = result.facts().path("mode").asText("auto").toLowerCase(Locale.ROOT);
                return Set.of("auto", "table", "chart", "kpi", "timeline").contains(mode) ? mode : "auto";
            }
        }
        return "auto";
    }

    private boolean hasSuccessfulRealDataQuery(List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return false;
        }
        for (ToolExecutionResult result : toolResults) {
            if (isSuccessfulRealDataQuery(result)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuccessfulRealDataQuery(ToolExecutionResult result) {
        if (result == null || RESULT_VISUALIZATION_TOOL.equals(result.toolName())
            || result.facts() == null || !result.facts().isObject()
            || !result.facts().path("query_audit").isObject()) {
            return false;
        }
        return toolRegistry.getTool(result.toolName())
            .map(tool -> tool.type() == AgentTool.ToolType.READ_ONLY
                && result.facts().path("query_audit").path("returned_count").asInt(0) > 0)
            .orElse(false);
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
    private Optional<AgentToolPlan> planNextIteration(
        String message,
        AgentToolPlan previousPlan,
        List<ToolExecutionResult> toolResults,
        List<ToolFailureResult> toolFailures,
        int iteration
    ) {
        return toolPlanner.planNextIteration(message, previousPlan, toolResults, toolFailures, iteration);
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

    private void executeToolPlan(Long ownerUserId, Long conversationId, SseEmitter emitter, String runId, AgentToolPlan plan,
                                  List<V2AgentDtos.ResultBlockDto> blocks,
        List<ToolExecutionResult> toolResults, List<ToolFailureResult> toolFailures) {
        executeToolPlan(ownerUserId, conversationId, emitter, runId, plan, blocks, toolResults, toolFailures,
            new LinkedHashSet<>(), new ToolExecutionBudget(MAX_TOOL_CALLS_PER_RUN),
            new AgentRunState(runId, conversationId, ownerUserId, currentStoreIdOrNull(), AgentRunState.HARD_ITERATION_CAP));
    }

    private void executeToolPlan(Long ownerUserId, Long conversationId, SseEmitter emitter, String runId, AgentToolPlan plan,
                                  List<V2AgentDtos.ResultBlockDto> blocks,
                                  List<ToolExecutionResult> toolResults, List<ToolFailureResult> toolFailures,
                                  Set<String> executedInvocationKeys, ToolExecutionBudget toolBudget, AgentRunState runState) {
        if (runState == null) {
            runState = new AgentRunState(
                runId, conversationId, ownerUserId, currentStoreIdOrNull(), AgentRunState.HARD_ITERATION_CAP);
        }
        if (plan != null && "model_tool_selection_failed".equals(plan.source())) {
            int sequence = toolResults.size() + toolFailures.size() + 1;
            List<NativeToolCallBlock> suppressedBlocks = plan.nativeToolCallBlocks() == null
                ? List.of()
                : plan.nativeToolCallBlocks();
            for (NativeToolCallBlock block : suppressedBlocks) {
                if (block == null || !StringUtils.hasText(block.toolName())) {
                    continue;
                }
                emitToolSkipped(
                    emitter,
                    runId,
                    block.toolName(),
                    "model_tool_selection_failed",
                    parseToolArguments(block.arguments()),
                    sequence++,
                    block.toolCallId()
                );
                if (runState != null) {
                    runState.markAttempted(block.toolName());
                }
            }
            return;
        }
        Set<String> invocationKeys = executedInvocationKeys == null ? new LinkedHashSet<>() : executedInvocationKeys;
        ToolExecutionBudget budget = toolBudget == null
            ? new ToolExecutionBudget(MAX_TOOL_CALLS_PER_RUN)
            : toolBudget;
        for (ToolInvocation invocation : orderToolInvocations(plan)) {
            String tool = invocation.toolName();
            JsonNode params = invocation.params();
            int sequence = toolResults.size() + toolFailures.size() + 1;
            String modelToolCallId = invocation.modelToolCallId();
            if (runState != null) {
                runState.markAttempted(tool);
            }
            // 原始参数 JSON 解析失败时记录结构化失败，禁止用 {} 掩盖参数丢失。
            if (invocation.rawArgumentsInvalid()) {
                String invalidMessage = "模型原始参数不是合法 JSON（TOOL_ARGUMENTS_INVALID）";
                toolFailures.add(new ToolFailureResult(tool, invalidMessage, modelToolCallId, sequence));
                sseStreamEmitter.emitToolFailed(
                    emitter, runId, tool, invalidMessage,
                    0L, System.currentTimeMillis(), defaultToolInput(null),
                    sequence, modelToolCallId
                );
                continue;
            }
            String invocationKey = ToolInvocationIdentity.key(
                tool, null, params, objectMapper
            );
            if (!invocationKeys.add(invocationKey)) {
                emitToolSkipped(
                    emitter, runId, tool, "duplicate_tool_semantic_key", params,
                    sequence, modelToolCallId
                );
                continue;
            }
            // result_visualization 是模型在读取真实数据后才可调用的展示决策工具。
            // 它自身不查询数据库，也不能凭空开启结构化展示。
            if (RESULT_VISUALIZATION_TOOL.equals(tool)
                && (!hasSuccessfulRealDataQuery(toolResults) || requestedResultVisualization(toolResults))) {
                emitToolSkipped(emitter, runId, tool, "visualization_requires_new_real_facts", params,
                    sequence, invocation.modelToolCallId());
                continue;
            }
            if (!budget.tryAcquire()) {
                emitToolSkipped(emitter, runId, tool, "run_tool_budget_exhausted", params,
                    sequence, invocation.modelToolCallId());
                continue;
            }
            String toolCallId = StringUtils.hasText(invocation.modelToolCallId())
                ? invocation.modelToolCallId()
                : RunAuditService.toolCallId(runId, sequence, tool);
            Map<String, Object> toolInput = defaultToolInput(params);
            SseStreamEmitter.ToolAudit audit = sseStreamEmitter.startToolAudit(
                emitter, runId, tool, toolInput, sequence, invocation.modelToolCallId()
            );
            long startedAt = System.currentTimeMillis();
            try {
                runAuditService.ensureRunActive(runId);
                ResponsePayload payload = executePlannedTool(
                    ownerUserId,
                    conversationId,
                    emitter,
                    runId,
                    runState,
                    tool,
                    params,
                    toolCallId,
                    sequence
                );
                runAuditService.ensureRunActive(runId);
                if (payload != null) {
                    if (payload.toolFailures() != null && !payload.toolFailures().isEmpty()) {
                        ToolFailureResult failure = payload.toolFailures().get(0)
                            .withCallIdentity(toolCallId, sequence);
                        toolFailures.add(failure);
                        sseStreamEmitter.emitToolFailed(
                            emitter,
                            runId,
                            tool,
                            failure.safeMessage(),
                            System.currentTimeMillis() - startedAt,
                            startedAt,
                            toolInput,
                            sequence,
                            invocation.modelToolCallId()
                        );
                        continue;
                    }
                    populateToolAudit(audit, payload.toolResults());
                    blocks.addAll(payload.blocks());
                    toolResults.addAll(payload.toolResults());
                    if (runState != null) {
                        runState.recordToolCall();
                        runState.markCompleted(tool);
                    }
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
                toolFailures.add(new ToolFailureResult(tool, errorSummary, toolCallId, sequence));
                sseStreamEmitter.emitToolFailed(
                    emitter,
                    runId,
                    tool,
                    errorSummary,
                    System.currentTimeMillis() - startedAt,
                    startedAt,
                    toolInput,
                    sequence,
                    invocation.modelToolCallId()
                );
            }
        }
    }

    private void emitToolSkipped(
        SseEmitter emitter,
        String runId,
        String tool,
        String reason,
        JsonNode params,
        int sequence,
        String modelToolCallId
    ) {
        sseStreamEmitter.emitToolSkipped(
            emitter,
            runId,
            tool,
            reason,
            defaultToolInput(params),
            sequence,
            modelToolCallId
        );
    }

    private List<ToolInvocation> orderToolInvocations(AgentToolPlan plan) {
        if (plan == null) {
            return List.of();
        }
        List<ToolInvocation> regular = new ArrayList<>();
        List<ToolInvocation> visualization = new ArrayList<>();
        Set<String> representedToolNames = new LinkedHashSet<>();
        if (plan.nativeToolCallBlocks() != null) {
            for (NativeToolCallBlock block : plan.nativeToolCallBlocks()) {
                if (block == null || !StringUtils.hasText(block.toolName())) {
                    continue;
                }
                ToolInvocation invocation = parseInvocationArguments(
                    block.toolCallId(), block.toolName(), block.arguments()
                );
                representedToolNames.add(block.toolName());
                if (RESULT_VISUALIZATION_TOOL.equals(block.toolName())) {
                    visualization.add(invocation);
                } else {
                    regular.add(invocation);
                }
            }
        }
        if (plan.tools() != null) {
            for (String tool : plan.tools()) {
                if (!StringUtils.hasText(tool) || representedToolNames.contains(tool)) {
                    continue;
                }
                ToolInvocation invocation = new ToolInvocation(
                    null,
                    tool,
                    plan.toolParams() == null ? null : plan.toolParams().get(tool)
                );
                if (RESULT_VISUALIZATION_TOOL.equals(tool)) {
                    visualization.add(invocation);
                } else {
                    regular.add(invocation);
                }
            }
        }
        regular.addAll(visualization);
        return regular;
    }

    /**
     * 严格解析模型原始参数：非法 JSON 返回 rawArgumentsInvalid=true 的调用，
     * 由执行层记录 TOOL_ARGUMENTS_INVALID 结构化失败，禁止用 {} 掩盖。
     */
    private ToolInvocation parseInvocationArguments(String toolCallId, String toolName, String rawArguments) {
        if (!StringUtils.hasText(rawArguments)) {
            return new ToolInvocation(toolCallId, toolName, objectMapper.createObjectNode(), false);
        }
        try {
            JsonNode parsed = objectMapper.readTree(rawArguments);
            return new ToolInvocation(toolCallId, toolName, parsed == null ? objectMapper.createObjectNode() : parsed, false);
        } catch (JsonProcessingException ex) {
            return new ToolInvocation(toolCallId, toolName, objectMapper.createObjectNode(), true);
        }
    }

    private JsonNode parseToolArguments(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(arguments);
            return parsed == null ? objectMapper.createObjectNode() : parsed;
        } catch (JsonProcessingException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private ResponsePayload executePlannedTool(
        Long ownerUserId,
        Long conversationId,
        SseEmitter emitter,
        String runId,
        AgentRunState runState,
        String tool,
        JsonNode params,
        String toolCallId,
        int sequence
    ) {
        ToolExecutor.ExecutionOutcome outcome = toolExecutor.execute(
            runState, tool, params, null, conversationId, runId, emitter, objectMapper
        );
        if (!outcome.executed()) {
            ToolExecutor.GateDecision decision = outcome.decision();
            String message = decision == null || decision.safeMessage() == null
                ? "工具执行被拒绝"
                : "[" + decision.reasonCode() + "] " + decision.safeMessage();
            return new ResponsePayload(
                List.of(),
                List.of(),
                List.of(new ToolFailureResult(tool, message, toolCallId, sequence)),
                new AgentToolPlan(List.of(tool), "工具执行门拒绝", "tool", Map.of())
            );
        }
        return adaptToolResult(outcome.result(), tool, toolCallId, sequence);
    }

    /** 保留给旧的隔离测试调用方；主执行链统一走 {@link ToolExecutor#execute}。 */
    private Optional<ToolResult> executeRegisteredTool(
        Long ownerUserId,
        Long conversationId,
        SseEmitter emitter,
        String runId,
        String tool,
        JsonNode params
    ) {
        toolRegistry.getTool(tool).ifPresent(registered ->
            currentOwnerService.requirePermissions(registered.requiredPermission())
        );
        Long currentUserId = currentOwnerService.requireCurrentUserId();
        Long currentStoreId = currentOwnerService.findCurrentStoreId().orElse(null);
        ToolContext ctx = new ToolContext(
            ownerUserId,
            currentUserId,
            currentStoreId,
            conversationId,
            runId,
            emitter,
            objectMapper
        );
        return toolRegistry.executeTool(tool, ctx, params);
    }


    private Map<String, Object> defaultToolInput(JsonNode params) {
        return paramsToInputMap(params);
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
    private ResponsePayload adaptToolResult(ToolResult result, String toolName, String toolCallId, int sequence) {
        if (!result.success()) {
            return new ResponsePayload(
                List.of(),
                List.of(),
                List.of(new ToolFailureResult(
                    toolName,
                    safeText(result.errorMessage(), toolName + " 执行失败"),
                    toolCallId,
                    sequence
                )),
                new AgentToolPlan(List.of(toolName), "工具直接返回", "tool", Map.of())
            );
        }
        ToolExecutionResult toolResult = new ToolExecutionResult(
            toolName,
            result.toolSummary(),
            result.toolFacts(),
            result.insufficient(),
            toolCallId,
            sequence
        );
        return new ResponsePayload(result.blocks(), List.of(toolResult));
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

    private boolean isLlmFailure(FinalAnswer finalAnswer) {
        return finalAnswer != null
            && (!StringUtils.hasText(finalAnswer.answer())
                || "llm_required".equals(finalAnswer.mode())
                || "llm_answer_unavailable".equals(finalAnswer.mode()));
    }

    private String llmFailureMessage(FinalAnswer finalAnswer) {
        return "暂时无法完成这次请求，请稍后重试。";
    }

    private FinalAnswer buildMultimodalDirectAnswer(
        String userMessage,
        List<LongCatAnthropicClient.ImageInput> imageInputs
    ) {
        if (!longCatAnthropicClient.isConfigured()) {
            return new FinalAnswer("", "llm_required", longCatAnthropicClient.configurationStatus(), false);
        }
        String systemPrompt = """
            你是智慧记 AI 助手。当前用户上传了图片。
            先基于图片给出可见事实，再结合用户文字回答。
            看不清或信息不足时要明确说明，不要把图片内容伪装成系统真实业务数据。
            """.trim();
        Optional<String> answer = longCatAnthropicClient.createJsonMessage(systemPrompt, userMessage, imageInputs);
        return answer
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(value -> new FinalAnswer(value, "multimodal_direct_llm", "available", true))
            .orElseGet(() -> new FinalAnswer("", "llm_answer_unavailable", "failed_or_empty", true));
    }

    private FinalAnswer buildMultimodalDirectAnswerForStream(
        String userMessage,
        List<LongCatAnthropicClient.ImageInput> imageInputs,
        SseEmitter emitter,
        String runId,
        Runnable onFirstVisibleDelta
    ) {
        if (!longCatAnthropicClient.isConfigured()) {
            return new FinalAnswer("", "llm_required", longCatAnthropicClient.configurationStatus(), false);
        }
        String systemPrompt = """
            你是智慧记 AI 助手。当前用户上传了图片。
            先基于图片给出可见事实，再结合用户文字回答。
            看不清或信息不足时要明确说明，不要把图片内容伪装成系统真实业务数据。
            """.trim();
        SseStreamEmitter.AnswerDeltaBatcher answerDeltaBatcher = sseStreamEmitter.newAnswerDeltaBatcher(emitter, runId);
        Optional<String> streamed = longCatAnthropicClient.streamTextMessage(
            systemPrompt,
            userMessage,
            imageInputs,
            runId,
            delta -> {
                runAuditService.ensureRunActive(runId);
                boolean emittedVisibleDelta = answerDeltaBatcher.accept(delta, "model_stream");
                if (emittedVisibleDelta) {
                    onFirstVisibleDelta.run();
                }
            }
        );
        answerDeltaBatcher.flush();
        return streamed
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(answer -> new FinalAnswer(
                answer,
                "multimodal_direct_streamed",
                "completed",
                true
            ))
            .orElseGet(() -> {
                Optional<String> retry = longCatAnthropicClient.createJsonMessage(systemPrompt, userMessage, imageInputs);
                if (retry.isPresent() && StringUtils.hasText(retry.get())) {
                    String answer = retry.get().trim();
                    answerSynthesizer.emitModelAnswerDeltas(emitter, runId, answer, "model_stream", onFirstVisibleDelta);
                    return new FinalAnswer(answer, "multimodal_direct_llm", "non_stream_retry", true);
                }
                return new FinalAnswer("", "llm_answer_unavailable", "failed_or_empty", true);
            });
    }

    private List<AgentMessageEntity> loadRecentHistory(Long ownerUserId, Long conversationId, int limit) {
        return answerSynthesizer.loadRecentHistory(ownerUserId, conversationId, limit);
    }

    /**
     * 保留边界之后（id > boundaryMessageId）的原始消息。
     *
     * <p>压缩检查点的边界消息本身已经压缩进摘要，边界之后的消息仍以原始形式
     * 注入模型；边界为 null 或列表为空时原样返回。
     */
    private static List<AgentMessageEntity> afterBoundary(
        List<AgentMessageEntity> messages, Long boundaryMessageId
    ) {
        if (messages == null || messages.isEmpty() || boundaryMessageId == null) {
            return messages == null ? List.of() : messages;
        }
        return messages.stream()
            .filter(message -> message != null && message.getId() != null
                && message.getId() > boundaryMessageId)
            .toList();
    }

    private List<V2AgentDtos.AgentToolCallDto> toToolCallDtos(String runId, ResponsePayload payload) {
        List<ToolExecutionResult> effectiveToolResults = payload.toolResults() == null
            ? List.of()
            : payload.toolResults();
        int estimatedSize = effectiveToolResults.size()
            + (payload.toolFailures() == null ? 0 : payload.toolFailures().size());
        List<V2AgentDtos.AgentToolCallDto> calls = new ArrayList<>(estimatedSize);
        int fallbackSequence = 1;
        for (ToolExecutionResult result : effectiveToolResults) {
            Map<String, Object> audit = result.queryAudit();
            int sequence = result.sequence() == null || result.sequence() <= 0
                ? fallbackSequence
                : result.sequence();
            calls.add(new V2AgentDtos.AgentToolCallDto(
                sequence,
                result.toolCallId() == null
                    ? RunAuditService.toolCallId(runId, sequence, result.toolName())
                    : result.toolCallId(),
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
            fallbackSequence = Math.max(fallbackSequence, sequence + 1);
        }
        if (payload.toolFailures() != null) {
            for (ToolFailureResult failure : payload.toolFailures()) {
                int sequence = failure.sequence() == null || failure.sequence() <= 0
                    ? fallbackSequence
                    : failure.sequence();
                calls.add(new V2AgentDtos.AgentToolCallDto(
                    sequence,
                    failure.toolCallId() == null
                        ? RunAuditService.toolCallId(runId, sequence, failure.toolName())
                        : failure.toolCallId(),
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
                fallbackSequence = Math.max(fallbackSequence, sequence + 1);
            }
        }
        return calls;
    }

    private List<V2AgentDtos.AgentEvidenceRefDto> toEvidenceRefs(String runId, ResponsePayload payload) {
        List<ToolExecutionResult> effectiveToolResults = payload.toolResults() == null
            ? List.of()
            : payload.toolResults();
        List<V2AgentDtos.AgentEvidenceRefDto> refs = new ArrayList<>(
            effectiveToolResults.size() * 2
        );
        if (effectiveToolResults.isEmpty()) {
            return refs;
        }
        int index = 1;
        int fallbackSequence = 1;
        for (ToolExecutionResult result : effectiveToolResults) {
            if (RESULT_VISUALIZATION_TOOL.equals(result.toolName())) {
                continue;
            }
            Map<String, Object> audit = result.queryAudit();
            int sequence = result.sequence() == null || result.sequence() <= 0
                ? fallbackSequence
                : result.sequence();
            String toolCallId = result.toolCallId() == null
                ? RunAuditService.toolCallId(runId, sequence, result.toolName())
                : result.toolCallId();
            List<Map<String, String>> evidenceItems = evidenceItemsFor(result);
            if (evidenceItems.isEmpty()) {
                refs.add(new V2AgentDtos.AgentEvidenceRefDto(
                    "evidence-" + index++,
                    toolCallId,
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
                    toolCallId,
                    result.toolName(),
                    item.get("label"),
                    item.get("value"),
                    toJsonNode(audit),
                    asBoolean(audit.get("is_truncated"))
                ));
            }
            fallbackSequence = Math.max(fallbackSequence, sequence + 1);
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
        persistAssistantResponse(ownerUserId, conversation, null, answer, blocks, now);
    }

    private void persistAssistantResponse(
        Long ownerUserId,
        AgentConversationEntity conversation,
        String runId,
        String answer,
        List<V2AgentDtos.ResultBlockDto> blocks,
        long now
    ) {
        saveMessage(ownerUserId, conversation.getId(), runId, "assistant", "text", answer, serializeBlocks(blocks), now);
        conversation.setLatestSummary(trimSummary(answer));
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        agentConversationRepository.save(conversation);
    }

    private void persistTerminalAssistantMessage(
        Long ownerUserId,
        Long conversationId,
        String runId,
        String messageType,
        String content,
        long now
    ) {
        // Terminal messages are status records, not synthetic successful answers.
        saveMessage(ownerUserId, conversationId, runId, "assistant", messageType, content, null, now);
        agentConversationRepository.findByIdAndOwnerUserId(conversationId, ownerUserId).ifPresent(conversation -> {
            conversation.setLatestSummary(trimSummary(content));
            conversation.setLastMessageAt(now);
            conversation.setUpdatedAt(now);
            agentConversationRepository.save(conversation);
        });
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
        return saveMessage(
            ownerUserId,
            conversationId,
            null,
            role,
            messageType,
            content,
            structuredDataJson,
            createdAt
        );
    }

    private AgentMessageEntity saveMessage(
        Long ownerUserId,
        Long conversationId,
        String runId,
        String role,
        String messageType,
        String content,
        String structuredDataJson,
        long createdAt
    ) {
        AgentMessageEntity entity = new AgentMessageEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(conversationId);
        entity.setRunId(runId);
        entity.setRole(role);
        entity.setMessageType(messageType);
        entity.setContent(content);
        entity.setStructuredDataJson(structuredDataJson);
        entity.setCreatedAt(createdAt);
        return agentMessageRepository.save(entity);
    }

    private String safetyBlockedMessage() {
        return "本次请求未执行：安全策略已拦截该请求。";
    }

    private String streamFailureMessage(Exception ex) {
        String rawMessage = ex == null ? null : ex.getMessage();
        String message = StringUtils.hasText(rawMessage) ? redactSensitiveText(rawMessage) : "流式请求未完成。";
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }

    private void finalizeFailedNonStreamingRun(
        Long ownerUserId,
        AgentConversationEntity conversation,
        String runId,
        ResponsePayload payload,
        RuntimeException failure
    ) {
        long completedAt = System.currentTimeMillis();
        String safeMessage = nonStreamingFailureMessage(failure);
        try {
            persistTerminalAssistantMessage(
                ownerUserId,
                conversation.getId(),
                runId,
                "failed",
                "暂时无法完成这次请求，请稍后重试。",
                completedAt
            );
        } catch (RuntimeException ignored) {
            // Preserve the original HTTP error when terminal-message persistence fails.
        }
        try {
            runAuditService.finishRunAudit(
                ownerUserId,
                runId,
                "failed",
                "runtime_exception",
                "failed",
                payload == null || payload.planSource() == null ? "unknown" : payload.planSource(),
                payload == null || payload.toolResults() == null ? 0 : payload.toolResults().size(),
                "AGENT_RUN_FAILED",
                safeMessage,
                completedAt
            );
        } catch (RuntimeException ignored) {
            // Preserve the original HTTP error when audit finalization fails.
        }
    }

    private String nonStreamingFailureMessage(RuntimeException failure) {
        String type = failure == null || failure.getClass().getSimpleName().isBlank()
            ? "RuntimeException"
            : failure.getClass().getSimpleName();
        String rawMessage = failure == null ? null : failure.getMessage();
        String message = StringUtils.hasText(rawMessage) ? redactSensitiveText(rawMessage) : "运行阶段未提供错误详情";
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() > 900) {
            message = message.substring(0, 900) + "...";
        }
        return type + ": " + message;
    }

    private String buildStoredUserMessage(String message, int imageCount) {
        if (imageCount <= 0) {
            return message;
        }
        return message + "\n\n[已附带图片 " + imageCount + " 张]";
    }

    private List<LongCatAnthropicClient.ImageInput> resolveImageInputs(Long ownerUserId, List<Long> imageAssetIds) {
        if (imageAssetIds == null || imageAssetIds.isEmpty()) {
            return List.of();
        }
        if (imageAssetIds.size() > 9) {
            throw new IllegalArgumentException("最多支持 9 张图片附件");
        }
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<>();
        for (Long imageAssetId : imageAssetIds) {
            if (imageAssetId != null && imageAssetId > 0L) {
                normalizedIds.add(imageAssetId);
            }
        }
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<LongCatAnthropicClient.ImageInput> imageInputs = new ArrayList<>(normalizedIds.size());
        for (Long imageAssetId : normalizedIds) {
            MediaAssetEntity asset = mediaAssetRepository.findByIdAndOwnerUserId(imageAssetId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("图片资源不存在: " + imageAssetId));
            if (!StringUtils.hasText(asset.getMimeType()) || !asset.getMimeType().startsWith("image/")) {
                throw new IllegalArgumentException("仅支持图片附件: " + imageAssetId);
            }
            try {
                byte[] bytes = mediaStorageService.load(asset.getObjectKey());
                String dataUrl = "data:" + asset.getMimeType() + ";base64," + Base64.getEncoder().encodeToString(bytes);
                imageInputs.add(new LongCatAnthropicClient.ImageInput(asset.getMimeType(), dataUrl));
            } catch (Exception ex) {
                throw new IllegalStateException("读取图片附件失败: " + imageAssetId, ex);
            }
        }
        return imageInputs;
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
        if (runId == null || plan == null
            || plan.tools() == null || plan.tools().isEmpty()
            || !StringUtils.hasText(plan.rationale())) {
            return;
        }
        try {
            String content = plan.rationale() + "：" + String.join("、", plan.tools());
            sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("plan_delta", mapOf(
                "run_id", runId,
                "plan_source", plan.source(),
                "selection_origin", plan.selectionOrigin(),
                "tool_choice_mode", plan.toolChoiceMode(),
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

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Map<Long, Integer> messageCountsByConversation(
        Long ownerUserId,
        List<AgentConversationEntity> conversations
    ) {
        if (conversations == null || conversations.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = conversations.stream()
            .map(AgentConversationEntity::getId)
            .filter(java.util.Objects::nonNull)
            .toList();
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (Object[] row : agentMessageRepository.countByOwnerUserIdAndConversationIdInGroupBy(
            ownerUserId,
            conversationIds
        )) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long conversationId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            counts.put(conversationId, count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
        }
        return counts;
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

    private record ToolInvocation(String modelToolCallId, String toolName, JsonNode params,
                                  boolean rawArgumentsInvalid) {
        ToolInvocation(String modelToolCallId, String toolName, JsonNode params) {
            this(modelToolCallId, toolName, params, false);
        }
    }

    private static final class ToolExecutionBudget {
        private final int limit;
        private int used;

        private ToolExecutionBudget(int limit) {
            this.limit = Math.max(0, limit);
        }

        private boolean tryAcquire() {
            if (used >= limit) {
                return false;
            }
            used++;
            return true;
        }
    }
}

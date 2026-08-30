package com.zhihuiji.backend.application.service.v2.agent.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.v2.agent.component.AgentPromptCatalog;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 上下文构建器（plan 6.2 / 6.3）。
 *
 * <p>每次模型请求都由本组件生成上下文包，按计划文档 6.2 节的顺序组织：
 * <pre>
 * A. 系统规则和安全约束
 * B. 当前 owner/store 作用域说明
 * C. 最新有效会话检查点
 * D. 检查点边界之后的原始完整轮次
 * E. 当前轮工具调用和工具结果（由调用方注入）
 * F. 当前用户问题
 * G. 输出格式和完成策略（由调用方注入）
 * </pre>
 *
 * <p>预算计算公式（6.3 节）：
 * <pre>
 * usableWindow = min(providerWindow, configuredMaximum)
 * historyBudget = usableWindow
 *   - systemBudget(10%)
 *   - scopeBudget(3%)
 *   - currentQuestionBudget(8%)
 *   - toolResultBudget(20%)
 *   - reservedOutputBudget(15%)
 *   - safetyMargin(10%)
 * </pre>
 *
 * <p>系统规则、owner/store 作用域、当前用户问题、未完成工具调用和待确认草稿
 * 不能被静默截断；预算不足时优先压缩已完成历史轮次。
 */
@Component
public class ContextBuilder {
    /** 系统规则预算比例。 */
    public static final double SYSTEM_BUDGET_RATIO = 0.10;
    /** owner/store 作用域预算比例。 */
    public static final double SCOPE_BUDGET_RATIO = 0.03;
    /** 当前用户问题预算比例。 */
    public static final double CURRENT_QUESTION_RATIO = 0.08;
    /** 当前轮工具结果预算比例。 */
    public static final double TOOL_RESULT_RATIO = 0.20;
    /** 正式回答预留预算比例。 */
    public static final double RESERVED_OUTPUT_RATIO = 0.15;
    /** 安全余量比例。 */
    public static final double SAFETY_MARGIN_RATIO = 0.10;
    /** 历史消息与检查点预算比例（剩余）。 */
    public static final double HISTORY_RATIO = 1.0
        - SYSTEM_BUDGET_RATIO - SCOPE_BUDGET_RATIO - CURRENT_QUESTION_RATIO
        - TOOL_RESULT_RATIO - RESERVED_OUTPUT_RATIO - SAFETY_MARGIN_RATIO;

    /** 估算降级时的额外安全余量提升（按窗口比例叠加）。 */
    public static final double DEGRADED_SAFETY_MARGIN_BOOST = 0.10;

    /** 兼容旧调用方的页大小常量；不再参与历史加载或压缩触发。 */
    @Deprecated
    public static final int HISTORY_MESSAGE_LIMIT = 24;
    /** 压缩触发阈值：已用预算占可用预算的比例。 */
    public static final double COMPACTION_THRESHOLD_RATIO = 0.70;
    /** 旧版本保护状态读取上限；当前查询不按固定数量截断。 */
    @Deprecated
    public static final int MAX_PENDING_DRAFTS = 64;
    private static final int MAX_SCOPE_TEXT_LENGTH = 1_200;
    private static final int MAX_PROTECTED_TEXT_LENGTH = 160;
    private static final Pattern SENSITIVE_CONTEXT_PATTERN = Pattern.compile(
        "(?i)(?:1[3-9]\\d{9}|sk-[a-z0-9_-]{6,}|eyJ[a-z0-9_-]{10,}\\.[a-z0-9_-]{10,}\\.[a-z0-9_-]{10,}|"
            + "\\bBearer\\s+[a-z0-9._~+/=-]{6,}|"
            + "(?:api[_-]?key|password|secret|token|cookie|authorization|access[_-]?token|refresh[_-]?token)"
            + "\\s*[\\\"']?\\s*[:=]\\s*[\\\"']?[^,;\\s}\\\"']{4,})"
    );

    private final AgentMessageRepository agentMessageRepository;
    private final AgentContextCheckpointRepository checkpointRepository;
    private final ContextWindowResolver windowResolver;
    private final TokenEstimator tokenEstimator;
    private final AgentDraftRepository draftRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ContextBuilder(
        AgentMessageRepository agentMessageRepository,
        AgentContextCheckpointRepository checkpointRepository,
        ContextWindowResolver windowResolver,
        TokenEstimator tokenEstimator,
        AgentLlmProperties llmProperties,
        AgentDraftRepository draftRepository,
        ObjectMapper objectMapper
    ) {
        this.agentMessageRepository = agentMessageRepository;
        this.checkpointRepository = checkpointRepository;
        this.windowResolver = windowResolver;
        this.tokenEstimator = tokenEstimator;
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /** 兼容现有隔离测试和旧调用方。 */
    public ContextBuilder(
        AgentMessageRepository agentMessageRepository,
        AgentContextCheckpointRepository checkpointRepository,
        ContextWindowResolver windowResolver,
        TokenEstimator tokenEstimator,
        AgentLlmProperties llmProperties,
        AgentDraftRepository draftRepository
    ) {
        this(
            agentMessageRepository,
            checkpointRepository,
            windowResolver,
            tokenEstimator,
            llmProperties,
            draftRepository,
            new ObjectMapper()
        );
    }

    /** 兼容现有隔离测试和旧调用方。 */
    public ContextBuilder(
        AgentMessageRepository agentMessageRepository,
        AgentContextCheckpointRepository checkpointRepository,
        ContextWindowResolver windowResolver,
        TokenEstimator tokenEstimator,
        AgentLlmProperties llmProperties
    ) {
        this(agentMessageRepository, checkpointRepository, windowResolver, tokenEstimator, llmProperties, null);
    }

    /**
     * 为当前 owner + conversation 构建上下文包。
     *
     * @param ownerUserId       当前 owner
     * @param conversationId    当前会话
     * @param currentUserMessage 当前用户问题（不可被截断）
     * @param toolCatalog        工具目录文本（来自 AgentPromptCatalog）
     * @param scopeDescription   owner/store 作用域说明（来自认证上下文）
     * @return 上下文包（含历史消息、检查点、预算估算）
     */
    public ContextPackage build(
        Long ownerUserId,
        Long conversationId,
        String currentUserMessage,
        String toolCatalog,
        String scopeDescription
    ) {
        return build(ownerUserId, conversationId, currentUserMessage, toolCatalog, scopeDescription, List.of());
    }

    /**
     * 构建上下文并接收运行时未完成工具的元数据。原始工具参数不进入上下文状态。
     */
    public ContextPackage build(
        Long ownerUserId,
        Long conversationId,
        String currentUserMessage,
        String toolCatalog,
        String scopeDescription,
        List<PendingToolCall> pendingToolCalls
    ) {
        ContextWindowResolver.Resolution resolution = windowResolver.resolveForCurrentWithSource();
        int providerWindow = resolution.tokens();
        boolean degraded = windowResolver.isConservativeFallback(resolution);
        double safetyMargin = SAFETY_MARGIN_RATIO + (degraded ? DEGRADED_SAFETY_MARGIN_BOOST : 0.0);
        int usableWindow = providerWindow;
        int systemBudget = budgetPortion(usableWindow, SYSTEM_BUDGET_RATIO);
        int scopeBudget = budgetPortion(usableWindow, SCOPE_BUDGET_RATIO);
        int currentQuestionBudget = budgetPortion(usableWindow, CURRENT_QUESTION_RATIO);
        int toolResultBudget = budgetPortion(usableWindow, TOOL_RESULT_RATIO);
        int reservedOutputBudget = budgetPortion(usableWindow, RESERVED_OUTPUT_RATIO);
        int safetyBudget = budgetPortion(usableWindow, safetyMargin);
        int historyBudget = Math.max(
            0,
            usableWindow - systemBudget - scopeBudget - currentQuestionBudget
                - toolResultBudget - reservedOutputBudget - safetyBudget
        );

        Optional<AgentContextCheckpointEntity> checkpointOpt =
            checkpointRepository.findActiveByOwnerAndConversation(ownerUserId, conversationId);
        Long boundaryId = checkpointOpt.map(AgentContextCheckpointEntity::getSourceBoundaryMessageId).orElse(null);
        List<AgentMessageEntity> loadedMessages = boundaryId == null
            ? agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(
                ownerUserId, conversationId
            )
            : agentMessageRepository.findAllByOwnerUserIdAndConversationIdAndIdGreaterThanOrderByIdAsc(
                ownerUserId, conversationId, boundaryId
            );
        List<AgentMessageEntity> messagesAfterBoundary = loadedMessages == null
            ? List.of()
            : loadedMessages.stream().filter(Objects::nonNull).toList();

        String checkpointSummary = checkpointOpt
            .map(AgentContextCheckpointEntity::getSummaryBody)
            .orElse(null);
        String safeScopeDescription = sanitizeScopeDescription(scopeDescription);
        List<PendingToolCall> safePendingToolCalls = normalizePendingToolCalls(pendingToolCalls);
        if (safePendingToolCalls.isEmpty()) {
            safePendingToolCalls = derivePendingToolCalls(messagesAfterBoundary);
        }
        List<PendingDraft> pendingDrafts = loadPendingDrafts(ownerUserId, conversationId);
        int checkpointTokens = checkpointSummary == null ? 0 : tokenEstimator.estimate(checkpointSummary);
        String formattedHistory = formatHistoryForBudget(messagesAfterBoundary);
        int historyTokens = tokenEstimator.estimateHistoryText(formattedHistory);
        int toolResultTokens = estimateToolResultTokens(messagesAfterBoundary);
        int nonToolHistoryTokens = Math.max(0, historyTokens - toolResultTokens);
        int currentQuestionTokens = tokenEstimator.estimate(currentUserMessage);
        int systemTokens = tokenEstimator.estimate(
            AgentPromptCatalog.initialSystemPrompt(toolCatalog == null ? "" : toolCatalog)
        );
        int scopeTokens = tokenEstimator.estimate(safeScopeDescription);
        int protectedStateTokens = tokenEstimator.estimate(protectedStateBudgetText(
            ownerUserId, conversationId, safePendingToolCalls, pendingDrafts
        ));

        int estimatedInputTokens = systemTokens + scopeTokens + checkpointTokens + historyTokens
            + currentQuestionTokens + protectedStateTokens;
        int inputBudget = Math.max(0, usableWindow - reservedOutputBudget - safetyBudget);
        int compactionThresholdTokens = Math.min(
            inputBudget,
            budgetPortion(usableWindow, COMPACTION_THRESHOLD_RATIO)
        );
        int estimatedWindowTokens = estimatedInputTokens + reservedOutputBudget + safetyBudget;
        boolean tokenBudgetExceeded = estimatedInputTokens > compactionThresholdTokens;
        boolean windowBudgetExceeded = estimatedWindowTokens > usableWindow;
        boolean historyBudgetExceeded = checkpointTokens + nonToolHistoryTokens > historyBudget;
        boolean toolResultBudgetExceeded = toolResultTokens > toolResultBudget;
        boolean currentQuestionBudgetExceeded = currentQuestionTokens > currentQuestionBudget;
        boolean compactionNeeded = tokenBudgetExceeded || windowBudgetExceeded || historyBudgetExceeded
            || toolResultBudgetExceeded || currentQuestionBudgetExceeded;

        return new ContextPackage(
            ownerUserId,
            conversationId,
            checkpointOpt.orElse(null),
            boundaryId,
            messagesAfterBoundary,
            formattedHistory,
            checkpointSummary,
            currentUserMessage,
            safeScopeDescription,
            toolCatalog,
            new ContextBudget(
                providerWindow, usableWindow,
                systemBudget, scopeBudget, currentQuestionBudget, toolResultBudget,
                reservedOutputBudget, safetyBudget, historyBudget,
                historyTokens, checkpointTokens, currentQuestionTokens,
                estimatedInputTokens, compactionNeeded, degraded,
                inputBudget, compactionThresholdTokens, protectedStateTokens,
                estimatedWindowTokens, tokenBudgetExceeded, windowBudgetExceeded,
                toolResultTokens, nonToolHistoryTokens, currentQuestionBudgetExceeded
            ),
            safePendingToolCalls,
            pendingDrafts
        );
    }

    private static int budgetPortion(int window, double ratio) {
        return (int) Math.max(0, Math.floor(window * ratio));
    }

    /** 预算估算使用完整历史文本；消息轮数和单条字符截断都不能替代摘要。 */
    static String formatHistoryForBudget(List<AgentMessageEntity> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("历史对话：\n");
        for (AgentMessageEntity message : history) {
            if (message == null) {
                continue;
            }
            String role = StringUtils.hasText(message.getRole()) ? message.getRole() : "unknown";
            text.append(role).append("：").append(message.getContent() == null ? "" : message.getContent()).append('\n');
        }
        return text.append('\n').toString();
    }

    /**
     * 上下文包：所有注入模型请求的输入文本与预算估算结果。
     *
     * <p>调用方按 A-G 顺序拼接，{@code current user message}、{@code scope description}
     * 不能被静默截断；预算不足时调用 {@link ContextCompactionService}。
     */
    public record ContextPackage(
        Long ownerUserId,
        Long conversationId,
        AgentContextCheckpointEntity checkpoint,
        Long boundaryMessageId,
        List<AgentMessageEntity> messagesAfterBoundary,
        String formattedHistory,
        String checkpointSummary,
        String currentUserMessage,
        String scopeDescription,
        String toolCatalog,
        ContextBudget budget,
        List<PendingToolCall> pendingToolCalls,
        List<PendingDraft> pendingDrafts
    ) {
        public ContextPackage(
            Long ownerUserId,
            Long conversationId,
            AgentContextCheckpointEntity checkpoint,
            Long boundaryMessageId,
            List<AgentMessageEntity> messagesAfterBoundary,
            String formattedHistory,
            String checkpointSummary,
            String currentUserMessage,
            String scopeDescription,
            String toolCatalog,
            ContextBudget budget
        ) {
            this(
                ownerUserId, conversationId, checkpoint, boundaryMessageId, messagesAfterBoundary,
                formattedHistory, checkpointSummary, currentUserMessage, scopeDescription, toolCatalog,
                budget, List.of(), List.of()
            );
        }

        public ContextPackage {
            pendingToolCalls = pendingToolCalls == null ? List.of() : List.copyOf(pendingToolCalls);
            pendingDrafts = pendingDrafts == null ? List.of() : List.copyOf(pendingDrafts);
        }

        public boolean hasActiveCheckpoint() {
            return checkpoint != null && StringUtils.hasText(checkpointSummary);
        }
    }

    /** 未完成工具调用的非敏感上下文元数据。 */
    public record PendingToolCall(String callId, String toolName, String status) {}

    /** 待确认草稿的非敏感上下文元数据；不携带 content_json。 */
    public record PendingDraft(
        Long draftId,
        String draftType,
        String title,
        String status,
        Long updatedAt
    ) {}

    /** 预算分配结果。 */
    public record ContextBudget(
        int providerWindow,
        int usableWindow,
        int systemBudget,
        int scopeBudget,
        int currentQuestionBudget,
        int toolResultBudget,
        int reservedOutputBudget,
        int safetyBudget,
        int historyBudget,
        int historyTokens,
        int checkpointTokens,
        int currentQuestionTokens,
        int estimatedInputTokens,
        boolean compactionNeeded,
        boolean degradedEstimate,
        int inputBudget,
        int compactionThresholdTokens,
        int protectedStateTokens,
        int estimatedWindowTokens,
        boolean tokenBudgetExceeded,
        boolean windowBudgetExceeded,
        int toolResultTokens,
        int nonToolHistoryTokens,
        boolean currentQuestionBudgetExceeded
    ) {
        public ContextBudget(
            int providerWindow,
            int usableWindow,
            int systemBudget,
            int scopeBudget,
            int currentQuestionBudget,
            int toolResultBudget,
            int reservedOutputBudget,
            int safetyBudget,
            int historyBudget,
            int historyTokens,
            int checkpointTokens,
            int currentQuestionTokens,
            int estimatedInputTokens,
            boolean compactionNeeded,
            boolean degradedEstimate
        ) {
            this(
                providerWindow, usableWindow, systemBudget, scopeBudget, currentQuestionBudget,
                toolResultBudget, reservedOutputBudget, safetyBudget, historyBudget, historyTokens,
                checkpointTokens, currentQuestionTokens, estimatedInputTokens, compactionNeeded,
                degradedEstimate,
                Math.max(0, usableWindow - reservedOutputBudget - safetyBudget),
                Math.min(
                    Math.max(0, usableWindow - reservedOutputBudget - safetyBudget),
                    budgetPortion(usableWindow, COMPACTION_THRESHOLD_RATIO)
                ),
                0,
                estimatedInputTokens + reservedOutputBudget + safetyBudget,
                estimatedInputTokens > Math.min(
                    Math.max(0, usableWindow - reservedOutputBudget - safetyBudget),
                    budgetPortion(usableWindow, COMPACTION_THRESHOLD_RATIO)
                ),
                estimatedInputTokens + reservedOutputBudget + safetyBudget > usableWindow,
                0,
                historyTokens,
                currentQuestionTokens > currentQuestionBudget
            );
        }
    }

    private List<PendingDraft> loadPendingDrafts(Long ownerUserId, Long conversationId) {
        if (draftRepository == null || ownerUserId == null || conversationId == null) {
            return List.of();
        }
        List<AgentDraftEntity> drafts = draftRepository
            .findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(ownerUserId, conversationId);
        if (drafts == null) {
            return List.of();
        }
        return drafts.stream()
            .filter(Objects::nonNull)
            .filter(draft -> isPendingDraftStatus(draft.getStatus()))
            .map(draft -> new PendingDraft(
                draft.getId(),
                safeIdentifier(draft.getDraftType(), 64),
                sanitizeProtectedText(draft.getTitle(), MAX_PROTECTED_TEXT_LENGTH),
                safeIdentifier(draft.getStatus(), 32),
                draft.getUpdatedAt()
            ))
            .toList();
    }

    private List<PendingToolCall> normalizePendingToolCalls(List<PendingToolCall> pendingToolCalls) {
        if (pendingToolCalls == null) {
            return List.of();
        }
        return pendingToolCalls.stream()
            .filter(Objects::nonNull)
            .map(call -> new PendingToolCall(
                safeIdentifier(call.callId(), 96),
                safeIdentifier(call.toolName(), 96),
                safeIdentifier(call.status(), 32)
            ))
            .filter(call -> StringUtils.hasText(call.toolName()))
            .toList();
    }

    private String protectedStateBudgetText(
        Long ownerUserId,
        Long conversationId,
        List<PendingToolCall> pendingToolCalls,
        List<PendingDraft> pendingDrafts
    ) {
        return "owner_user_id=" + ownerUserId
            + " conversation_id=" + conversationId
            + " pending_tools=" + pendingToolCalls.stream()
                .map(call -> call.callId() + ":" + call.toolName() + ":" + call.status())
                .reduce("", (left, right) -> left + right)
            + " pending_drafts=" + pendingDrafts.stream()
                .map(draft -> draft.draftId() + ":" + draft.draftType() + ":" + draft.status())
                .reduce("", (left, right) -> left + right);
    }

    private String sanitizeScopeDescription(String scopeDescription) {
        if (!StringUtils.hasText(scopeDescription)) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        for (String line : scopeDescription.split("\\R")) {
            String normalized = line == null ? "" : line.trim();
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!StringUtils.hasText(normalized)
                || !(lower.contains("owner") || lower.contains("store") || lower.contains("permission")
                    || lower.contains("scope") || lower.contains("role") || normalized.contains("权限")
                    || normalized.contains("作用域") || normalized.contains("门店") || normalized.contains("角色"))) {
                continue;
            }
            String bounded = sanitizeProtectedText(normalized, MAX_SCOPE_TEXT_LENGTH);
            if (safe.length() > 0) {
                safe.append('\n');
            }
            safe.append(bounded);
            if (safe.length() >= MAX_SCOPE_TEXT_LENGTH) {
                break;
            }
        }
        return safe.length() <= MAX_SCOPE_TEXT_LENGTH
            ? safe.toString()
            : safe.substring(0, MAX_SCOPE_TEXT_LENGTH);
    }

    private String sanitizeProtectedText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text == null ? "" : text;
        }
        String redacted = SENSITIVE_CONTEXT_PATTERN.matcher(text).replaceAll("[REDACTED]");
        return redacted.length() > maxLength ? redacted.substring(0, maxLength) : redacted;
    }

    private String safeIdentifier(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("[^a-zA-Z0-9_.:-]", "_");
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private boolean isPendingDraftStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return Set.of("active", "pending", "awaiting_confirmation", "confirming")
            .contains(status.trim().toLowerCase(Locale.ROOT));
    }

    private int estimateToolResultTokens(List<AgentMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (AgentMessageEntity message : messages) {
            if (message == null || !("tool".equalsIgnoreCase(message.getRole())
                || "function".equalsIgnoreCase(message.getRole()))) {
                continue;
            }
            total += (long) tokenEstimator.estimate(message.getContent()) + TokenEstimator.PER_MESSAGE_OVERHEAD;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    /** 从已持久化的结构化工具状态补齐五参数调用方没有传入的未完成工具元数据。 */
    private List<PendingToolCall> derivePendingToolCalls(List<AgentMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Map<String, PendingToolCall> pending = new LinkedHashMap<>();
        Set<String> completed = new HashSet<>();
        for (AgentMessageEntity message : messages) {
            if (message == null || !StringUtils.hasText(message.getStructuredDataJson())) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(message.getStructuredDataJson());
                if (root == null || !root.isObject()) {
                    continue;
                }
                JsonNode calls = root.get("tool_calls");
                if (calls != null && calls.isArray()) {
                    for (JsonNode call : calls) {
                        addPendingToolCall(call, pending, completed);
                    }
                } else {
                    addMessageToolState(message, root, pending, completed);
                }
            } catch (Exception ignored) {
                // Malformed structured data is not copied into context state.
            }
        }
        return pending.values().stream()
            .filter(call -> !completed.contains(pendingKey(call.callId(), call.toolName())))
            .toList();
    }

    private void addPendingToolCall(
        JsonNode call,
        Map<String, PendingToolCall> pending,
        Set<String> completed
    ) {
        if (call == null || !call.isObject()) {
            return;
        }
        JsonNode function = call.get("function");
        JsonNode source = function != null && function.isObject() ? function : call;
        String toolName = safeIdentifier(firstText(source, "tool_name", "name"), 96);
        if (!StringUtils.hasText(toolName)) {
            return;
        }
        String callId = safeIdentifier(firstText(call, "tool_call_id", "call_id", "id"), 96);
        String status = safeIdentifier(firstText(call, "status", "state"), 32);
        String effectiveStatus = StringUtils.hasText(status) ? status : "pending";
        if (isCompletedToolStatus(effectiveStatus)) {
            completed.add(pendingKey(callId, toolName));
            pending.remove(pendingKey(callId, toolName));
        } else {
            pending.put(pendingKey(callId, toolName), new PendingToolCall(callId, toolName, effectiveStatus));
        }
    }

    private void addMessageToolState(
        AgentMessageEntity message,
        JsonNode root,
        Map<String, PendingToolCall> pending,
        Set<String> completed
    ) {
        String toolName = safeIdentifier(firstText(root, "tool_name", "name"), 96);
        if (!StringUtils.hasText(toolName)) {
            return;
        }
        String callId = safeIdentifier(firstText(root, "tool_call_id", "call_id", "id"), 96);
        String status = safeIdentifier(firstText(root, "status", "state"), 32);
        boolean resultMessage = "tool".equalsIgnoreCase(message.getRole())
            || "function".equalsIgnoreCase(message.getRole());
        String effectiveStatus = StringUtils.hasText(status)
            ? status
            : (resultMessage ? "completed" : "pending");
        String key = pendingKey(callId, toolName);
        if (isCompletedToolStatus(effectiveStatus) || resultMessage && !isPendingToolStatus(effectiveStatus)) {
            completed.add(key);
            pending.remove(key);
        } else {
            pending.put(key, new PendingToolCall(callId, toolName, effectiveStatus));
        }
    }

    private boolean isCompletedToolStatus(String status) {
        return Set.of("completed", "complete", "success", "succeeded", "failed", "cancelled", "canceled")
            .contains(status.toLowerCase(Locale.ROOT));
    }

    private boolean isPendingToolStatus(String status) {
        return Set.of("pending", "queued", "running", "in_progress", "awaiting_confirmation", "missing_output")
            .contains(status.toLowerCase(Locale.ROOT));
    }

    private String pendingKey(String callId, String toolName) {
        return (StringUtils.hasText(callId) ? callId : "name") + "|" + toolName;
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node == null ? null : node.get(name);
            if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }
}

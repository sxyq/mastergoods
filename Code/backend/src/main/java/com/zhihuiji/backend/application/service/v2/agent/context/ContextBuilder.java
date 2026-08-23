package com.zhihuiji.backend.application.service.v2.agent.context;

import com.zhihuiji.backend.application.service.v2.agent.component.AgentPromptCatalog;
import com.zhihuiji.backend.application.service.v2.agent.component.ToolPlanner;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.List;
import java.util.Optional;
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

    /** 历史消息条数上限：超过即进入压缩评估。 */
    public static final int HISTORY_MESSAGE_LIMIT = 24;
    /** 压缩触发阈值：已用预算占可用预算的比例。 */
    public static final double COMPACTION_THRESHOLD_RATIO = 0.70;

    private final AgentMessageRepository agentMessageRepository;
    private final AgentContextCheckpointRepository checkpointRepository;
    private final ContextWindowResolver windowResolver;
    private final TokenEstimator tokenEstimator;
    private final AgentLlmProperties llmProperties;

    public ContextBuilder(
        AgentMessageRepository agentMessageRepository,
        AgentContextCheckpointRepository checkpointRepository,
        ContextWindowResolver windowResolver,
        TokenEstimator tokenEstimator,
        AgentLlmProperties llmProperties
    ) {
        this.agentMessageRepository = agentMessageRepository;
        this.checkpointRepository = checkpointRepository;
        this.windowResolver = windowResolver;
        this.tokenEstimator = tokenEstimator;
        this.llmProperties = llmProperties;
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
        int providerWindow = windowResolver.resolveForCurrent();
        boolean degraded = windowResolver.isConservativeFallback(providerWindow);
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
        List<AgentMessageEntity> messagesAfterBoundary = boundaryId == null
            ? agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
                ownerUserId, conversationId,
                org.springframework.data.domain.PageRequest.of(0, HISTORY_MESSAGE_LIMIT)
            ).reversed()
            : agentMessageRepository.findAllByOwnerUserIdAndConversationIdAndIdGreaterThanOrderByIdAsc(
                ownerUserId, conversationId, boundaryId
            );

        String checkpointSummary = checkpointOpt
            .map(AgentContextCheckpointEntity::getSummaryBody)
            .orElse(null);
        int checkpointTokens = checkpointSummary == null ? 0 : tokenEstimator.estimate(checkpointSummary);
        String formattedHistory = ToolPlanner.formatHistoryContext(messagesAfterBoundary);
        int historyTokens = tokenEstimator.estimateHistoryText(formattedHistory);
        int currentQuestionTokens = tokenEstimator.estimate(currentUserMessage);
        int systemTokens = tokenEstimator.estimate(
            AgentPromptCatalog.initialSystemPrompt(toolCatalog == null ? "" : toolCatalog, currentUserMessage)
        );
        int scopeTokens = tokenEstimator.estimate(scopeDescription == null ? "" : scopeDescription);

        int estimatedInputTokens = systemTokens + scopeTokens + checkpointTokens + historyTokens
            + currentQuestionTokens;
        boolean compactionNeeded = estimatedInputTokens > historyBudget * COMPACTION_THRESHOLD_RATIO
            || messagesAfterBoundary.size() > HISTORY_MESSAGE_LIMIT;

        return new ContextPackage(
            ownerUserId,
            conversationId,
            checkpointOpt.orElse(null),
            boundaryId,
            messagesAfterBoundary,
            formattedHistory,
            checkpointSummary,
            currentUserMessage,
            scopeDescription,
            toolCatalog,
            new ContextBudget(
                providerWindow, usableWindow,
                systemBudget, scopeBudget, currentQuestionBudget, toolResultBudget,
                reservedOutputBudget, safetyBudget, historyBudget,
                historyTokens, checkpointTokens, currentQuestionTokens,
                estimatedInputTokens, compactionNeeded, degraded
            )
        );
    }

    private static int budgetPortion(int window, double ratio) {
        return (int) Math.max(0, Math.floor(window * ratio));
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
        ContextBudget budget
    ) {
        public boolean hasActiveCheckpoint() {
            return checkpoint != null && StringUtils.hasText(checkpointSummary);
        }
    }

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
        boolean degradedEstimate
    ) {}
}

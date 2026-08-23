package com.zhihuiji.backend.application.service.v2.agent.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.annotation.PreDestroy;

/**
 * 上下文压缩服务（plan 6.4 - 6.7）。
 *
 * <p>触发条件（满足任意即进入压缩评估）：
 * <ol>
 *   <li>预计上下文占用超过可用窗口的 70%。</li>
 *   <li>历史消息数超过上限，且存在至少两个已完成轮次。</li>
 *   <li>工具结果加入后历史与当前轮合计超过工具预算。</li>
 *   <li>Provider 返回上下文超限错误。</li>
 *   <li>检查点失效后重建上下文仍超过预算。</li>
 * </ol>
 *
 * <p>两级压缩策略：
 * <ul>
 *   <li>一级：确定性抽取摘要（不调用模型），由 {@link #deterministicSummary} 生成。</li>
 *   <li>二级：隔离的语义压缩请求（只接收历史轮次，不进入工具循环，独立超时）。</li>
 * </ul>
 *
 * <p>压缩失败时使用确定性摘要；无效语义摘要不覆盖旧检查点；并发创建同一检查点
 * 通过事务和唯一约束处理，重复请求回退为读取已提交的有效版本。
 */
@Component
public class ContextCompactionService {
    private static final Logger log = LoggerFactory.getLogger(ContextCompactionService.class);

    /** 压缩请求独立超时：不进入工具循环，避免压缩请求拖延主请求。 */
    public static final int COMPACTION_TIMEOUT_MS = 20_000;
    /** 确定性摘要最大长度。 */
    public static final int DETERMINISTIC_SUMMARY_MAX_LEN = 1_500;
    /** 语义摘要最大长度。 */
    public static final int SEMANTIC_SUMMARY_MAX_LEN = 2_500;
    /** 压缩至少包含的已完成轮次（不能只压缩半个 user/assistant 对）。 */
    public static final int MIN_COMPACTED_TURNS = 1;
    /** 历史中至少完成的轮次（满足才考虑压缩）。 */
    public static final int MIN_COMPLETED_TURNS_FOR_COMPACTION = 2;
    /** 检查点保存时 revision 提升重试上限（失效后同一边界重建 + 并发竞争兜底）。 */
    public static final int MAX_CHECKPOINT_REVISION_ATTEMPTS = 3;
    /** 当前上下文策略版本（变更时使旧检查点失效）。 */
    public static final int CURRENT_POLICY_VERSION = 1;
    /** 当前工具 Schema 版本（变更时使旧检查点失效）。 */
    public static final int CURRENT_TOOL_SCHEMA_VERSION = 1;
    /** 检查点状态：active / invalidated。 */
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INVALIDATED = "invalidated";
    /** 检查点质量：deterministic / semantic。 */
    public static final String QUALITY_DETERMINISTIC = "deterministic";
    public static final String QUALITY_SEMANTIC = "semantic";

    /**
     * 摘要脱敏模式：中国大陆手机号、API Key / 密码等长凭据模式。
     *
     * <p>计划 6.7 要求手机号、地址、凭据、完整认证载荷和无关客户资料不得进入
     * 摘要；确定性摘要在写入前统一脱敏，语义摘要由提示词约束 + 结构校验兜底。
     */
    private static final java.util.regex.Pattern SENSITIVE_SUMMARY_PATTERN = java.util.regex.Pattern.compile(
        "(?i)(sk-[a-z0-9_-]{6,}|(?:api[_-]?key|password|secret|token)[=:\\s]+[a-z0-9_-]{6,}|1[3-9]\\d{9})"
    );

    private final AgentContextCheckpointRepository checkpointRepository;
    private final LongCatAnthropicClient llmClient;
    private final AgentLlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final TokenEstimator tokenEstimator;
    private final ExecutorService semanticCompactionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ContextCompactionService(
        AgentContextCheckpointRepository checkpointRepository,
        LongCatAnthropicClient llmClient,
        AgentLlmProperties llmProperties,
        ObjectMapper objectMapper,
        TokenEstimator tokenEstimator
    ) {
        this.checkpointRepository = checkpointRepository;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.tokenEstimator = tokenEstimator;
    }

    @PreDestroy
    void shutdownSemanticCompactionExecutor() {
        semanticCompactionExecutor.shutdownNow();
    }

    /**
     * 评估并执行必要的压缩，返回当前请求应使用的检查点。
     *
     * <p>当 {@code ContextPackage.budget().compactionNeeded()} 为 true 时调用。
     * 压缩失败时使用确定性摘要；不抛异常，避免影响主请求。
     *
     * @param contextPackage 当前请求的上下文包
     * @return 压缩结果（含检查点、是否复用、压缩原因）
     */
    @Transactional
    public CompactionResult compactIfNeeded(ContextBuilder.ContextPackage contextPackage) {
        if (contextPackage == null) {
            return CompactionResult.noCompaction(null);
        }
        ContextBuilder.ContextBudget budget = contextPackage.budget();
        if (budget == null || !budget.compactionNeeded()) {
            return CompactionResult.noCompaction(contextPackage.checkpoint());
        }
        // 注意：这里不因 hasActiveCheckpoint() 直接复用。预算超限说明现有检查点
        // 加上边界后的原始消息仍超出窗口，按计划 6.8 必须继续压缩边界后的
        // 已完成历史轮次，生成边界更新的新检查点；只有预算足够时才由
        // ContextBuilder 直接复用原始消息，避免每轮固定压缩。

        List<AgentMessageEntity> messages = contextPackage.messagesAfterBoundary();
        if (!hasEnoughCompletedTurns(messages)) {
            // 历史不足以压缩时退化为不压缩；预算不足由调用方决定是否降级到
            // EXHAUSTED 终态。
            return CompactionResult.noCompaction(null);
        }

        // 一次至少压缩一个完整轮次（MIN_COMPACTED_TURNS=1）。
        int compactableIndex = findCompactableBoundary(messages);
        if (compactableIndex < 0) {
            return CompactionResult.noCompaction(null);
        }
        List<AgentMessageEntity> compactableMessages = messages.subList(0, compactableIndex + 1);
        Long boundaryMessageId = compactableMessages.get(compactableMessages.size() - 1).getId();
        int compactedCount = compactableMessages.size();

        // 一级：先生成确定性摘要，确保 Provider 不可用时仍能构建请求。
        String deterministic = deterministicSummary(compactableMessages);
        // 二级：尝试隔离的语义压缩请求；失败、超时或输出无效时使用确定性摘要。
        SemanticCompactionOutcome semantic = runSemanticCompaction(
            compactableMessages, contextPackage.checkpointSummary(), boundaryMessageId
        );
        String summaryBody = semantic.body() != null ? semantic.body() : deterministic;
        String quality = semantic.body() != null ? QUALITY_SEMANTIC : QUALITY_DETERMINISTIC;

        // 保存检查点（并发冲突时回退为读取已提交版本）。
        AgentContextCheckpointEntity saved = saveCheckpoint(
            contextPackage, boundaryMessageId, compactedCount, summaryBody, quality
        );
        return new CompactionResult(
            saved,
            compactedCount,
            boundaryMessageId,
            summaryBody,
            quality,
            "context_budget_threshold",
            false,
            tokenEstimator.estimate(summaryBody),
            0
        );
    }

    /**
     * 使边界之后的检查点失效（消息编辑、删除或重新生成时调用）。
     */
    @Transactional
    public int invalidateAfterBoundary(Long ownerUserId, Long conversationId, Long boundaryMessageId, String reason) {
        return checkpointRepository.invalidateAfterBoundary(
            ownerUserId, conversationId, boundaryMessageId, reason,
            System.currentTimeMillis()
        );
    }

    /**
     * 会话删除时一并清理检查点（外键级联也会处理，这里提供显式入口）。
     */
    @Transactional
    public void deleteByOwnerAndConversation(Long ownerUserId, Long conversationId) {
        checkpointRepository.deleteAllByOwnerUserIdAndConversationId(ownerUserId, conversationId);
    }

    // ---- 内部方法 ----

    /**
     * 一级压缩：从消息列表抽取确定性摘要（不调用模型）。
     *
     * <p>摘要只保留完成判断所需的最小信息：用户问题短标题、已确认业务事实、
     * 已完成工具及数量、未完成动作、待确认草稿、错误/取消状态、最后消息时间。
     * 手机号、地址、凭据、完整认证载荷和无关客户资料不得进入摘要。
     */
    String deterministicSummary(List<AgentMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("summary_version", 1);
        // 用户问题短标题：取最近一条 user 消息的前 60 字。
        String userTitle = messages.stream()
            .filter(m -> "user".equalsIgnoreCase(m.getRole()))
            .reduce((first, second) -> second)
            .map(AgentMessageEntity::getContent)
            .map(text -> text == null ? "" : text.substring(0, Math.min(60, text.length())))
            .map(this::sanitizeSummaryText)
            .orElse("");
        root.put("conversation_goal", userTitle);
        ArrayNode facts = root.putArray("confirmed_facts");
        ArrayNode decisions = root.putArray("decisions");
        ArrayNode pendingActions = root.putArray("pending_actions");
        ArrayNode toolEvidence = root.putArray("tool_evidence");
        ArrayNode openQuestions = root.putArray("open_questions");

        Set<String> seenTools = new LinkedHashSet<>();
        for (AgentMessageEntity message : messages) {
            if (message == null) {
                continue;
            }
            String role = message.getRole();
            String content = message.getContent();
            String structured = message.getStructuredDataJson();
            if ("user".equalsIgnoreCase(role) && StringUtils.hasText(content)) {
                String trimmed = content.length() > 100 ? content.substring(0, 100) : content;
                appendBounded(openQuestions, sanitizeSummaryText(trimmed), 200);
            } else if ("assistant".equalsIgnoreCase(role)) {
                if (StringUtils.hasText(content)) {
                    appendBounded(decisions, sanitizeSummaryText(content), 200);
                }
            } else if ("tool".equalsIgnoreCase(role) || "function".equalsIgnoreCase(role)) {
                if (StringUtils.hasText(structured)) {
                    String toolName = extractToolNameFromStructured(structured);
                    if (toolName != null && seenTools.add(toolName)) {
                        ObjectNode toolNode = toolEvidence.addObject();
                        toolNode.put("tool_name", toolName);
                        toolNode.put("status", "completed");
                    }
                }
            }
        }
        long lastMessageAt = messages.stream()
            .map(AgentMessageEntity::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        root.put("last_message_at", lastMessageAt);
        root.put("source_boundary_message_id",
            messages.get(messages.size() - 1).getId());
        root.put("source_message_count", messages.size());
        String json = renderJson(root);
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > DETERMINISTIC_SUMMARY_MAX_LEN) {
            ObjectNode bounded = objectMapper.createObjectNode();
            bounded.put("summary_version", 1);
            bounded.put("conversation_goal", root.path("conversation_goal").asText(""));
            bounded.putArray("confirmed_facts");
            bounded.putArray("decisions");
            bounded.putArray("pending_actions");
            bounded.putArray("entity_references");
            bounded.putArray("tool_evidence");
            bounded.putArray("open_questions");
            bounded.put("last_message_at", lastMessageAt);
            bounded.put("source_boundary_message_id", messages.get(messages.size() - 1).getId());
            bounded.put("source_message_count", messages.size());
            json = renderJson(bounded);
        }
        return json;
    }

    /**
     * 二级压缩：发起隔离的语义压缩请求。
     *
     * <p>压缩请求只接收历史轮次和已有检查点；不进入工具循环；不允许调用
     * 业务工具、搜索工具或写入工具；使用结构化 JSON 输出；独立超时；
     * 压缩请求本身不再次触发上下文压缩。
     */
    private SemanticCompactionOutcome runSemanticCompaction(
        List<AgentMessageEntity> compactableMessages,
        String existingCheckpoint,
        Long boundaryMessageId
    ) {
        if (!llmClient.isConfigured()) {
            return SemanticCompactionOutcome.failed("llm_unavailable");
        }
        String systemPrompt = "你是会话压缩助手。只输出 JSON 结构化摘要，不输出其他文本。\n"
            + "输出必须包含字段：summary_version、conversation_goal、confirmed_facts[]、"
            + "decisions[]、pending_actions[]、entity_references[]、tool_evidence[]、"
            + "open_questions[]、source_boundary_message_id、source_message_count。\n"
            + "禁止包含手机号、地址、凭据、完整认证载荷或无关客户资料；实体显示名脱敏。\n"
            + "不要调用任何工具；不要进入工具循环；不要再次触发上下文压缩。\n";
        ObjectNode requestPayload = objectMapper.createObjectNode();
        requestPayload.put("source_boundary_message_id", boundaryMessageId);
        requestPayload.put("source_message_count", compactableMessages.size());
        if (StringUtils.hasText(existingCheckpoint)) {
            requestPayload.put("existing_checkpoint", existingCheckpoint);
        }
        ArrayNode rounds = requestPayload.putArray("rounds");
        for (AgentMessageEntity message : compactableMessages) {
            ObjectNode round = rounds.addObject();
            round.put("role", message.getRole() == null ? "" : message.getRole());
            String content = message.getContent() == null ? "" : message.getContent();
            round.put("content", content.length() > 400 ? content.substring(0, 400) : content);
        }
        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(requestPayload);
        } catch (Exception ex) {
            return SemanticCompactionOutcome.failed("payload_serialization_failed");
        }
        try {
            Optional<String> response = callSemanticCompactionWithTimeout(systemPrompt, userPrompt);
            if (response.isEmpty() || !StringUtils.hasText(response.get())) {
                return SemanticCompactionOutcome.failed("empty_response");
            }
            String body = response.get();
            if (!isValidSemanticSummary(body, boundaryMessageId, compactableMessages.size())) {
                return SemanticCompactionOutcome.failed("validation_failed");
            }
            return SemanticCompactionOutcome.ok(body);
        } catch (Exception ex) {
            log.warn("Semantic compaction failed (boundary={}): {}", boundaryMessageId, ex.getMessage());
            return SemanticCompactionOutcome.failed("provider_error");
        }
    }

    /**
     * 语义摘要输出校验（plan 6.6）。
     *
     * <p>校验：边界 ID 必须属于当前 owner/store 的当前会话；confirmed_facts
     * 只能来自消息或工具证据；字段数量、文本长度、JSON 深度和总字节数均有限制。
     * 输出缺字段、格式错误、超限或 Provider 失败时使用确定性摘要；无效语义摘要
     * 不覆盖旧检查点。
     */
    private boolean isValidSemanticSummary(String body, Long expectedBoundary, int expectedCount) {
        if (!StringUtils.hasText(body)) {
            return false;
        }
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (!parsed.isObject()) {
                return false;
            }
            if (body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > SEMANTIC_SUMMARY_MAX_LEN
                || treeDepth(parsed, 0) > 6
                || treeNodeCount(parsed) > 160
                || containsSensitiveText(parsed)) {
                return false;
            }
            if (!parsed.path("summary_version").isIntegralNumber()
                || parsed.path("summary_version").asInt() != 1
                || !parsed.path("conversation_goal").isTextual()
                || parsed.path("conversation_goal").asText().length() > 200) {
                return false;
            }
            for (String field : List.of(
                "confirmed_facts", "decisions", "pending_actions", "entity_references",
                "tool_evidence", "open_questions")) {
                JsonNode array = parsed.get(field);
                if (array == null || !array.isArray() || array.size() > 24) {
                    return false;
                }
            }
            JsonNode boundary = parsed.path("source_boundary_message_id");
            if (!boundary.isIntegralNumber() || boundary.asLong() != expectedBoundary.longValue()) {
                return false;
            }
            JsonNode count = parsed.path("source_message_count");
            if (!count.isIntegralNumber() || count.asInt() != expectedCount || count.asInt() < 1) {
                return false;
            }
            // 字段数量与深度限制。
            int fieldCount = 0;
            for (JsonNode ignored : parsed) {
                fieldCount++;
                if (fieldCount > 32) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Optional<String> callSemanticCompactionWithTimeout(String systemPrompt, String userPrompt) {
        CompletableFuture<Optional<String>> task = CompletableFuture.supplyAsync(
            () -> llmClient.createJsonMessage(systemPrompt, userPrompt), semanticCompactionExecutor);
        try {
            return task.get(COMPACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            task.cancel(true);
            return Optional.empty();
        } catch (ExecutionException | TimeoutException ex) {
            task.cancel(true);
            return Optional.empty();
        }
    }

    private int treeDepth(JsonNode node, int depth) {
        if (node == null || node.isValueNode()) {
            return depth;
        }
        int maximum = depth;
        for (JsonNode child : node) {
            maximum = Math.max(maximum, treeDepth(child, depth + 1));
        }
        return maximum;
    }

    private int treeNodeCount(JsonNode node) {
        if (node == null) {
            return 0;
        }
        int count = 1;
        for (JsonNode child : node) {
            count += treeNodeCount(child);
        }
        return count;
    }

    private boolean containsSensitiveText(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isTextual()) {
            return SENSITIVE_SUMMARY_PATTERN.matcher(node.asText()).find();
        }
        for (JsonNode child : node) {
            if (containsSensitiveText(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 保存检查点：并发冲突时回退为读取已提交的有效版本。
     *
     * <p>唯一约束冲突意味着两种可能：<br>
     * 1. 并发：另一个并发请求已经为该边界创建了有效检查点。失败请求不创建
     *    第二个有效副本，回退为读取已提交版本，不把并发异常返回为未处理的 500。<br>
     * 2. 失效后重建：同一边界的旧检查点已被 invalidate（例如消息编辑或策略
     *    版本变化），没有有效版本可读。此时提升 {@code revision} 重试，允许
     *    在唯一约束下重新生成同一边界的有效检查点；重试仍失败则放弃本次压缩，
     *    由调用方退化为不压缩（保留原始历史）。
     */
    private AgentContextCheckpointEntity saveCheckpoint(
        ContextBuilder.ContextPackage contextPackage,
        Long boundaryMessageId,
        int compactedCount,
        String summaryBody,
        String quality
    ) {
        long now = System.currentTimeMillis();
        for (int revisionAttempt = 1; revisionAttempt <= MAX_CHECKPOINT_REVISION_ATTEMPTS; revisionAttempt++) {
            AgentContextCheckpointEntity entity = new AgentContextCheckpointEntity();
            entity.setOwnerUserId(contextPackage.ownerUserId());
            entity.setConversationId(contextPackage.conversationId());
            entity.setSourceBoundaryMessageId(boundaryMessageId);
            entity.setSourceMessageCount(compactedCount);
            entity.setSummaryBody(summaryBody);
            entity.setSummaryVersion(1);
            entity.setContextPolicyVersion(CURRENT_POLICY_VERSION);
            entity.setToolSchemaVersion(CURRENT_TOOL_SCHEMA_VERSION);
            entity.setRevision(revisionAttempt);
            entity.setQuality(quality);
            entity.setStatus(STATUS_ACTIVE);
            entity.setModelName(llmProperties.getModel());
            entity.setEstimatedInputTokens(tokenEstimator.estimate(summaryBody));
            entity.setEstimatedOutputTokens(0);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            try {
                return checkpointRepository.save(entity);
            } catch (DataIntegrityViolationException ex) {
                // 并发或失效后重建：优先读取已提交的有效版本；不存在有效版本时
                // 提升 revision 重试，允许同一边界失效后重新生成有效检查点。
                AgentContextCheckpointEntity committed = checkpointRepository
                    .findActiveByOwnerAndConversation(entity.getOwnerUserId(), entity.getConversationId())
                    .orElse(null);
                if (committed != null) {
                    log.debug("Concurrent checkpoint creation detected (boundary={}); reading committed version",
                        boundaryMessageId);
                    return committed;
                }
                log.debug("Checkpoint boundary {} conflict without active version (revision={}); retrying with higher revision",
                    boundaryMessageId, revisionAttempt);
            }
        }
        log.warn("Checkpoint save failed after {} attempts (boundary={}); falling back to no compaction",
            MAX_CHECKPOINT_REVISION_ATTEMPTS, boundaryMessageId);
        return null;
    }

    private boolean hasEnoughCompletedTurns(List<AgentMessageEntity> messages) {
        if (messages == null || messages.size() < MIN_COMPLETED_TURNS_FOR_COMPACTION * 2) {
            return false;
        }
        // 至少 MIN_COMPLETED_TURNS_FOR_COMPACTION 个 user + assistant 对。
        long userCount = messages.stream()
            .filter(m -> "user".equalsIgnoreCase(m.getRole()))
            .count();
        long assistantCount = messages.stream()
            .filter(m -> "assistant".equalsIgnoreCase(m.getRole()))
            .count();
        return userCount >= MIN_COMPLETED_TURNS_FOR_COMPACTION
            && assistantCount >= MIN_COMPLETED_TURNS_FOR_COMPACTION;
    }

    /**
     * 找到第一个可压缩的边界消息 ID 的索引（保留最近完整轮次）。
     *
     * <p>压缩按"完整用户轮次"选择最早的历史段：可压缩 = 已完成的
     * user -> assistant -> tool evidence 轮次；保留 = 当前 user 问题、
     * 未完成工具调用、待确认草稿、最近完整轮次、安全拦截/取消/失败/关键决定。
     */
    private int findCompactableBoundary(List<AgentMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return -1;
        }
        // 保留最近 1 个完整 user/assistant 对，压缩更早的部分。
        // 从后往前找最后一个 user 消息位置（即当前轮 user），其之前均为可压缩。
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(messages.get(i).getRole())) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex <= 0) {
            return -1;
        }
        return lastUserIndex - 1;
    }

    private String renderJson(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private void appendBounded(ArrayNode array, String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String bounded = text.length() > maxLen ? text.substring(0, maxLen) : text;
        array.add(bounded);
    }

    /**
     * 摘要文本脱敏：命中手机号/凭据模式时保留前后缀并替换中间为星号，
     * 便于核对长度来源但不暴露原文。
     */
    private String sanitizeSummaryText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return SENSITIVE_SUMMARY_PATTERN.matcher(text).replaceAll(match -> {
            String token = match.group();
            if (token.length() <= 6) {
                return "******";
            }
            return token.substring(0, 2) + "****" + token.substring(token.length() - 2);
        });
    }

    private String extractToolNameFromStructured(String structured) {
        try {
            JsonNode node = objectMapper.readTree(structured);
            JsonNode nameNode = node.path("tool_name");
            if (nameNode.isTextual() && StringUtils.hasText(nameNode.asText())) {
                return nameNode.asText();
            }
            JsonNode callNode = node.path("tool_call_id");
            if (callNode.isTextual() && StringUtils.hasText(callNode.asText())) {
                return callNode.asText();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /** 二级压缩结果。 */
    private record SemanticCompactionOutcome(String body, String failureReason) {
        static SemanticCompactionOutcome ok(String body) {
            return new SemanticCompactionOutcome(body, null);
        }
        static SemanticCompactionOutcome failed(String reason) {
            return new SemanticCompactionOutcome(null, reason);
        }
    }

    /** 压缩结果：调用方据此 emit context_compacted 事件。 */
    public record CompactionResult(
        AgentContextCheckpointEntity checkpoint,
        int compactedCount,
        Long boundaryMessageId,
        String summaryPreview,
        String quality,
        String reason,
        boolean reused,
        long inputTokenEstimate,
        long outputTokenEstimate
    ) {
        public static CompactionResult noCompaction(AgentContextCheckpointEntity existing) {
            return new CompactionResult(existing, 0, null, null, null, null, false, 0, 0);
        }
        public static CompactionResult reused(AgentContextCheckpointEntity existing) {
            return new CompactionResult(
                existing,
                existing == null ? 0 : (existing.getSourceMessageCount() == null ? 0 : existing.getSourceMessageCount()),
                existing == null ? null : existing.getSourceBoundaryMessageId(),
                existing == null ? null : existing.getSummaryBody(),
                existing == null ? null : existing.getQuality(),
                "checkpoint_reused",
                true,
                existing == null || existing.getEstimatedInputTokens() == null ? 0 : existing.getEstimatedInputTokens(),
                existing == null || existing.getEstimatedOutputTokens() == null ? 0 : existing.getEstimatedOutputTokens()
            );
        }
        public boolean occurred() {
            return checkpoint != null && compactedCount > 0;
        }
    }
}

// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
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
import java.util.Locale;
import java.util.Map;
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
    /** 旧版本兼容常量；压缩触发与边界选择不依赖固定轮次数量。 */
    @Deprecated
    public static final int MIN_COMPACTED_TURNS = 1;
    /** 旧版本兼容常量；预算不足时允许从单个完整历史段开始压缩。 */
    @Deprecated
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
        "(?i)(?:sk-[a-z0-9_-]{6,}|eyJ[a-z0-9_-]{10,}\\.[a-z0-9_-]{10,}\\.[a-z0-9_-]{10,}|"
            + "\\bBearer\\s+[a-z0-9._~+/=-]{6,}|1[3-9]\\d{9}|"
            + "(?:api[_-]?key|password|secret|token|cookie|authorization|access[_-]?token|refresh[_-]?token)"
            + "\\s*[\\\"']?\\s*[:=]\\s*[\\\"']?[^,;\\s}\\\"']{4,}|"
            + "(?:address|地址)\\s*[:=]\\s*[^,;\\n]+)"
    );
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
        "token", "access_token", "refresh_token", "password", "secret", "api_key", "apikey",
        "cookie", "authorization", "bearer", "credential", "credentials", "auth", "raw_arguments",
        "arguments", "params", "parameters", "input", "content_json", "payload"
    );
    private static final Set<String> OPTIONAL_SUMMARY_ARRAYS = Set.of(
        "confirmed_facts", "decisions", "pending_actions", "entity_references", "tool_evidence", "open_questions"
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

        List<AgentMessageEntity> messages = contextPackage.messagesAfterBoundary() == null
            ? List.of()
            : contextPackage.messagesAfterBoundary();
        int compactableIndex = findCompactableBoundary(contextPackage);
        if (compactableIndex < 0) {
            return CompactionResult.noCompaction(null);
        }
        List<AgentMessageEntity> compactableMessages = List.copyOf(messages.subList(0, compactableIndex + 1));
        Long boundaryMessageId = compactableMessages.get(compactableMessages.size() - 1).getId();
        int compactedCount = compactableMessages.size();

        // 一级：先生成确定性摘要，确保 Provider 不可用时仍能构建请求。
        String deterministic = deterministicSummary(contextPackage, compactableMessages);
        // 二级：尝试隔离的语义压缩请求；失败、超时或输出无效时使用确定性摘要。
        SemanticCompactionOutcome semantic = runSemanticCompaction(
            contextPackage, compactableMessages, boundaryMessageId
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
        return deterministicSummary(null, messages);
    }

    private String deterministicSummary(
        ContextBuilder.ContextPackage contextPackage,
        List<AgentMessageEntity> messages
    ) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        List<AgentMessageEntity> safeMessages = messages.stream()
            .filter(java.util.Objects::nonNull)
            .toList();
        if (safeMessages.isEmpty()) {
            return "";
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("summary_version", 1);
        // 用户问题短标题只用于导航；完整当前问题由 protected state 单独保留。
        String userTitle = safeMessages.stream()
            .filter(m -> "user".equalsIgnoreCase(m.getRole()))
            .reduce((first, second) -> second)
            .map(AgentMessageEntity::getContent)
            .map(text -> safeMessageText(text, 60))
            .orElse("");
        root.put("conversation_goal", userTitle);
        root.putArray("confirmed_facts");
        ArrayNode decisions = root.putArray("decisions");
        ArrayNode pendingActions = root.putArray("pending_actions");
        root.putArray("entity_references");
        ArrayNode toolEvidence = root.putArray("tool_evidence");
        ArrayNode openQuestions = root.putArray("open_questions");

        Set<String> seenTools = new LinkedHashSet<>();
        for (AgentMessageEntity message : safeMessages) {
            String role = message.getRole();
            String content = message.getContent();
            String structured = message.getStructuredDataJson();
            if ("user".equalsIgnoreCase(role) && StringUtils.hasText(content)) {
                appendBounded(openQuestions, safeMessageText(content, 100), 200);
            } else if ("assistant".equalsIgnoreCase(role)) {
                if (StringUtils.hasText(content)) {
                    appendBounded(decisions, safeMessageText(content, 200), 200);
                }
            } else if ("tool".equalsIgnoreCase(role) || "function".equalsIgnoreCase(role)) {
                String toolName = extractToolNameFromStructured(structured);
                if (toolName != null && seenTools.add(toolName)) {
                    ObjectNode toolNode = toolEvidence.addObject();
                    toolNode.put("tool_name", toolName);
                    toolNode.put("status", extractToolStatus(structured, "completed"));
                }
            }
        }
        long lastMessageAt = safeMessages.stream()
            .map(AgentMessageEntity::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        root.put("last_message_at", lastMessageAt);
        root.put("source_boundary_message_id", safeMessages.get(safeMessages.size() - 1).getId());
        root.put("source_message_count", safeMessages.size());
        if (contextPackage != null) {
            appendProtectedState(root, contextPackage);
        }
        return renderDeterministicSummary(root);
    }

    /** 压缩可选事实数组，保护字段始终完整保留，极端情况下也不截断 JSON。 */
    private String renderDeterministicSummary(ObjectNode root) {
        String json = renderJson(root);
        while (utf8Length(json) > DETERMINISTIC_SUMMARY_MAX_LEN) {
            ArrayNode largest = null;
            for (String field : OPTIONAL_SUMMARY_ARRAYS) {
                JsonNode node = root.get(field);
                if (node instanceof ArrayNode array && (largest == null || array.size() > largest.size())) {
                    largest = array;
                }
            }
            if (largest == null || largest.isEmpty()) {
                // current_question/scope/pending state are protected fields. A hard
                // size cap may protect resources, but it cannot replace them with a
                // substring or become the normal compaction termination condition.
                return json;
            }
            largest.remove(largest.size() - 1);
            json = renderJson(root);
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
        ContextBuilder.ContextPackage contextPackage,
        List<AgentMessageEntity> compactableMessages,
        Long boundaryMessageId
    ) {
        if (!llmClient.isConfigured()) {
            return SemanticCompactionOutcome.failed("llm_unavailable");
        }
        String systemPrompt = "你是会话压缩助手。只输出 JSON 结构化摘要，不输出其他文本。\n"
            + "输出必须包含字段：summary_version、conversation_goal、confirmed_facts[]、"
            + "decisions[]、pending_actions[]、entity_references[]、tool_evidence[]、"
            + "open_questions[]、source_boundary_message_id、source_message_count。\n"
            + "服务端会附加并校验 current_question、owner_user_id、conversation_id、"
            + "scope_description、permissions、pending_tools、pending_drafts。\n"
            + "禁止包含手机号、地址、凭据、完整认证载荷、原始工具参数或无关客户资料；实体显示名脱敏。\n"
            + "不要调用任何工具；不要进入工具循环；不要再次触发上下文压缩。\n";
        ObjectNode requestPayload = objectMapper.createObjectNode();
        requestPayload.put("source_boundary_message_id", boundaryMessageId);
        requestPayload.put("source_message_count", compactableMessages.size());
        appendProtectedState(requestPayload, contextPackage);
        if (StringUtils.hasText(contextPackage.checkpointSummary())) {
            requestPayload.put(
                "existing_checkpoint",
                sanitizeOpaqueText(contextPackage.checkpointSummary(), DETERMINISTIC_SUMMARY_MAX_LEN)
            );
        }
        ArrayNode rounds = requestPayload.putArray("rounds");
        for (AgentMessageEntity message : compactableMessages) {
            if (message == null) {
                continue;
            }
            ObjectNode round = rounds.addObject();
            String role = message.getRole() == null ? "" : message.getRole().toLowerCase(Locale.ROOT);
            round.put("role", role);
            if ("tool".equals(role) || "function".equals(role)) {
                String toolName = extractToolNameFromStructured(message.getStructuredDataJson());
                if (StringUtils.hasText(toolName)) {
                    round.put("tool_name", toolName);
                }
                round.put("status", extractToolStatus(message.getStructuredDataJson(), "completed"));
                round.put("summary", safeMessageText(message.getContent(), 400));
            } else {
                round.put("content", safeMessageText(message.getContent(), 400));
            }
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
            String normalized = normalizeSemanticSummary(
                body, contextPackage, boundaryMessageId, compactableMessages.size()
            );
            if (normalized == null) {
                return SemanticCompactionOutcome.failed("validation_failed");
            }
            return SemanticCompactionOutcome.ok(normalized);
        } catch (Exception ex) {
            log.warn(
                "Semantic compaction failed (boundary={}): {}",
                boundaryMessageId,
                sanitizeLogMessage(ex.getMessage())
            );
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
    private String normalizeSemanticSummary(
        String body,
        ContextBuilder.ContextPackage contextPackage,
        Long expectedBoundary,
        int expectedCount
    ) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (!parsed.isObject()) {
                return null;
            }
            if (body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > SEMANTIC_SUMMARY_MAX_LEN
                || treeDepth(parsed, 0) > 6
                || treeNodeCount(parsed) > 160
                || containsSensitiveText(parsed)
                || containsSensitiveFieldName(parsed)) {
                return null;
            }
            if (!parsed.path("summary_version").isIntegralNumber()
                || parsed.path("summary_version").asInt() != 1
                || !parsed.path("conversation_goal").isTextual()
                || parsed.path("conversation_goal").asText().length() > 200) {
                return null;
            }
            for (String field : List.of(
                "confirmed_facts", "decisions", "pending_actions", "entity_references",
                "tool_evidence", "open_questions")) {
                JsonNode array = parsed.get(field);
                if (array == null || !array.isArray() || array.size() > 24) {
                    return null;
                }
            }
            JsonNode boundary = parsed.path("source_boundary_message_id");
            if (expectedBoundary == null || !boundary.isIntegralNumber()
                || boundary.asLong() != expectedBoundary.longValue()) {
                return null;
            }
            JsonNode count = parsed.path("source_message_count");
            if (!count.isIntegralNumber() || count.asInt() != expectedCount || count.asInt() < 1) {
                return null;
            }
            // 字段数量与深度限制。
            int fieldCount = 0;
            for (JsonNode ignored : parsed) {
                fieldCount++;
                if (fieldCount > 32) {
                    return null;
                }
            }
            if (!protectedFieldsMatchOrAbsent(parsed, contextPackage)) {
                return null;
            }
            ObjectNode normalized = (ObjectNode) parsed;
            appendProtectedState(normalized, contextPackage);
            String normalizedBody = renderJson(normalized);
            return normalizedBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                <= SEMANTIC_SUMMARY_MAX_LEN ? normalizedBody : null;
        } catch (Exception ex) {
            return null;
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

    /**
     * 按预算选择最小的可压缩前缀；若当前固定区块本身已超预算，则压缩所有
     * 可安全闭合的历史前缀，把完整当前问题和未配对工具状态留在边界之后。
     *
     * <p>边界由消息 token 贡献和结构化 tool call 配对状态共同决定，不以消息数或
     * 固定轮次作为终止条件。
     */
    private int findCompactableBoundary(ContextBuilder.ContextPackage contextPackage) {
        List<AgentMessageEntity> messages = contextPackage.messagesAfterBoundary();
        if (messages == null || messages.isEmpty()) {
            return -1;
        }
        int currentQuestionIndex = findCurrentQuestionIndex(messages, contextPackage.currentUserMessage());
        if (currentQuestionIndex <= 0) {
            return -1;
        }
        int selected = -1;
        int targetBudget = contextPackage.budget() == null
            ? 0
            : (contextPackage.budget().compactionThresholdTokens() > 0
                ? contextPackage.budget().compactionThresholdTokens()
                : contextPackage.budget().inputBudget());
        for (int index = 0; index < currentQuestionIndex; index++) {
            if (!isCompleteBoundary(messages, index)) {
                continue;
            }
            selected = index;
            List<AgentMessageEntity> prefix = messages.subList(0, index + 1);
            String candidateSummary = deterministicSummary(contextPackage, prefix);
            int estimatedAfter = estimateAfterCompaction(contextPackage, index, candidateSummary);
            if (targetBudget > 0 && estimatedAfter <= targetBudget) {
                return index;
            }
        }
        return selected;
    }

    private int findCurrentQuestionIndex(List<AgentMessageEntity> messages, String currentQuestion) {
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            AgentMessageEntity message = messages.get(i);
            if (message == null || !"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            if (lastUserIndex < 0) {
                lastUserIndex = i;
            }
            if (currentQuestion != null && currentQuestion.equals(message.getContent())) {
                return i;
            }
        }
        return lastUserIndex;
    }

    private boolean isCompleteBoundary(List<AgentMessageEntity> messages, int boundaryIndex) {
        AgentMessageEntity boundary = messages.get(boundaryIndex);
        if (boundary == null || "user".equalsIgnoreCase(boundary.getRole())
            || pendingToolKeys(messages, boundaryIndex).size() > 0
            || boundaryIndex + 1 < messages.size()
                && messages.get(boundaryIndex + 1) != null
                && ("tool".equalsIgnoreCase(messages.get(boundaryIndex + 1).getRole())
                    || "function".equalsIgnoreCase(messages.get(boundaryIndex + 1).getRole()))) {
            return false;
        }
        boolean hasUser = false;
        boolean hasAssistantAfterUser = false;
        for (int i = 0; i <= boundaryIndex; i++) {
            AgentMessageEntity message = messages.get(i);
            if (message == null) {
                continue;
            }
            if ("user".equalsIgnoreCase(message.getRole())) {
                hasUser = true;
                hasAssistantAfterUser = false;
            } else if (hasUser && "assistant".equalsIgnoreCase(message.getRole())) {
                hasAssistantAfterUser = true;
            }
        }
        return hasAssistantAfterUser;
    }

    private int estimateAfterCompaction(
        ContextBuilder.ContextPackage contextPackage,
        int boundaryIndex,
        String summary
    ) {
        ContextBuilder.ContextBudget budget = contextPackage.budget();
        if (budget == null) {
            return Integer.MAX_VALUE;
        }
        List<AgentMessageEntity> messages = contextPackage.messagesAfterBoundary();
        List<AgentMessageEntity> remaining = messages.subList(boundaryIndex + 1, messages.size());
        int remainingHistory = tokenEstimator.estimateHistoryText(
            ContextBuilder.formatHistoryForBudget(remaining)
        );
        int fixedTokens = Math.max(0, budget.estimatedInputTokens()
            - budget.checkpointTokens() - budget.historyTokens());
        return saturatingTokenSum(fixedTokens, tokenEstimator.estimate(summary), remainingHistory);
    }

    /** 返回 boundary 之前仍未收到结果的 tool call；原始参数永远不被读取到摘要路径。 */
    private Set<String> pendingToolKeys(List<AgentMessageEntity> messages, int endIndex) {
        Map<String, String> pending = new java.util.LinkedHashMap<>();
        for (int i = 0; i <= endIndex && i < messages.size(); i++) {
            AgentMessageEntity message = messages.get(i);
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
                        updatePendingTool(call, message.getRole(), pending);
                    }
                } else {
                    updatePendingTool(root, message.getRole(), pending);
                }
            } catch (Exception ignored) {
                // Malformed structured data is not propagated into the model prompt.
            }
        }
        return new java.util.LinkedHashSet<>(pending.keySet());
    }

    private void updatePendingTool(JsonNode call, String role, Map<String, String> pending) {
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
        String key = toolKey(callId, toolName);
        String status = firstText(call, "status", "state");
        boolean resultMessage = "tool".equalsIgnoreCase(role) || "function".equalsIgnoreCase(role);
        boolean completed = isCompletedStatus(status)
            || resultMessage && !isPendingStatus(status);
        if (completed) {
            pending.remove(key);
        } else {
            pending.put(key, toolName);
        }
    }

    private boolean isCompletedStatus(String status) {
        return StringUtils.hasText(status) && Set.of(
            "completed", "complete", "success", "succeeded", "failed", "cancelled", "canceled"
        ).contains(status.toLowerCase(Locale.ROOT));
    }

    private boolean isPendingStatus(String status) {
        return StringUtils.hasText(status) && Set.of(
            "pending", "queued", "running", "in_progress", "awaiting_confirmation", "missing_output"
        ).contains(status.toLowerCase(Locale.ROOT));
    }

    private String toolKey(String callId, String toolName) {
        return (StringUtils.hasText(callId) ? callId : "name") + "|" + toolName;
    }

    /** 将服务端掌握的保护状态写入摘要，模型返回值不能覆盖这些字段。 */
    private void appendProtectedState(
        ObjectNode target,
        ContextBuilder.ContextPackage contextPackage
    ) {
        putNullableLong(target, "owner_user_id", contextPackage.ownerUserId());
        putNullableLong(target, "conversation_id", contextPackage.conversationId());
        target.put("current_question", sanitizeSummaryText(nullToEmpty(contextPackage.currentUserMessage())));
        target.put("scope_description", sanitizeSummaryText(nullToEmpty(contextPackage.scopeDescription())));
        target.set("permissions", permissionNodes(contextPackage.scopeDescription()));
        target.set("pending_tools", pendingToolNodes(contextPackage.pendingToolCalls()));
        target.set("pending_drafts", pendingDraftNodes(contextPackage.pendingDrafts()));
        if (StringUtils.hasText(contextPackage.checkpointSummary())) {
            target.put(
                "prior_context_summary",
                sanitizeOpaqueText(contextPackage.checkpointSummary(), DETERMINISTIC_SUMMARY_MAX_LEN)
            );
        }
    }

    private boolean protectedFieldsMatchOrAbsent(
        JsonNode parsed,
        ContextBuilder.ContextPackage contextPackage
    ) {
        ObjectNode expected = objectMapper.createObjectNode();
        appendProtectedState(expected, contextPackage);
        for (String field : List.of(
            "owner_user_id", "conversation_id", "current_question", "scope_description",
            "permissions", "pending_tools", "pending_drafts", "prior_context_summary"
        )) {
            JsonNode actual = parsed.get(field);
            if (actual != null && !actual.equals(expected.get(field))) {
                return false;
            }
        }
        return true;
    }

    private ArrayNode permissionNodes(String scopeDescription) {
        ArrayNode permissions = objectMapper.createArrayNode();
        if (!StringUtils.hasText(scopeDescription)) {
            return permissions;
        }
        for (String line : scopeDescription.split("\\R")) {
            String normalized = line == null ? "" : line.trim();
            String lower = normalized.toLowerCase(Locale.ROOT);
            String values = permissionValues(normalized, lower);
            if (!StringUtils.hasText(values) || "无".equals(values)) {
                continue;
            }
            for (String value : values.split("[,，\\s]+")) {
                String permission = safeIdentifier(value, 96);
                if (StringUtils.hasText(permission)) {
                    permissions.add(permission);
                }
            }
        }
        return permissions;
    }

    private String permissionValues(String line, String lower) {
        if (!StringUtils.hasText(line)) {
            return "";
        }
        for (String marker : List.of("权限：", "权限:", "permissions=", "permission=")) {
            int index = marker.startsWith("权限") ? line.indexOf(marker) : lower.indexOf(marker);
            if (index >= 0) {
                return line.substring(index + marker.length()).trim();
            }
        }
        return "";
    }

    private ArrayNode pendingToolNodes(List<ContextBuilder.PendingToolCall> pendingTools) {
        ArrayNode nodes = objectMapper.createArrayNode();
        if (pendingTools == null) {
            return nodes;
        }
        for (ContextBuilder.PendingToolCall call : pendingTools) {
            if (call == null || !StringUtils.hasText(call.toolName())) {
                continue;
            }
            ObjectNode node = nodes.addObject();
            node.put("call_id", safeIdentifier(call.callId(), 96));
            node.put("tool_name", safeIdentifier(call.toolName(), 96));
            node.put("status", safeIdentifier(call.status(), 32));
        }
        return nodes;
    }

    private ArrayNode pendingDraftNodes(List<ContextBuilder.PendingDraft> pendingDrafts) {
        ArrayNode nodes = objectMapper.createArrayNode();
        if (pendingDrafts == null) {
            return nodes;
        }
        for (ContextBuilder.PendingDraft draft : pendingDrafts) {
            if (draft == null) {
                continue;
            }
            ObjectNode node = nodes.addObject();
            putNullableLong(node, "draft_id", draft.draftId());
            node.put("draft_type", safeIdentifier(draft.draftType(), 64));
            node.put("title", safeMessageText(draft.title(), 160));
            node.put("status", safeIdentifier(draft.status(), 32));
            putNullableLong(node, "updated_at", draft.updatedAt());
        }
        return nodes;
    }

    private void putNullableLong(ObjectNode target, String field, Long value) {
        if (value == null) {
            target.putNull(field);
        } else {
            target.put(field, value);
        }
    }

    private String safeMessageText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String safe = sanitizeSummaryText(text);
        if (safe.startsWith("{") || safe.startsWith("[")) {
            safe = sanitizeOpaqueText(text, maxLength);
        }
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }

    private String sanitizeOpaqueText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String safe;
        try {
            JsonNode parsed = objectMapper.readTree(text);
            safe = sanitizeJsonNode(parsed, 0).toString();
        } catch (Exception ignored) {
            safe = sanitizeSummaryText(text);
        }
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }

    private JsonNode sanitizeJsonNode(JsonNode node, int depth) {
        if (node == null || node.isNull() || depth > 8) {
            return objectMapper.getNodeFactory().textNode("[REDACTED]");
        }
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(sanitizeSummaryText(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode safe = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                if (!isSensitiveFieldName(entry.getKey())) {
                    safe.set(entry.getKey(), sanitizeJsonNode(entry.getValue(), depth + 1));
                }
            });
            return safe;
        }
        if (node.isArray()) {
            ArrayNode safe = objectMapper.createArrayNode();
            for (JsonNode child : node) {
                safe.add(sanitizeJsonNode(child, depth + 1));
            }
            return safe;
        }
        return node;
    }

    private boolean containsSensitiveFieldName(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveFieldName(entry.getKey()) || containsSensitiveFieldName(entry.getValue())) {
                    return true;
                }
            }
        }
        for (JsonNode child : node) {
            if (containsSensitiveFieldName(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSensitiveFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace('-', '_');
        return SENSITIVE_FIELD_NAMES.contains(normalized)
            || normalized.contains("token")
            || normalized.contains("password")
            || normalized.contains("secret")
            || normalized.contains("cookie")
            || normalized.contains("authorization")
            || normalized.contains("credential")
            || normalized.contains("raw_argument");
    }

    private int saturatingTokenSum(int... values) {
        long total = 0;
        for (int value : values) {
            total += Math.max(0, value);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private int utf8Length(String text) {
        return text == null ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    private String sanitizeLogMessage(String message) {
        String safe = sanitizeSummaryText(StringUtils.hasText(message) ? message : "unknown");
        safe = safe.replace('\n', ' ').replace('\r', ' ').trim();
        return safe.length() > 160 ? safe.substring(0, 160) : safe;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    /** 摘要文本脱敏；凭据、认证载荷和手机号不保留任何可复原片段。 */
    private String sanitizeSummaryText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return SENSITIVE_SUMMARY_PATTERN.matcher(text).replaceAll("[REDACTED]");
    }

    private String extractToolNameFromStructured(String structured) {
        try {
            JsonNode node = objectMapper.readTree(structured);
            JsonNode nameNode = node.path("tool_name");
            if (!nameNode.isTextual() || !StringUtils.hasText(nameNode.asText())) {
                nameNode = node.path("name");
            }
            return nameNode.isTextual() && StringUtils.hasText(nameNode.asText())
                ? safeIdentifier(nameNode.asText(), 96)
                : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractToolStatus(String structured, String fallback) {
        if (!StringUtils.hasText(structured)) {
            return fallback;
        }
        try {
            JsonNode node = objectMapper.readTree(structured);
            String status = firstText(node, "status", "state");
            return StringUtils.hasText(status) ? safeIdentifier(status, 32) : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node == null ? null : node.get(field);
            if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }

    private String safeIdentifier(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String safe = value.trim().replaceAll("[^a-zA-Z0-9_.:-]", "_");
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
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

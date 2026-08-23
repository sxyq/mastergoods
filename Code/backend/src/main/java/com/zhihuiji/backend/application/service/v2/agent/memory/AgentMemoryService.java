package com.zhihuiji.backend.application.service.v2.agent.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.domain.entity.AgentMemoryEntity;
import com.zhihuiji.backend.infrastructure.config.AgentMemoryProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentMemoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Agent 长期记忆服务。
 *
 * <p>提供按 owner/store 隔离的跨会话记忆召回、异步提取与删除能力。第一版使用
 * 数据库文本检索（LIKE）；记忆提取不阻塞当前回答，召回结果标记为历史记忆，
 * 不能与当前实时业务查询混合。
 *
 * <p>禁止保存凭据、完整认证载荷、私钥、模型密钥、完整手机号、地址或身份证号。
 * 敏感模式在写入前清洗；实体展示名需要脱敏或使用最小展示字段。
 *
 * <p>第一版不引入 Milvus、独立 Python 服务或 Provider 托管记忆。
 */
@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);

    /** 记忆类型：会话事实（第一版默认类型）。 */
    public static final String MEMORY_TYPE_CONVERSATION_FACT = "conversation_fact";

    /** 默认敏感级别。 */
    public static final String SENSITIVITY_NORMAL = "normal";

    /** 第一版自动提取的记忆置信度（低，标记为待审核）。 */
    public static final double DEFAULT_EXTRACTED_CONFIDENCE = 0.3;

    /**
     * 敏感信息匹配模式：手机号、邮箱、身份证号、银行卡号、长数字串、IP。
     * 匹配到的内容替换为 {@code [REDACTED]}，避免凭据或个人隐私原文入库。
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(?:" +
            "\\b1[3-9]\\d{9}\\b" +                              // 中国手机号
            "|\\b[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+\\b" +         // 邮箱
            "|\\b\\d{17}[0-9Xx]\\b" +                           // 18 位身份证号（含末位 X）
            "|\\b\\d{15}(?:\\d{2}|\\d{3}[0-9Xx])\\b" +          // 17/18 位身份证号（旧模式兼容）
            "|\\b\\d{16,19}\\b" +                               // 银行卡号 / 长数字串
            "|\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b" +               // IPv4
            ")", Pattern.UNICODE_CHARACTER_CLASS);

    private static final String REDACTED_PLACEHOLDER = "[REDACTED]";

    private final AgentMemoryRepository memoryRepository;
    private final AgentMemoryProperties properties;
    private final ExecutorService extractionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public AgentMemoryService(AgentMemoryRepository memoryRepository, AgentMemoryProperties properties) {
        this.memoryRepository = memoryRepository;
        this.properties = properties;
    }

    /**
     * 按当前 owner/store 召回有效记忆。
     *
     * <p>第一版使用数据库 LIKE 文本检索；返回结果标记为历史记忆，不可与当前
     * 实时业务查询混合。storeId 为 null 时召回 owner 范围（不限门店）的记忆。
     * 召回后更新 last_accessed_at 用于审计与 TTL。
     *
     * @param ownerUserId 归属用户 ID（不可为空）
     * @param storeId 当前门店 ID（可为 null）
     * @param query 检索关键词（可为 null）
     * @param limit 召回条数上限（受 maxRecallLimit 约束）
     * @return 历史记忆列表（按 updated_at desc）
     */
    public List<RecalledMemory> recallMemories(Long ownerUserId, Long storeId, String query, Integer limit) {
        if (ownerUserId == null) {
            return List.of();
        }
        if (!properties.isEnabled()) {
            return List.of();
        }
        int effectiveLimit = clampLimit(limit, properties.getDefaultRecallLimit(), properties.getMaxRecallLimit());
        String safeQuery = sanitizeQuery(query);
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        List<AgentMemoryEntity> entities;
        try {
            entities = storeId != null
                ? memoryRepository.findActiveByOwnerAndStore(ownerUserId, storeId, safeQuery, pageable)
                : memoryRepository.findActiveByOwner(ownerUserId, safeQuery, pageable);
        } catch (RuntimeException ex) {
            // 召回失败不能阻塞主回答：返回空记忆列表，由调用方降级。
            log.debug("memory recall failed for owner={} store={}: {}", ownerUserId, storeId, ex.getMessage());
            return List.of();
        }

        long now = System.currentTimeMillis();
        List<RecalledMemory> memories = new ArrayList<>(entities.size());
        for (AgentMemoryEntity entity : entities) {
            try {
                memoryRepository.updateLastAccessed(entity.getId(), ownerUserId, now, now);
            } catch (RuntimeException ex) {
                log.debug("updateLastAccessed failed for memory {}: {}", entity.getId(), ex.getMessage());
            }
            memories.add(RecalledMemory.from(entity));
        }
        return memories;
    }

    /**
     * 异步提取候选记忆，不阻塞当前回答。
     *
     * <p>第一版采用确定性规则：将用户问题与回答摘要作为单条候选记忆保存。
     * 不调用 LLM；不保存凭据、手机号、邮箱、身份证号等敏感原文。已存在同
     * sourceMessageId 的记忆则更新而非重复插入。
     *
     * <p>自动学习关闭 ({@code agent.memory.enabled=false}) 时直接返回，不写入。
     *
     * @param ownerUserId 归属用户 ID
     * @param storeId 当前门店 ID（可为 null）
     * @param conversationId 来源会话 ID
     * @param messageId 来源消息 ID
     * @param userQuestion 用户问题
     * @param answer 助手正式回答
     * @param toolResults 工具结果（用于后续 LLM 提取扩展，第一版可忽略）
     */
    public void extractMemoriesAsync(
        Long ownerUserId,
        Long storeId,
        Long conversationId,
        Long messageId,
        String userQuestion,
        String answer,
        JsonNode toolResults
    ) {
        if (!properties.isEnabled() || ownerUserId == null) {
            return;
        }
        if ((answer == null || answer.isBlank()) && (userQuestion == null || userQuestion.isBlank())) {
            return;
        }
        SecurityContext capturedSecurityContext = SecurityContextHolder.createEmptyContext();
        capturedSecurityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        CompletableFuture.runAsync(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(capturedSecurityContext);
                persistCandidateMemory(ownerUserId, storeId, conversationId, messageId, userQuestion, answer);
            } catch (RuntimeException ex) {
                log.warn("async memory extraction failed for owner={} conversation={}: {}",
                    ownerUserId, conversationId, ex.getMessage());
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        }, extractionExecutor);
    }

    /**
     * 用户删除指定记忆后停止召回。
     *
     * @param ownerUserId 归属用户 ID
     * @param memoryId 记忆 ID
     * @return 删除成功返回 true
     */
    public boolean deleteMemory(Long ownerUserId, Long memoryId) {
        if (ownerUserId == null || memoryId == null) {
            return false;
        }
        long deleted = memoryRepository.deleteByIdAndOwnerUserId(memoryId, ownerUserId);
        return deleted > 0;
    }

    /**
     * 查看单条记忆详情（按 owner 隔离）。
     */
    public Optional<AgentMemoryEntity> getMemory(Long ownerUserId, Long memoryId) {
        if (ownerUserId == null || memoryId == null) {
            return Optional.empty();
        }
        return memoryRepository.findByIdAndOwnerUserId(memoryId, ownerUserId);
    }

    /**
     * 按来源会话列出记忆（用于审计、溯源与用户导出）。
     */
    public List<AgentMemoryEntity> listMemoriesByConversation(Long ownerUserId, Long conversationId) {
        if (ownerUserId == null || conversationId == null) {
            return List.of();
        }
        return memoryRepository.findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(
            ownerUserId, conversationId);
    }

    private void persistCandidateMemory(
        Long ownerUserId,
        Long storeId,
        Long conversationId,
        Long messageId,
        String userQuestion,
        String answer
    ) {
        long now = System.currentTimeMillis();
        String safeQuestion = sanitizeSensitive(userQuestion);
        String safeAnswer = sanitizeSensitive(answer);
        String summary = buildSummary(safeQuestion, safeAnswer);
        String recallText = buildRecallText(safeQuestion, safeAnswer);

        // Dedup: if a memory with the same sourceMessageId already exists, update
        // rather than insert.  This keeps the candidate set stable across replays.
        List<AgentMemoryEntity> existing = conversationId != null && messageId != null
            ? memoryRepository.findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(
                ownerUserId, conversationId)
            : List.of();
        for (AgentMemoryEntity entity : existing) {
            if (messageId != null && messageId.equals(entity.getSourceMessageId())) {
                entity.setSummary(summary);
                entity.setRecallText(recallText);
                entity.setUpdatedAt(now);
                entity.setLastAccessedAt(now);
                memoryRepository.save(entity);
                return;
            }
        }

        AgentMemoryEntity entity = new AgentMemoryEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setStoreId(storeId);
        entity.setSourceConversationId(conversationId);
        entity.setSourceMessageId(messageId);
        entity.setMemoryType(MEMORY_TYPE_CONVERSATION_FACT);
        entity.setSummary(summary);
        entity.setRecallText(recallText);
        entity.setSensitivity(SENSITIVITY_NORMAL);
        entity.setConfidence(DEFAULT_EXTRACTED_CONFIDENCE);
        entity.setStatus("active");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastAccessedAt(now);
        memoryRepository.save(entity);
    }

    private String buildSummary(String userQuestion, String answer) {
        int max = properties.getMaxSummaryLength();
        StringBuilder sb = new StringBuilder();
        if (userQuestion != null && !userQuestion.isBlank()) {
            sb.append("Q: ").append(truncate(userQuestion.trim(), max / 3)).append(" | ");
        }
        if (answer != null && !answer.isBlank()) {
            sb.append("A: ").append(truncate(answer.trim(), max - sb.length() - 4));
        }
        String result = sb.toString();
        return result.length() > max ? result.substring(0, max) : result;
    }

    private String buildRecallText(String userQuestion, String answer) {
        int max = properties.getMaxRecallTextLength();
        StringBuilder sb = new StringBuilder();
        if (userQuestion != null && !userQuestion.isBlank()) {
            sb.append(userQuestion.trim());
        }
        if (answer != null && !answer.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(answer.trim());
        }
        String result = sb.toString();
        return result.length() > max ? result.substring(0, max) : result;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    /**
     * 清洗敏感信息：将匹配到的手机号、邮箱、身份证号、银行卡号、IP 替换为
     * {@code [REDACTED]}，避免凭据或个人隐私原文入库。
     */
    static String sanitizeSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return SENSITIVE_PATTERN.matcher(text).replaceAll(REDACTED_PLACEHOLDER);
    }

    private static String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int clampLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }

    /**
     * 召回的记忆 DTO，标记为历史记忆（{@code historical=true}）。
     *
     * <p>不可与当前实时业务查询混合；上下文注入时使用明确的历史标记。
     */
    public record RecalledMemory(
        Long id,
        Long storeId,
        String memoryType,
        String summary,
        String details,
        String sensitivity,
        Double confidence,
        Long sourceConversationId,
        Long sourceMessageId,
        Long createdAt,
        Long updatedAt,
        Long lastAccessedAt,
        boolean historical
    ) {
        static RecalledMemory from(AgentMemoryEntity entity) {
            return new RecalledMemory(
                entity.getId(),
                entity.getStoreId(),
                entity.getMemoryType(),
                entity.getSummary(),
                entity.getDetails(),
                entity.getSensitivity(),
                entity.getConfidence(),
                entity.getSourceConversationId(),
                entity.getSourceMessageId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastAccessedAt(),
                true
            );
        }
    }
}

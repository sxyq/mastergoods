package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentConversationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2AgentConversationService {
    private static final Set<String> VALID_CONVERSATION_STATUSES = Set.of("active", "closed", "archived");
    private static final Set<String> TERMINAL_STATUSES = Set.of("closed", "archived");
    private static final Set<String> VALID_DRAFT_STATUSES = Set.of("active", "archived");
    private static final int DEFAULT_CONVERSATION_LIMIT = 50;
    private static final int DEFAULT_MESSAGE_LIMIT = 100;
    private static final int DEFAULT_DRAFT_LIMIT = 50;
    private static final int DEFAULT_TRACE_LIMIT = 80;
    private static final int MAX_LIST_LIMIT = 200;
    private static final Set<String> SAFE_QUERY_WINDOW_KEYS = Set.of(
        "owner_scope", "period", "date_from", "date_to", "start_date", "end_date",
        "days", "limit", "offset", "status", "order_status", "payment_status",
        "stock_status", "category", "keyword"
    );
    private static final Set<String> SAFE_EVIDENCE_KEYS = Set.of(
        "source", "scope", "returned_count", "total_count", "is_truncated", "summary"
    );

    private final AgentConversationRepository agentConversationRepository;
    private final AgentMessageRepository agentMessageRepository;
    private final AgentDraftRepository agentDraftRepository;
    private final CurrentOwnerService currentOwnerService;
    private final AgentRunAuditRepository agentRunAuditRepository;
    private final AgentRunAuditEventRepository agentRunAuditEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public V2AgentConversationService(
        AgentConversationRepository agentConversationRepository,
        AgentMessageRepository agentMessageRepository,
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService,
        AgentRunAuditRepository agentRunAuditRepository,
        AgentRunAuditEventRepository agentRunAuditEventRepository,
        ObjectMapper objectMapper
    ) {
        this.agentConversationRepository = agentConversationRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.agentDraftRepository = agentDraftRepository;
        this.currentOwnerService = currentOwnerService;
        this.agentRunAuditRepository = agentRunAuditRepository;
        this.agentRunAuditEventRepository = agentRunAuditEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Backward-compatible constructor for focused conversation-service tests. */
    public V2AgentConversationService(
        AgentConversationRepository agentConversationRepository,
        AgentMessageRepository agentMessageRepository,
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this(
            agentConversationRepository,
            agentMessageRepository,
            agentDraftRepository,
            currentOwnerService,
            null,
            null,
            new ObjectMapper()
        );
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentConversationResponse> listConversations() {
        return listConversations(null, null);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentConversationResponse> listConversations(Integer page, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentConversationEntity> rows = agentConversationRepository
            .findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(
                ownerUserId,
                PageRequest.of(safePage(page), safeLimit(limit, DEFAULT_CONVERSATION_LIMIT))
            );
        return rows.stream()
            .map(this::toConversationResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public V2AgentDtos.AgentConversationResponse getConversation(Long id) {
        return toConversationResponse(getOwnedConversation(id));
    }

    @Transactional
    public V2AgentDtos.AgentConversationResponse createConversation(V2AgentDtos.AgentConversationCreateRequest request) {
        long now = System.currentTimeMillis();
        AgentConversationEntity entity = new AgentConversationEntity();
        entity.setOwnerUserId(currentOwnerService.requireCurrentOwnerUserId());
        entity.setTitle(normalizeRequired(request.title(), "title 不能为空"));
        entity.setStatus(normalizeConversationStatus(request.status()));
        entity.setLatestSummary(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastMessageAt(null);
        return toConversationResponse(agentConversationRepository.save(entity));
    }

    @Transactional
    public V2AgentDtos.AgentConversationResponse updateConversation(Long id, V2AgentDtos.AgentConversationUpdateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentConversationEntity entity = agentConversationRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("agent conversation 不存在"));
        if (request.title() != null && !request.title().trim().isBlank()) {
            entity.setTitle(request.title().trim());
        }
        if (request.status() != null && !request.status().trim().isBlank()) {
            entity.setStatus(validateConversationStatus(request.status().trim()));
        }
        entity.setUpdatedAt(System.currentTimeMillis());
        return toConversationResponse(agentConversationRepository.save(entity));
    }

    @Transactional
    public void deleteConversation(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentConversationEntity entity = agentConversationRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("agent conversation 不存在"));
        agentDraftRepository.deleteAllByOwnerUserIdAndConversationId(ownerUserId, id);
        agentMessageRepository.deleteAllByOwnerUserIdAndConversationId(ownerUserId, id);
        agentConversationRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentMessageResponse> listMessages(Long conversationId) {
        return listMessages(conversationId, null, null);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentMessageResponse> listMessages(Long conversationId, Integer page, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ensureConversationOwned(conversationId, ownerUserId);
        List<AgentMessageEntity> recentMessages = agentMessageRepository
            .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
                ownerUserId,
                conversationId,
                PageRequest.of(safePage(page), safeLimit(limit, DEFAULT_MESSAGE_LIMIT))
            );
        return recentMessages.reversed().stream()
            .map(this::toMessageResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentRunTraceResponse> listRunTraces(Long conversationId, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        ensureConversationOwned(conversationId, ownerUserId);
        if (agentRunAuditRepository == null || agentRunAuditEventRepository == null) {
            throw new IllegalStateException("Agent run trace repository 未配置");
        }
        PageRequest pageRequest = PageRequest.of(0, safeLimit(limit, DEFAULT_TRACE_LIMIT));
        return agentRunAuditRepository
            .findAllByOwnerUserIdAndConversationIdOrderByStartedAtAscIdAsc(ownerUserId, conversationId, pageRequest)
            .stream()
            .map(audit -> toRunTraceResponse(audit, ownerUserId))
            .toList();
    }

    @Transactional
    public V2AgentDtos.AgentMessageResponse createMessage(Long conversationId, V2AgentDtos.AgentMessageCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentConversationEntity conversation = ensureConversationOwned(conversationId, ownerUserId);
        if (TERMINAL_STATUSES.contains(conversation.getStatus())) {
            throw new IllegalArgumentException("已关闭或已归档的会话不能追加消息");
        }
        long now = System.currentTimeMillis();
        AgentMessageEntity entity = new AgentMessageEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(conversationId);
        entity.setRole(normalizeRequired(request.role(), "role 不能为空"));
        entity.setMessageType(normalizeRequired(request.messageType(), "messageType 不能为空"));
        entity.setContent(normalizeRequired(request.content(), "content 不能为空"));
        entity.setStructuredDataJson(normalizeOptional(request.structuredDataJson()));
        entity.setCreatedAt(now);
        AgentMessageEntity saved = agentMessageRepository.save(entity);
        conversation.setLatestSummary(trimSummary(entity.getContent()));
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        agentConversationRepository.save(conversation);
        return toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentDraftResponse> listDrafts(Long conversationId) {
        return listDrafts(conversationId, null, null);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentDraftResponse> listDrafts(Long conversationId, Integer page, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentDraftEntity> rows;
        PageRequest pageRequest = PageRequest.of(safePage(page), safeLimit(limit, DEFAULT_DRAFT_LIMIT));
        if (conversationId == null) {
            rows = agentDraftRepository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(ownerUserId, pageRequest);
        } else {
            ensureConversationOwned(conversationId, ownerUserId);
            rows = agentDraftRepository.findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(ownerUserId, conversationId, pageRequest);
        }
        return rows.stream()
            .map(this::toDraftResponse)
            .toList();
    }

    @Transactional
    public V2AgentDtos.AgentDraftResponse createDraft(V2AgentDtos.AgentDraftCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (request.conversationId() != null) {
            ensureConversationOwned(request.conversationId(), ownerUserId);
        }
        long now = System.currentTimeMillis();
        AgentDraftEntity entity = new AgentDraftEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(request.conversationId());
        entity.setDraftType(normalizeRequired(request.draftType(), "draftType 不能为空"));
        entity.setTitle(normalizeRequired(request.title(), "title 不能为空"));
        entity.setContentJson(normalizeRequired(request.contentJson(), "contentJson 不能为空"));
        entity.setStatus(normalizeDraftStatus(request.status()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDraftResponse(agentDraftRepository.save(entity));
    }

    @Transactional
    public V2AgentDtos.AgentDraftResponse updateDraft(Long id, V2AgentDtos.AgentDraftUpdateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("agent draft 不存在"));
        if (request.conversationId() != null) {
            ensureConversationOwned(request.conversationId(), ownerUserId);
        }
        entity.setConversationId(request.conversationId());
        entity.setDraftType(normalizeRequired(request.draftType(), "draftType 不能为空"));
        entity.setTitle(normalizeRequired(request.title(), "title 不能为空"));
        entity.setContentJson(normalizeRequired(request.contentJson(), "contentJson 不能为空"));
        entity.setStatus(normalizeDraftStatus(request.status()));
        entity.setUpdatedAt(System.currentTimeMillis());
        return toDraftResponse(agentDraftRepository.save(entity));
    }

    @Transactional
    public void deleteDraft(Long id) {
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("agent draft 不存在"));
        agentDraftRepository.delete(entity);
    }

    private AgentConversationEntity getOwnedConversation(Long id) {
        return agentConversationRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("agent conversation 不存在"));
    }

    private AgentConversationEntity ensureConversationOwned(Long id, Long ownerUserId) {
        return agentConversationRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("agent conversation 不存在"));
    }

    private V2AgentDtos.AgentConversationResponse toConversationResponse(AgentConversationEntity entity) {
        return new V2AgentDtos.AgentConversationResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getStatus(),
            entity.getLatestSummary(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getLastMessageAt()
        );
    }

    private V2AgentDtos.AgentMessageResponse toMessageResponse(AgentMessageEntity entity) {
        return new V2AgentDtos.AgentMessageResponse(
            entity.getId(),
            entity.getConversationId(),
            entity.getRunId(),
            entity.getRole(),
            entity.getMessageType(),
            entity.getContent(),
            entity.getStructuredDataJson(),
            entity.getCreatedAt()
        );
    }

    private V2AgentDtos.AgentRunTraceResponse toRunTraceResponse(AgentRunAuditEntity audit, Long ownerUserId) {
        List<V2AgentDtos.AgentTraceEventResponse> events = agentRunAuditEventRepository
            .findAllByRunIdAndOwnerUserIdOrderBySeqAsc(audit.getRunId(), ownerUserId)
            .stream()
            .map(this::toTraceEventResponse)
            .toList();
        return new V2AgentDtos.AgentRunTraceResponse(
            audit.getRunId(),
            audit.getConversationId(),
            audit.getStatus(),
            audit.getMode(),
            audit.getLlmStatus(),
            audit.getPlanSource(),
            audit.getToolCount(),
            nonNegativeCount(audit.getEventCount()),
            nonNegativeCount(audit.getAuditWriteDroppedCount()),
            nonNegativeCount(audit.getAuditWriteFailedCount()),
            auditLossy(audit),
            nonNegativeCount(audit.getEmittedEventCount()),
            traceWarnings(audit),
            safeIdentifier(audit.getAuditId(), null, 128),
            safeIdentifier(audit.getTraceId(), null, 128),
            safeText(audit.getErrorCode(), null, 64),
            safeText(audit.getErrorMessage(), null, 1000),
            audit.getStartedAt(),
            audit.getCompletedAt(),
            audit.getUpdatedAt(),
            events
        );
    }

    private List<String> traceWarnings(AgentRunAuditEntity audit) {
        List<String> warnings = new ArrayList<>();
        int droppedCount = audit.getAuditWriteDroppedCount() == null ? 0 : audit.getAuditWriteDroppedCount();
        int failedCount = audit.getAuditWriteFailedCount() == null ? 0 : audit.getAuditWriteFailedCount();
        if (droppedCount > 0) {
            warnings.add("audit_events_dropped:" + droppedCount);
        }
        if (failedCount > 0) {
            warnings.add("audit_events_write_failed:" + failedCount);
        }
        return warnings;
    }

    /**
     * Convert stored SSE payloads into the small, safe trace contract. The raw
     * audit payload is deliberately never exposed to API callers.
     */
    private V2AgentDtos.AgentTraceEventResponse toTraceEventResponse(AgentRunAuditEventEntity entity) {
        JsonNode payload = parseJson(entity.getPayloadJson());
        String eventType = safeText(entity.getEventType(), "unknown", 64);
        String toolCallId = safeIdentifier(payload.path("tool_call_id").asText(null), null, 128);
        String toolName = safeText(payload.path("tool_name").asText(null), null, 128);
        return new V2AgentDtos.AgentTraceEventResponse(
            safeText(entity.getEventId(), null, 128),
            entity.getSeq(),
            integerValue(payload, "tool_sequence"),
            eventType,
            toolCallId,
            toolName,
            toolLabel(toolName),
            eventContent(eventType, payload),
            safeText(payload.path("delta_source").asText(null), null, 64),
            safeText(payload.path("input_summary").asText(null), null, 600),
            safeObject(payload.path("query_window"), SAFE_QUERY_WINDOW_KEYS),
            safeText(payload.path("result_summary").asText(null), null, 600),
            integerValue(payload, "returned_count"),
            integerValue(payload, "total_count"),
            integerValue(payload, "limit"),
            booleanValue(payload, "is_truncated"),
            safeObject(payload.path("evidence"), SAFE_EVIDENCE_KEYS),
            safeMessage(eventType, payload),
            longValue(payload, "draft_id"),
            safeText(payload.path("draft_type").asText(null), null, 64),
            safeText(payload.path("title").asText(null), null, 255),
            entity.getCreatedAt()
        );
    }

    private JsonNode parseJson(String json) {
        try {
            JsonNode parsed = objectMapper.readTree(json == null ? "{}" : json);
            return parsed == null || !parsed.isObject() ? objectMapper.createObjectNode() : parsed;
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode safeObject(JsonNode source, Set<String> allowedKeys) {
        if (source == null || !source.isObject()) {
            return null;
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        allowedKeys.stream()
            .sorted()
            .forEach(key -> {
                JsonNode value = source.get(key);
                if (value == null || value.isNull()) {
                    return;
                }
                if (value.isBoolean()) {
                    safe.put(key, value.booleanValue());
                } else if (value.isIntegralNumber()) {
                    safe.put(key, value.longValue());
                } else if (value.isFloatingPointNumber()) {
                    safe.put(key, value.doubleValue());
                } else if (value.isTextual()) {
                    String text = safeText(value.asText(), null, 160);
                    if (text != null) {
                        safe.put(key, text);
                    }
                }
            });
        return safe.isEmpty() ? null : objectMapper.valueToTree(safe);
    }

    private String eventContent(String eventType, JsonNode payload) {
        String value = switch (eventType) {
            case "run_started" -> payload.path("prompt").asText(null);
            case "plan_delta" -> payload.path("content").asText(null);
            case "answer_delta" -> payload.path("delta").asText(null);
            case "answer_completed" -> payload.path("answer").asText(null);
            case "run_completed" -> payload.path("final_answer").asText(null);
            case "draft_created" -> payload.path("summary").asText(null);
            case "error" -> payload.path("message").asText(null);
            default -> null;
        };
        return safeText(value, null, 4000);
    }

    private String safeMessage(String eventType, JsonNode payload) {
        String value = payload.path("safe_message").asText(null);
        if (value == null) {
            value = payload.path("error_summary").asText(null);
        }
        if (value == null && "error".equals(eventType)) {
            value = payload.path("message").asText(null);
        }
        return safeText(value, null, 1000);
    }

    private Boolean auditLossy(AgentRunAuditEntity audit) {
        return Boolean.TRUE.equals(audit.getAuditLossy())
            || nonNegativeCount(audit.getAuditWriteDroppedCount()) > 0
            || nonNegativeCount(audit.getAuditWriteFailedCount()) > 0;
    }

    private String toolLabel(String toolName) {
        if (toolName == null) {
            return null;
        }
        return switch (toolName) {
            case "customer_receivable_lookup" -> "客户应收";
            case "supplier_payable_lookup" -> "供应商应付";
            case "sales_overview_lookup" -> "销售概览";
            case "sale_order_lookup" -> "销售订单";
            case "purchase_order_lookup" -> "采购订单";
            case "pay_order_lookup" -> "付款记录";
            case "finance_record_lookup" -> "资金流水";
            case "product_catalog_lookup" -> "商品目录";
            case "inventory_low_stock_lookup" -> "低库存商品";
            default -> toolName.replace('_', ' ');
        };
    }

    private Integer integerValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isIntegralNumber() ? value.intValue() : null;
    }

    private Long longValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isIntegralNumber() ? value.longValue() : null;
    }

    private Boolean booleanValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isBoolean() ? value.booleanValue() : null;
    }

    private Integer nonNegativeCount(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private String safeIdentifier(String value, String fallback, int maxLength) {
        String normalized = safeText(value, fallback, maxLength);
        if (normalized == null || normalized.length() > maxLength
            || !normalized.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) {
            return fallback;
        }
        return normalized;
    }

    private String safeText(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("password") || lower.contains("authorization") || lower.contains("bearer ")
            || lower.contains("select ") || lower.contains(" from ") || lower.contains("drop table")
            || lower.contains("delete from") || lower.contains("truncate table") || lower.contains("api_key")
            || lower.contains("api-key") || lower.contains("api key") || lower.contains("access_token")
            || lower.contains("refresh_token") || lower.contains("secret") || lower.matches(".*\\bsk-[a-z0-9_-]{16,}\\b.*")) {
            return "已隐藏敏感内部内容";
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private V2AgentDtos.AgentDraftResponse toDraftResponse(AgentDraftEntity entity) {
        return new V2AgentDtos.AgentDraftResponse(
            entity.getId(),
            entity.getConversationId(),
            entity.getDraftType(),
            entity.getTitle(),
            entity.getContentJson(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeConversationStatus(String status) {
        if (status == null || status.trim().isBlank()) {
            return "active";
        }
        return validateConversationStatus(status.trim());
    }

    private String validateConversationStatus(String status) {
        if (!VALID_CONVERSATION_STATUSES.contains(status)) {
            throw new IllegalArgumentException("无效的会话状态: " + status + "，允许值: " + VALID_CONVERSATION_STATUSES);
        }
        return status;
    }

    private String normalizeDraftStatus(String status) {
        if (status == null || status.trim().isBlank()) {
            return "active";
        }
        String trimmed = status.trim();
        if (!VALID_DRAFT_STATUSES.contains(trimmed)) {
            throw new IllegalArgumentException("无效的草稿状态: " + trimmed + "，允许值: " + VALID_DRAFT_STATUSES);
        }
        return trimmed;
    }

    private String trimSummary(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120);
    }

    private int safePage(Integer page) {
        return Math.max(0, page == null ? 0 : page);
    }

    private int safeLimit(Integer limit, int defaultLimit) {
        int value = limit == null ? defaultLimit : limit;
        if (value <= 0) {
            value = defaultLimit;
        }
        return Math.min(MAX_LIST_LIMIT, value);
    }
}

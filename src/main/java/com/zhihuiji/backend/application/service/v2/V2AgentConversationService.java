package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.AgentConversationEntity;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentConversationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private static final int MAX_LIST_LIMIT = 200;

    private final AgentConversationRepository agentConversationRepository;
    private final AgentMessageRepository agentMessageRepository;
    private final AgentDraftRepository agentDraftRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2AgentConversationService(
        AgentConversationRepository agentConversationRepository,
        AgentMessageRepository agentMessageRepository,
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.agentConversationRepository = agentConversationRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.agentDraftRepository = agentDraftRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentConversationResponse> listConversations() {
        return listConversations(null, null);
    }

    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentConversationResponse> listConversations(Integer page, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentConversationEntity> rows = agentConversationRepository
            .findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(
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
        List<V2AgentDtos.AgentMessageResponse> responses = new ArrayList<>(recentMessages.size());
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            responses.add(toMessageResponse(recentMessages.get(i)));
        }
        return responses;
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
            entity.getRole(),
            entity.getMessageType(),
            entity.getContent(),
            entity.getStructuredDataJson(),
            entity.getCreatedAt()
        );
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

package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

class V2AgentConversationServiceTest {
    @Mock
    private AgentConversationRepository agentConversationRepository;
    @Mock
    private AgentMessageRepository agentMessageRepository;
    @Mock
    private AgentDraftRepository agentDraftRepository;
    @Mock
    private AgentRunAuditRepository agentRunAuditRepository;
    @Mock
    private AgentRunAuditEventRepository agentRunAuditEventRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2AgentConversationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2AgentConversationService(
            agentConversationRepository,
            agentMessageRepository,
            agentDraftRepository,
            currentOwnerService,
            agentRunAuditRepository,
            agentRunAuditEventRepository,
            new ObjectMapper()
        );
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(agentMessageRepository.save(any(AgentMessageEntity.class))).thenAnswer(invocation -> {
            AgentMessageEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 301L);
            }
            return entity;
        });
        when(agentDraftRepository.save(any(AgentDraftEntity.class))).thenAnswer(invocation -> {
            AgentDraftEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 401L);
            }
            return entity;
        });
        when(agentConversationRepository.save(any(AgentConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createConversationDefaultsStatusToActive() {
        V2AgentDtos.AgentConversationResponse response = service.createConversation(
            new V2AgentDtos.AgentConversationCreateRequest("测试会话", null)
        );
        assertEquals("active", response.status());

        ArgumentCaptor<AgentConversationEntity> captor = ArgumentCaptor.forClass(AgentConversationEntity.class);
        verify(agentConversationRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals("测试会话", captor.getValue().getTitle());
    }

    @Test
    void createConversationRejectsInvalidStatus() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createConversation(new V2AgentDtos.AgentConversationCreateRequest("测试", "invalid_status"))
        );
        assertEquals(true, error.getMessage().contains("无效的会话状态: invalid_status"));
    }

    @Test
    void listConversationsReturnsOwnerScopedRowsInRepositoryOrder() {
        AgentConversationEntity first = conversation(7L);
        first.setUpdatedAt(30L);
        AgentConversationEntity second = conversation(8L);
        second.setTitle("导入追踪");
        second.setUpdatedAt(20L);
        when(agentConversationRepository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(eq(1L), any(Pageable.class)))
            .thenReturn(java.util.List.of(first, second));

        java.util.List<V2AgentDtos.AgentConversationResponse> result = service.listConversations();

        assertEquals(2, result.size());
        assertEquals(7L, result.get(0).id());
        assertEquals("导入追踪", result.get(1).title());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentConversationRepository).findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(eq(1L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listConversationsClampsInvalidPageAndOversizedLimit() {
        when(agentConversationRepository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(eq(1L), any(Pageable.class)))
            .thenReturn(java.util.List.of());

        service.listConversations(-2, 999);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentConversationRepository).findAllByOwnerUserIdOrderByUpdatedAtDescIdDescForHistory(eq(1L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(200, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getConversationReturnsOwnedConversation() {
        AgentConversationEntity entity = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(entity));

        V2AgentDtos.AgentConversationResponse result = service.getConversation(7L);

        assertEquals(7L, result.id());
        assertEquals("采购讨论", result.title());
    }

    @Test
    void updateConversationUpdatesTitleAndStatus() {
        AgentConversationEntity entity = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(entity));

        V2AgentDtos.AgentConversationResponse response = service.updateConversation(
            7L,
            new V2AgentDtos.AgentConversationUpdateRequest("新标题", "closed")
        );

        assertEquals("新标题", response.title());
        assertEquals("closed", response.status());

        ArgumentCaptor<AgentConversationEntity> captor = ArgumentCaptor.forClass(AgentConversationEntity.class);
        verify(agentConversationRepository).save(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
        assertEquals("closed", captor.getValue().getStatus());
    }

    @Test
    void updateConversationRejectsInvalidStatus() {
        AgentConversationEntity entity = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(entity));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateConversation(7L, new V2AgentDtos.AgentConversationUpdateRequest(null, "bad_status"))
        );
        assertEquals(true, error.getMessage().contains("无效的会话状态: bad_status"));
    }

    @Test
    void deleteConversationCascadeDeletesMessagesAndDrafts() {
        AgentConversationEntity entity = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(entity));

        service.deleteConversation(7L);

        verify(agentDraftRepository).deleteAllByOwnerUserIdAndConversationId(1L, 7L);
        verify(agentMessageRepository).deleteAllByOwnerUserIdAndConversationId(1L, 7L);
        verify(agentConversationRepository).delete(entity);
    }

    @Test
    void deleteConversationRejectsForeignOwner() {
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.deleteConversation(7L)
        );
        assertEquals("agent conversation 不存在", error.getMessage());
    }

    @Test
    void createMessageRefreshesConversationSummaryAndLastMessageAt() {
        AgentConversationEntity conversation = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));

        String longContent = (
            "这是一个很长的会话内容，用来验证 agent conversation 在追加 message 之后会自动刷新 latest summary，" +
            "并且长度会被安全裁剪到一百二十个字符以内，以便工作台和列表页后续直接复用。" +
            "这里继续补充更多描述，包括导入任务、库存波动、补货建议、资金安排和提醒事项，确保摘要逻辑真正覆盖超长消息场景。"
        );
        V2AgentDtos.AgentMessageResponse response = service.createMessage(
            7L,
            new V2AgentDtos.AgentMessageCreateRequest("assistant", "text", longContent, "{\"kind\":\"summary\"}")
        );

        assertEquals(301L, response.id());
        assertEquals(7L, response.conversationId());

        ArgumentCaptor<AgentConversationEntity> captor = ArgumentCaptor.forClass(AgentConversationEntity.class);
        verify(agentConversationRepository).save(captor.capture());
        assertEquals(longContent.substring(0, 120), captor.getValue().getLatestSummary());
        assertEquals(captor.getValue().getUpdatedAt(), captor.getValue().getLastMessageAt());
    }

    @Test
    void createMessageRejectsClosedConversation() {
        AgentConversationEntity conversation = conversation(7L);
        conversation.setStatus("closed");
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createMessage(7L, new V2AgentDtos.AgentMessageCreateRequest("user", "text", "内容", null))
        );
        assertEquals("已关闭或已归档的会话不能追加消息", error.getMessage());
    }

    @Test
    void createMessageRejectsArchivedConversation() {
        AgentConversationEntity conversation = conversation(7L);
        conversation.setStatus("archived");
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createMessage(7L, new V2AgentDtos.AgentMessageCreateRequest("user", "text", "内容", null))
        );
        assertEquals("已关闭或已归档的会话不能追加消息", error.getMessage());
    }

    @Test
    void listMessagesReturnsRecentWindowInTimelineOrder() {
        AgentConversationEntity conversation = conversation(7L);
        AgentMessageEntity newest = message(303L, 7L, "assistant", "summary", "最新摘要");
        AgentMessageEntity recent = message(302L, 7L, "user", "text", "最近正文");
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(eq(1L), eq(7L), any(Pageable.class)))
            .thenReturn(java.util.List.of(newest, recent));

        java.util.List<V2AgentDtos.AgentMessageResponse> result = service.listMessages(7L, 0, 80);

        assertEquals(2, result.size());
        assertEquals("最近正文", result.get(0).content());
        assertEquals("最新摘要", result.get(1).content());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentMessageRepository).findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(eq(1L), eq(7L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(80, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listRunTracesReturnsStableToolCallIdAndSafeAuditMetadata() throws Exception {
        AgentConversationEntity conversation = conversation(7L);
        AgentRunAuditEntity audit = audit("run-trace-1");
        audit.setEventCount(3);
        audit.setAuditWriteDroppedCount(2);
        audit.setAuditWriteFailedCount(1);
        audit.setAuditLossy(false);
        audit.setEmittedEventCount(5);
        audit.setAuditId("run-trace-1:audit");
        audit.setTraceId("run-trace-1:trace");
        audit.setErrorCode("TOOL_QUERY_FAILED");
        audit.setErrorMessage("授权失败，api_key=sk-live-1234567890123456");
        audit.setUpdatedAt(60L);
        String toolCallId = "run-trace-1:customer_receivable_lookup";
        String sensitivePayload = "select * from customers WHERE authorization='Bearer secret' api_key='sk-live-1234567890123456'";
        List<AgentRunAuditEventEntity> events = List.of(
            auditEvent("run-trace-1:1", 1, "tool_started", "{\"tool_call_id\":\"" + toolCallId + "\",\"tool_name\":\"customer_receivable_lookup\",\"tool_input\":{\"sql\":\"" + sensitivePayload + "\"},\"input_summary\":\"查询客户应收\"}"),
            auditEvent("run-trace-1:2", 2, "tool_completed", "{\"tool_call_id\":\"" + toolCallId + "\",\"tool_name\":\"customer_receivable_lookup\",\"result_summary\":\"已返回 1 条\",\"authorization\":\"Bearer secret\"}"),
            auditEvent("run-trace-1:3", 3, "tool_failed", "{\"tool_call_id\":\"" + toolCallId + "\",\"tool_name\":\"customer_receivable_lookup\",\"safe_message\":\"查询失败\",\"api_key\":\"sk-live-1234567890123456\"}")
        );
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));
        when(agentRunAuditRepository.findAllByOwnerUserIdAndConversationIdOrderByStartedAtAscIdAsc(eq(1L), eq(7L), any(Pageable.class)))
            .thenReturn(List.of(audit));
        when(agentRunAuditEventRepository.findAllByRunIdAndOwnerUserIdOrderBySeqAsc("run-trace-1", 1L))
            .thenReturn(events);

        V2AgentDtos.AgentRunTraceResponse response = service.listRunTraces(7L, 999).get(0);

        assertEquals(3, response.eventCount());
        assertEquals(2, response.auditWriteDroppedCount());
        assertEquals(1, response.auditWriteFailedCount());
        assertTrue(response.auditLossy());
        assertEquals(5, response.emittedEventCount());
        assertEquals("run-trace-1:audit", response.auditId());
        assertEquals("run-trace-1:trace", response.traceId());
        assertEquals("TOOL_QUERY_FAILED", response.errorCode());
        assertEquals("已隐藏敏感内部内容", response.errorMessage());
        assertEquals(60L, response.updatedAt());
        assertEquals(3, response.events().size());
        assertTrue(response.events().stream().allMatch(event -> toolCallId.equals(event.toolCallId())));

        String serialized = new ObjectMapper().writeValueAsString(response);
        assertFalse(serialized.contains(sensitivePayload));
        assertFalse(serialized.contains("authorization"));
        assertFalse(serialized.contains("api_key"));
        assertFalse(serialized.contains("payload"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentRunAuditRepository).findAllByOwnerUserIdAndConversationIdOrderByStartedAtAscIdAsc(eq(1L), eq(7L), pageableCaptor.capture());
        assertEquals(200, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listRunTracesDoesNotFabricateToolCallIdWhenPayloadOmitsIt() {
        AgentRunAuditEntity audit = audit("run-trace-no-id");
        AgentRunAuditEventEntity event = auditEvent(
            "run-trace-no-id:1",
            1,
            "tool_started",
            "{\"tool_name\":\"customer_receivable_lookup\",\"event_id\":\"different-event-id\"}"
        );
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation(7L)));
        when(agentRunAuditRepository.findAllByOwnerUserIdAndConversationIdOrderByStartedAtAscIdAsc(eq(1L), eq(7L), any(Pageable.class)))
            .thenReturn(List.of(audit));
        when(agentRunAuditEventRepository.findAllByRunIdAndOwnerUserIdOrderBySeqAsc("run-trace-no-id", 1L))
            .thenReturn(List.of(event));

        V2AgentDtos.AgentTraceEventResponse response = service.listRunTraces(7L, 1).get(0).events().get(0);

        assertNull(response.toolCallId());
        assertEquals("customer_receivable_lookup", response.toolName());
    }

    @Test
    void createDraftRejectsForeignConversation() {
        when(agentConversationRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createDraft(new V2AgentDtos.AgentDraftCreateRequest(
                9L,
                "operation",
                "补货建议",
                "{\"action\":\"purchase\"}",
                "active"
            ))
        );

        assertEquals("agent conversation 不存在", error.getMessage());
    }

    @Test
    void createDraftRejectsInvalidStatus() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createDraft(new V2AgentDtos.AgentDraftCreateRequest(
                null,
                "operation",
                "补货建议",
                "{\"action\":\"purchase\"}",
                "bad_status"
            ))
        );
        assertEquals(true, error.getMessage().contains("无效的草稿状态: bad_status"));
    }

    @Test
    void createDraftPersistsOwnerScopedDraft() {
        V2AgentDtos.AgentDraftResponse response = service.createDraft(
            new V2AgentDtos.AgentDraftCreateRequest(
                null,
                "operation",
                "补货建议",
                "{\"action\":\"purchase\"}",
                null
            )
        );

        assertEquals(401L, response.id());
        assertEquals("active", response.status());

        ArgumentCaptor<AgentDraftEntity> captor = ArgumentCaptor.forClass(AgentDraftEntity.class);
        verify(agentDraftRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals("operation", captor.getValue().getDraftType());
    }

    @Test
    void listDraftsReturnsOwnerScopedRows() {
        AgentDraftEntity first = draft(12L, 7L);
        AgentDraftEntity second = draft(13L, 7L);
        second.setTitle("新草稿");
        AgentConversationEntity conversation = conversation(7L);
        when(agentConversationRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(conversation));
        when(agentDraftRepository.findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(eq(1L), eq(7L), any(Pageable.class)))
            .thenReturn(java.util.List.of(first, second));

        java.util.List<V2AgentDtos.AgentDraftResponse> result = service.listDrafts(7L);

        assertEquals(2, result.size());
        assertEquals(12L, result.get(0).id());
        assertEquals("新草稿", result.get(1).title());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentDraftRepository).findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(eq(1L), eq(7L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listDraftsWithoutConversationUsesOwnerScopedDefaultPage() {
        AgentDraftEntity draft = draft(12L, null);
        when(agentDraftRepository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(eq(1L), any(Pageable.class)))
            .thenReturn(java.util.List.of(draft));

        java.util.List<V2AgentDtos.AgentDraftResponse> result = service.listDrafts(null);

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).id());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(agentDraftRepository).findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(eq(1L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void updateDraftPersistsOwnerScopedConversationReference() {
        AgentDraftEntity draft = draft(12L, 7L);
        AgentConversationEntity conversation = conversation(8L);
        when(agentDraftRepository.findByIdAndOwnerUserId(12L, 1L)).thenReturn(Optional.of(draft));
        when(agentConversationRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(conversation));

        V2AgentDtos.AgentDraftResponse response = service.updateDraft(
            12L,
            new V2AgentDtos.AgentDraftUpdateRequest(8L, "operation", "新的补货计划", "{\"sku\":7}", "archived")
        );

        assertEquals(12L, response.id());
        assertEquals(8L, response.conversationId());
        assertEquals("archived", response.status());
    }

    @Test
    void deleteDraftRemovesOwnedDraft() {
        AgentDraftEntity draft = draft(12L, 7L);
        when(agentDraftRepository.findByIdAndOwnerUserId(12L, 1L)).thenReturn(Optional.of(draft));

        service.deleteDraft(12L);

        verify(agentDraftRepository).delete(draft);
    }

    private AgentConversationEntity conversation(Long id) {
        AgentConversationEntity entity = new AgentConversationEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setTitle("采购讨论");
        entity.setStatus("active");
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(10L);
        return entity;
    }

    private AgentDraftEntity draft(Long id, Long conversationId) {
        AgentDraftEntity entity = new AgentDraftEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setConversationId(conversationId);
        entity.setDraftType("operation");
        entity.setTitle("旧草稿");
        entity.setContentJson("{\"old\":true}");
        entity.setStatus("active");
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(10L);
        return entity;
    }

    private AgentMessageEntity message(Long id, Long conversationId, String role, String messageType, String content) {
        AgentMessageEntity entity = new AgentMessageEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setMessageType(messageType);
        entity.setContent(content);
        entity.setCreatedAt(10L + id);
        return entity;
    }

    private AgentRunAuditEntity audit(String runId) {
        AgentRunAuditEntity entity = new AgentRunAuditEntity();
        entity.setOwnerUserId(1L);
        entity.setConversationId(7L);
        entity.setRunId(runId);
        entity.setStatus("completed");
        entity.setMode("tool_query_llm_streamed");
        entity.setStartedAt(10L);
        entity.setCompletedAt(50L);
        entity.setUpdatedAt(50L);
        entity.setEventCount(1);
        entity.setAuditWriteDroppedCount(0);
        entity.setAuditWriteFailedCount(0);
        entity.setAuditLossy(false);
        entity.setEmittedEventCount(1);
        return entity;
    }

    private AgentRunAuditEventEntity auditEvent(String eventId, int seq, String eventType, String payloadJson) {
        AgentRunAuditEventEntity entity = new AgentRunAuditEventEntity();
        entity.setRunId(eventId.substring(0, eventId.lastIndexOf(':')));
        entity.setEventId(eventId);
        entity.setSeq(seq);
        entity.setEventType(eventType);
        entity.setPayloadJson(payloadJson);
        entity.setCreatedAt(20L + seq);
        return entity;
    }

    private void setId(AgentConversationEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = AgentConversationEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setId(AgentDraftEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = AgentDraftEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setId(AgentMessageEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = AgentMessageEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

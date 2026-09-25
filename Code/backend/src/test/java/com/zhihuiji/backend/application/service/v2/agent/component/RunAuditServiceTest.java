// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RunAuditServiceTest {
    @Mock private AgentRunAuditRepository auditRepository;
    @Mock private AgentRunAuditEventRepository eventRepository;

    private RunAuditService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RunAuditService(auditRepository, eventRepository, new ObjectMapper());
    }

    @Test
    void recordsLinkedDraftActionAndKeepsOriginalTerminalStatus() throws Exception {
        AgentRunAuditEntity audit = audit("run-1", 8, "confirmation_pending");
        AgentRunAuditEventEntity previous = event("run-1", 8);
        when(auditRepository.findByRunIdAndOwnerUserId("run-1", 41L)).thenReturn(Optional.of(audit));
        when(eventRepository.findByEventId("run-1:draft-action:7:confirmed"))
            .thenReturn(Optional.empty());
        when(eventRepository.findTopByRunIdOrderBySeqDescIdDesc("run-1"))
            .thenReturn(Optional.of(previous));
        when(eventRepository.countByRunIdAndOwnerUserId("run-1", 41L)).thenReturn(9L);

        service.recordDraftAction(
            41L, "run-1", 7L, "create_customer", "confirmed", 99L,
            "create_customer:88", null, 200L
        );

        ArgumentCaptor<AgentRunAuditEventEntity> eventCaptor =
            ArgumentCaptor.forClass(AgentRunAuditEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        AgentRunAuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("run-1", savedEvent.getRunId());
        assertEquals("run-1:draft-action:7:confirmed", savedEvent.getEventId());
        assertEquals(9, savedEvent.getSeq());
        assertEquals("draft_confirmed", savedEvent.getEventType());
        JsonNode payload = new ObjectMapper().readTree(savedEvent.getPayloadJson());
        assertEquals(7L, payload.path("draft_id").asLong());
        assertEquals("create_customer:88", payload.path("business_reference").asText());
        assertEquals(99L, payload.path("actor_user_id").asLong());
        assertEquals("confirmation_pending", audit.getStatus());
        assertEquals(9, audit.getEventCount());
        assertEquals(200L, audit.getUpdatedAt());
        verify(auditRepository).save(audit);
    }

    @Test
    void doesNotDuplicateExistingDraftAction() {
        AgentRunAuditEntity audit = audit("run-2", 4, "confirmation_pending");
        AgentRunAuditEventEntity existing = event("run-2", 5);
        when(auditRepository.findByRunIdAndOwnerUserId("run-2", 1L)).thenReturn(Optional.of(audit));
        when(eventRepository.findByEventId("run-2:draft-action:9:cancelled"))
            .thenReturn(Optional.of(existing));

        service.recordDraftAction(
            1L, "run-2", 9L, "create_product", "cancelled", 1L,
            null, null, 300L
        );

        verify(eventRepository, never()).findTopByRunIdOrderBySeqDescIdDesc(any());
        verify(eventRepository, never()).save(any(AgentRunAuditEventEntity.class));
        assertEquals(4, audit.getEventCount());
        assertEquals(300L, audit.getUpdatedAt());
        verify(auditRepository).save(audit);
    }

    private AgentRunAuditEntity audit(String runId, int eventCount, String status) {
        AgentRunAuditEntity entity = new AgentRunAuditEntity();
        entity.setOwnerUserId(1L);
        entity.setConversationId(101L);
        entity.setRunId(runId);
        entity.setStatus(status);
        entity.setEventCount(eventCount);
        entity.setUpdatedAt(100L);
        return entity;
    }

    private AgentRunAuditEventEntity event(String runId, int seq) {
        AgentRunAuditEventEntity event = new AgentRunAuditEventEntity();
        event.setRunId(runId);
        event.setSeq(seq);
        return event;
    }
}

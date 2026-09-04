package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseStreamEmitterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AgentRunAuditEventRepository eventRepository;
    private RunAuditService runAuditService;
    private SseStreamEmitter sseStreamEmitter;

    @BeforeEach
    void setUp() {
        eventRepository = mock(AgentRunAuditEventRepository.class);
        runAuditService = new RunAuditService(
            mock(AgentRunAuditRepository.class),
            eventRepository,
            objectMapper
        );
        sseStreamEmitter = new SseStreamEmitter(objectMapper, runAuditService);
    }

    @AfterEach
    void tearDown() {
        runAuditService.shutdownAuditWriteExecutor();
    }

    @Test
    void queuesCancellationAuditWhenEmitterIsAlreadyCompleted() throws Exception {
        String runId = "run-closed-emitter";
        SseEmitter emitter = new SseEmitter();
        runAuditService.registerRun(new RunAuditService.ActiveAgentRun(1L, runId, 9L, emitter));
        emitter.complete();
        assertThrows(
            IllegalStateException.class,
            () -> emitter.send(SseEmitter.event().data("closed"))
        );

        sseStreamEmitter.emitRunCancelled(emitter, runId, "用户已停止生成");

        ArgumentCaptor<AgentRunAuditEventEntity> eventCaptor =
            ArgumentCaptor.forClass(AgentRunAuditEventEntity.class);
        verify(eventRepository, timeout(1000)).save(eventCaptor.capture());
        AgentRunAuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(runId, savedEvent.getRunId());
        assertEquals(runId + ":1", savedEvent.getEventId());
        assertEquals(1, savedEvent.getSeq());
        assertEquals("run_cancelled", savedEvent.getEventType());

        JsonNode payload = objectMapper.readTree(savedEvent.getPayloadJson());
        assertEquals("run_cancelled", payload.path("event_type").asText());
        assertEquals("CANCELLED", payload.path("terminal_status").asText());
        assertEquals("用户已停止生成", payload.path("reason").asText());
        assertTrue(payload.path("timestamp").isNumber());
    }
}

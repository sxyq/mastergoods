// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import java.util.Map;
import java.io.IOException;
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

    @Test
    void preservesArgumentValidationErrorCodeOnToolFailure() throws Exception {
        String runId = "run-invalid-tool-arguments";
        SseEmitter emitter = new SseEmitter();
        runAuditService.registerRun(new RunAuditService.ActiveAgentRun(1L, runId, 9L, emitter));

        sseStreamEmitter.emitToolFailed(
            emitter,
            runId,
            "product_catalog_lookup",
            "工具参数不符合声明的参数约束（TOOL_ARGUMENTS_INVALID）",
            0L,
            System.currentTimeMillis(),
            Map.of(),
            1,
            "call-1",
            "TOOL_ARGUMENTS_INVALID"
        );

        ArgumentCaptor<AgentRunAuditEventEntity> eventCaptor =
            ArgumentCaptor.forClass(AgentRunAuditEventEntity.class);
        verify(eventRepository, timeout(1000)).save(eventCaptor.capture());
        JsonNode payload = objectMapper.readTree(eventCaptor.getValue().getPayloadJson());
        assertEquals("tool_failed", payload.path("event_type").asText());
        assertEquals("TOOL_ARGUMENTS_INVALID", payload.path("error_code").asText());
    }

    @Test
    void rollsBackSequenceWhenNonCancellationEventCannotReachClient() throws Exception {
        String runId = "run-disconnected-client";
        SseEmitter emitter = mock(SseEmitter.class);
        runAuditService.registerRun(new RunAuditService.ActiveAgentRun(1L, runId, 9L, emitter));
        doThrow(new IOException("client disconnected")).when(emitter)
            .send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));

        assertThrows(
            IOException.class,
            () -> sseStreamEmitter.sendEvent(emitter, SseStreamEmitter.eventMap("plan_delta", Map.of(
                "run_id", runId,
                "content", "plan"
            )))
        );

        // The next event can reuse the sequence that never reached the client.
        Map<String, Object> nextPayload = new java.util.LinkedHashMap<>(Map.of(
            "run_id", runId,
            "event_type", "plan_delta"
        ));
        runAuditService.prepareSend(runId, nextPayload, false);
        assertEquals(1, nextPayload.get("seq"));
        assertEquals(runId + ":1", nextPayload.get("event_id"));
    }
}

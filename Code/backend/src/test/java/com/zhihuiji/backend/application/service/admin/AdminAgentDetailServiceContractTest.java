package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentCheckpointQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentDetailQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentDraftQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentEventQueryRepository;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminAgentDetailServiceContractTest {
    @Mock
    private AdminAuthorizationService authorizationService;
    @Mock
    private AdminAgentQueryRepository runRepository;
    @Mock
    private AdminAgentDetailQueryRepository messageRepository;
    @Mock
    private AdminAgentEventQueryRepository eventRepository;
    @Mock
    private AdminAgentCheckpointQueryRepository checkpointRepository;
    @Mock
    private AdminAgentDraftQueryRepository draftRepository;

    private AdminAgentDetailService service;
    private AdminPrincipal observer;
    private AdminPrincipal superAdmin;
    private AgentRunAuditEntity run;

    @BeforeEach
    void setUp() {
        service = new AdminAgentDetailService(
            authorizationService,
            runRepository,
            messageRepository,
            eventRepository,
            checkpointRepository,
            draftRepository,
            new ObjectMapper()
        );
        observer = AdminPrincipal.forRole(
            900L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );
        superAdmin = AdminPrincipal.forRole(
            901L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );
        run = new AgentRunAuditEntity();
        run.setRunId("run-1");
        run.setOwnerUserId(101L);
        run.setConversationId(9007199254740993L);
        run.setStartedAt(1000L);
        run.setCompletedAt(3000L);
        run.setStatus("completed");
        run.setToolCount(2);
    }

    @Test
    void messagesKeepLargeConversationIdsAsStringsAndClampPageSize() {
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, 101L, null))
            .thenReturn(observer.scope());
        AgentMessageEntity message = new AgentMessageEntity();
        message.setOwnerUserId(101L);
        message.setConversationId(9007199254740993L);
        message.setRunId("run-1");
        message.setRole("assistant");
        message.setMessageType("text");
        message.setContent("password:top-secret");
        message.setCreatedAt(2000L);
        when(messageRepository.findMessages(
            eq(9007199254740993L), eq(false), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(message), PageRequest.of(3, 200), 201));

        AdminPageDtos.PageResponse<AdminAgentDtos.Message> response = service.messages(
            observer, "9007199254740993", false, -1, 999, 101L, null
        );

        assertEquals("9007199254740993", response.items().get(0).conversationId());
        assertEquals("REDACTED", response.items().get(0).content());
        assertEquals("REDACTED", response.items().get(0).redactionState());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findMessages(eq(9007199254740993L), eq(false), any(), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(200, pageable.getValue().getPageSize());
    }

    @Test
    void invalidConversationIdDoesNotReachRepository() {
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, null, null))
            .thenReturn(observer.scope());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.messages(observer, "9007199254740993x", false, 0, 50, null, null)
        );
        verifyNoInteractions(messageRepository);
    }

    @Test
    void observerCannotRequestContentAndNoContentQueryIsPerformed() {
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, null, null))
            .thenReturn(observer.scope());
        org.mockito.Mockito.doThrow(new AccessDeniedException("administrator permission denied"))
            .when(authorizationService).requirePermission(observer, AdminPermission.AGENT_CONTENT_READ);

        assertThrows(
            AccessDeniedException.class,
            () -> service.events(observer, "run-1", null, true, null, null)
        );
        verifyNoInteractions(eventRepository);
    }

    @Test
    void eventReplayUsesSequenceCursorAndFlagsGaps() {
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, null, null))
            .thenReturn(observer.scope());
        when(runRepository.findRun(eq("run-1"), eq(false), any())).thenReturn(java.util.Optional.of(run));
        when(eventRepository.findEvents(eq("run-1"), eq(false), any(), eq(2))).thenReturn(List.of(
            event(3, "tool_progress", "tool_name: inventory", "call_id: c-3"),
            event(5, "run_completed", "", "")
        ));

        AdminAgentDtos.EventPage response = service.events(observer, "run-1", 2, false, null, null);

        assertEquals(List.of(3L, 5L), response.items().stream()
            .map(AdminAgentDtos.Event::sequence).toList());
        assertFalse(response.eventIntegrity());
        assertEquals("inventory", response.items().get(0).toolName());
        assertEquals("REDACTED", response.items().get(0).redactionState().name());
        verify(eventRepository).findEvents(eq("run-1"), eq(false), any(), eq(2));
    }

    @Test
    void duplicateSequenceIsReportedAsCorruptAndSensitiveSummariesAreRedacted() {
        when(authorizationService.authorize(superAdmin, AdminPermission.AGENT_RUN_READ, null, null))
            .thenReturn(superAdmin.scope());
        when(runRepository.findRun(eq("run-1"), eq(true), any())).thenReturn(java.util.Optional.of(run));
        when(eventRepository.findEvents(eq("run-1"), eq(true), any(), eq(null))).thenReturn(List.of(
            event(1, "tool_started", "api_key:secret-value", "token:result-secret"),
            event(1, "tool_completed", "input", "output")
        ));

        AdminAgentDtos.EventPage response = service.events(superAdmin, "run-1", null, true, null, null);

        assertFalse(response.eventIntegrity());
        assertEquals("api_key:***", response.items().get(0).argumentSummary());
        assertEquals("token:***", response.items().get(0).resultSummary());
        assertEquals("PARTIAL", response.items().get(0).redactionState().name());
    }

    @Test
    void visibleRunRejectsStoreScopedObserverWhenPersistedRunHasNoStoreField() {
        AdminDataScope storeScope = new AdminDataScope(
            false, Set.of(101L), Set.of(501L), false, AdminDataScope.ContentMode.REDACTED
        );
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, null, 501L))
            .thenReturn(storeScope);

        assertThrows(
            IllegalStateException.class,
            () -> service.events(observer, "run-1", null, false, null, 501L)
        );
        verifyNoInteractions(runRepository, eventRepository);
    }

    @Test
    void usageMarksTokenEstimatesAndKeepsDurationBoundedToRun() {
        when(authorizationService.authorize(observer, AdminPermission.AGENT_RUN_READ, null, null))
            .thenReturn(observer.scope());
        AgentRunAuditEventEntity tokenEvent = event(1, "run_completed", "", "");
        tokenEvent.setPayloadJson("{\"input_token_estimate\":12,\"output_token_estimate\":8}");
        when(runRepository.findRuns(eq(false), any(), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(run), PageRequest.of(0, 50), 1));
        when(eventRepository.findEvents(eq("run-1"), eq(false), any(), eq(null))).thenReturn(List.of(tokenEvent));

        AdminAgentDtos.Usage usage = service.usage(observer, null, null, null, null, 0, 50).items().get(0);

        assertEquals(12L, usage.inputTokens());
        assertEquals(8L, usage.outputTokens());
        assertEquals(20L, usage.totalTokens());
        assertEquals(2000L, usage.durationMs());
        assertEquals(AdminAgentDtos.TokenSource.ESTIMATED, usage.tokenSource());
        assertTrue(usage.estimated());
    }

    private AgentRunAuditEventEntity event(int sequence, String type, String input, String result) {
        AgentRunAuditEventEntity event = new AgentRunAuditEventEntity();
        event.setRunId("run-1");
        event.setEventId("event-" + sequence + "-" + type);
        event.setSeq(sequence);
        event.setEventType(type);
        event.setPayloadJson("{\"tool_name\":\"inventory\",\"input_summary\":\""
            + input + "\",\"result_summary\":\"" + result + "\"}");
        event.setCreatedAt(2000L + sequence);
        return event;
    }
}

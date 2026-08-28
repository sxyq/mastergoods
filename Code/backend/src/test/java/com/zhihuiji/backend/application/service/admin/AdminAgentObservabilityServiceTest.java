package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminAgentQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAgentObservabilityServiceTest {
    @Mock
    private AdminAuthorizationService authorizationService;
    @Mock
    private AdminAgentQueryRepository agentQueryRepository;

    private AdminAgentObservabilityService service;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new AdminAgentObservabilityService(authorizationService, agentQueryRepository);
        principal = AdminPrincipal.forRole(
            900L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );
    }

    @Test
    void detailMapsMissingActorStoreModelAndUsageToExplicitUnknownValues() {
        AgentRunAuditEntity run = new AgentRunAuditEntity();
        run.setRunId("run-1");
        run.setOwnerUserId(101L);
        run.setConversationId(601L);
        run.setStatus("completed");
        run.setToolCount(2);
        run.setStartedAt(1000L);
        run.setCompletedAt(3000L);
        when(authorizationService.authorize(
            principal, AdminPermission.AGENT_RUN_READ, null, null
        )).thenReturn(principal.scope());
        when(agentQueryRepository.findRun("run-1", false, Set.of(101L))).thenReturn(Optional.of(run));

        var response = service.getRun(principal, "run-1", null, null);

        assertEquals("101", response.ownerUserId());
        assertEquals("601", response.conversationId());
        assertNull(response.actorUserId());
        assertNull(response.storeId());
        assertNull(response.modelId());
        assertNull(response.inputTokens());
        assertEquals("UNAVAILABLE", response.tokenSource().name());
        assertEquals("PARTIAL", response.scopeCompleteness());
        assertEquals(2000L, response.durationMs());
    }

    @Test
    void detailRejectsStoreRestrictedScopeBeforeUnscopedRunLookup() {
        AdminDataScope restricted = new AdminDataScope(
            false,
            Set.of(101L),
            Set.of(501L),
            false,
            AdminDataScope.ContentMode.REDACTED
        );
        when(authorizationService.authorize(
            principal, AdminPermission.AGENT_RUN_READ, null, 501L
        )).thenReturn(restricted);

        assertThrows(
            IllegalStateException.class,
            () -> service.getRun(principal, "run-1", null, 501L)
        );
    }
}

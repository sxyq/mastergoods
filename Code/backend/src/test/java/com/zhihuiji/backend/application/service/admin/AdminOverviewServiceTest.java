package com.zhihuiji.backend.application.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.admin.AdminOverviewDtos;
import com.zhihuiji.backend.infrastructure.repository.admin.AdminOverviewQueryRepository;
import com.zhihuiji.backend.infrastructure.security.admin.AdminAuthorizationService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPermission;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {
    @Mock
    private AdminAuthorizationService authorizationService;
    @Mock
    private AdminOverviewQueryRepository overviewQueryRepository;

    private AdminOverviewService overviewService;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        overviewService = new AdminOverviewService(authorizationService, overviewQueryRepository);
        principal = AdminPrincipal.forRole(
            900L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );
    }

    @Test
    void overviewUsesServerScopeAndReturnsBoundedTrend() {
        when(authorizationService.authorize(
            principal, AdminPermission.DASHBOARD_READ, null, null
        )).thenReturn(principal.scope());
        when(overviewQueryRepository.countUsers(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(2L);
        when(overviewQueryRepository.countStores(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(1L);
        when(overviewQueryRepository.countAgentRuns(any(Boolean.class), any(), any(), any())).thenReturn(4L);
        when(overviewQueryRepository.sumAgentToolCount(any(Boolean.class), any(), any(), any())).thenReturn(3L);
        when(overviewQueryRepository.averageAgentRunDuration(any(Boolean.class), any(), any(), any())).thenReturn(null);
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-03T00:00:00Z");

        var response = overviewService.overview(principal, from, to, null, null);

        assertEquals(from, response.from());
        assertEquals(to, response.to());
        assertEquals(7, response.metrics().size());
        assertEquals(2, response.trend().size());
        assertEquals("COMPLETE", response.scopeCompleteness());
        assertEquals(true, response.scope().allOwners());
        assertNull(metric(response, "agent_success_rate").value());
        assertEquals("UNAVAILABLE", metric(response, "agent_average_latency").availability());
    }

    @Test
    void overviewFiltersAgentMetricsByPersistedStoreScope() {
        AdminDataScope restricted = new AdminDataScope(
            false,
            Set.of(101L),
            Set.of(501L),
            false,
            AdminDataScope.ContentMode.REDACTED
        );
        when(authorizationService.authorize(
            principal, AdminPermission.DASHBOARD_READ, null, 501L
        )).thenReturn(restricted);

        when(overviewQueryRepository.countUsers(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(1L);
        when(overviewQueryRepository.countStores(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(1L);
        when(overviewQueryRepository.countAgentRuns(any(Boolean.class), any(), any(Boolean.class), any(), any(), any())).thenReturn(2L);
        when(overviewQueryRepository.sumAgentToolCount(any(Boolean.class), any(), any(Boolean.class), any(), any(), any())).thenReturn(3L);
        var response = overviewService.overview(principal, Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-02T00:00:00Z"), null, 501L);
        assertEquals(2L, response.metrics().stream().filter(m -> m.key().equals("agent_runs")).findFirst().orElseThrow().value());
    }

    @Test
    void overviewReportsReliableRunMetricsWithoutInventingTokenValues() {
        when(authorizationService.authorize(
            principal, AdminPermission.DASHBOARD_READ, null, null
        )).thenReturn(principal.scope());
        when(overviewQueryRepository.countUsers(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(2L);
        when(overviewQueryRepository.countStores(any(Boolean.class), any(), any(), any(Boolean.class))).thenReturn(1L);
        when(overviewQueryRepository.countAgentRuns(any(Boolean.class), any(), any(), any())).thenReturn(4L);
        when(overviewQueryRepository.sumAgentToolCount(any(Boolean.class), any(), any(), any())).thenReturn(3L);
        when(overviewQueryRepository.countAgentRunsByStatus(any(Boolean.class), any(), eq("running"), any(), any())).thenReturn(1L);
        when(overviewQueryRepository.countAgentRunsByStatus(any(Boolean.class), any(), eq("completed"), any(), any())).thenReturn(3L);
        when(overviewQueryRepository.countAgentRunsByStatuses(any(Boolean.class), any(), any(), any(), any())).thenReturn(4L);
        when(overviewQueryRepository.averageAgentRunDuration(any(Boolean.class), any(), any(), any())).thenReturn(1500.0);

        var response = overviewService.overview(
            principal,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-02T00:00:00Z"),
            null,
            null
        );

        assertEquals(1L, metric(response, "agent_active_runs").value());
        assertEquals(75.0, metric(response, "agent_success_rate").value());
        assertEquals(1500.0, metric(response, "agent_average_latency").value());
        assertEquals("AVAILABLE", metric(response, "agent_success_rate").availability());
    }

    private AdminOverviewDtos.Metric metric(AdminOverviewDtos.OverviewResponse response, String key) {
        return response.metrics().stream().filter(item -> item.key().equals(key)).findFirst().orElseThrow();
    }
}

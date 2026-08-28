package com.zhihuiji.backend.api.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminOrganizationDtos;
import com.zhihuiji.backend.api.dto.admin.AdminOverviewDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.application.service.admin.AdminAgentObservabilityService;
import com.zhihuiji.backend.application.service.admin.AdminOrganizationService;
import com.zhihuiji.backend.application.service.admin.AdminOverviewService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminReadControllerTest {
    @Mock
    private AdminPrincipalResolver principalResolver;
    @Mock
    private AdminOverviewService overviewService;
    @Mock
    private AdminOrganizationService organizationService;
    @Mock
    private AdminAgentObservabilityService observabilityService;

    private MockMvc mockMvc;
    private AdminPrincipal principal;
    private AdminScopeDtos.Scope scope;

    @BeforeEach
    void setUp() {
        principal = AdminPrincipal.forRole(
            900L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );
        scope = AdminScopeDtos.Scope.from(principal.scope());
        when(principalResolver.requireCurrent()).thenReturn(principal);
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        mockMvc = MockMvcBuilders.standaloneSetup(
            new AdminOverviewController(overviewService, principalResolver),
            new AdminOrganizationController(organizationService, principalResolver),
            new AdminAgentController(observabilityService, principalResolver)
        ).setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
    }

    @Test
    void overviewRouteDelegatesToOverviewService() throws Exception {
        when(overviewService.overview(any(), any(), any(), any(), any())).thenReturn(
            new AdminOverviewDtos.OverviewResponse(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-02T00:00:00Z"),
                List.of(new AdminOverviewDtos.Metric("agent_runs", 2L, "count")),
                List.of(),
                false,
                "COMPLETE",
                Instant.parse("2026-08-02T00:00:00Z"),
                scope
            )
        );

        mockMvc.perform(get("/v2/admin/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metrics[0].key").value("agent_runs"))
            .andExpect(jsonPath("$.data.scope.all_owners").value(true));
    }

    @Test
    void organizationRoutesReturnStringIdsAndPageEnvelope() throws Exception {
        AdminPageDtos.PageResponse<AdminOrganizationDtos.UserSummary> users = new AdminPageDtos.PageResponse<>(
            List.of(new AdminOrganizationDtos.UserSummary(
                "101", "*******8000", "用户", "ACTIVE", Instant.EPOCH, Instant.EPOCH
            )),
            0, 50, 1, false, Instant.EPOCH, scope, "COMPLETE"
        );
        when(organizationService.listUsers(any(), any(), any(), any(), any(), any())).thenReturn(users);

        mockMvc.perform(get("/v2/admin/users").param("query", "用户"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].user_id").value("101"))
            .andExpect(jsonPath("$.data.items[0].phone_masked").value("*******8000"));
    }

    @Test
    void agentDetailRouteReturnsUnknownFieldsAsNull() throws Exception {
        AdminAgentDtos.RunSummary run = new AdminAgentDtos.RunSummary(
            "run-1", null, null, "101", null, "COMPLETED", null,
            Instant.EPOCH, Instant.EPOCH, 0L, null, null, 0,
            null, null, null, AdminAgentDtos.TokenSource.UNAVAILABLE, true, "PARTIAL"
        );
        when(observabilityService.getRun(any(), any(), any(), any())).thenReturn(run);

        mockMvc.perform(get("/v2/admin/agent/runs/run-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.run_id").value("run-1"))
            .andExpect(jsonPath("$.data.actor_user_id").doesNotExist())
            .andExpect(jsonPath("$.data.store_id").doesNotExist())
            .andExpect(jsonPath("$.data.token_source").value("UNAVAILABLE"));
    }
}

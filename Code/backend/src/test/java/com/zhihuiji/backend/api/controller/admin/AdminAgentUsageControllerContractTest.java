package com.zhihuiji.backend.api.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminScopeDtos;
import com.zhihuiji.backend.application.service.admin.AdminAgentDetailService;
import com.zhihuiji.backend.application.service.admin.AdminAgentObservabilityService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipal;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAgentUsageControllerContractTest {
    @Mock
    private AdminAgentDetailService detailService;
    @Mock
    private AdminAgentObservabilityService observabilityService;
    @Mock
    private AdminPrincipalResolver principalResolver;

    private MockMvc mockMvc;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = AdminPrincipal.forRole(
            901L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );
        when(principalResolver.requireCurrent()).thenReturn(principal);
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        mockMvc = MockMvcBuilders.standaloneSetup(
            new AdminAgentController(observabilityService, detailService, principalResolver, objectMapper)
        ).setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
    }

    @Test
    void usageRoutePassesModelAndGranularityAndReturnsAggregateFields() throws Exception {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-02T00:00:00Z");
        AdminAgentDtos.Usage usage = new AdminAgentDtos.Usage(
            null,
            "model-a",
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-02T00:00:00Z"),
            2L,
            150L,
            60L,
            210L,
            3_000L,
            33L,
            3_000L,
            4_000L,
            33L,
            40L,
            AdminAgentDtos.TokenSource.ESTIMATED,
            true,
            "COMPLETE"
        );
        AdminAgentDtos.UsagePage response = new AdminAgentDtos.UsagePage(
            List.of(usage),
            1L,
            Instant.parse("2026-08-02T00:00:00Z"),
            from,
            to,
            "DAY",
            AdminScopeDtos.Scope.from(principal.scope()),
            "COMPLETE"
        );
        when(detailService.usage(
            any(AdminPrincipal.class),
            any(Instant.class),
            any(Instant.class),
            any(String.class),
            any(String.class),
            any(Long.class),
            any(Long.class),
            any(Integer.class),
            any(Integer.class)
        ))
            .thenReturn(response);

        mockMvc.perform(get("/v2/admin/agent/usage")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("modelId", "model-a")
                .param("granularity", "DAY")
                .param("ownerUserId", "101")
                .param("storeId", "501")
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].model_id").value("model-a"))
            .andExpect(jsonPath("$.data.items[0].request_count").value(2))
            .andExpect(jsonPath("$.data.items[0].input_tokens").value(150))
            .andExpect(jsonPath("$.data.items[0].output_tokens").value(60))
            .andExpect(jsonPath("$.data.items[0].total_tokens").value(210))
            .andExpect(jsonPath("$.data.items[0].average_duration_ms").value(3000))
            .andExpect(jsonPath("$.data.items[0].p95_duration_ms").value(4000))
            .andExpect(jsonPath("$.data.items[0].average_time_to_first_token_ms").value(33))
            .andExpect(jsonPath("$.data.items[0].p95_time_to_first_token_ms").value(40))
            .andExpect(jsonPath("$.data.items[0].token_source").value("ESTIMATED"))
            .andExpect(jsonPath("$.data.items[0].estimated").value(true))
            .andExpect(jsonPath("$.data.granularity").value("DAY"));

        verify(detailService).usage(principal, from, to, "model-a", "DAY", 101L, 501L, 0, 50);
    }
}

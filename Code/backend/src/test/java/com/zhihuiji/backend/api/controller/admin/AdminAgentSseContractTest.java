package com.zhihuiji.backend.api.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAgentSseContractTest {
    @Mock
    private AdminAgentObservabilityService observabilityService;
    @Mock
    private AdminAgentDetailService detailService;
    @Mock
    private AdminPrincipalResolver principalResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminPrincipal principal = AdminPrincipal.forRole(
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
    void streamReplaysAfterLastEventIdAndCompletesAtTerminalEvent() throws Exception {
        when(detailService.events(any(), eq("run-1"), eq(8), eq(false), eq(null), eq(null)))
            .thenReturn(new AdminAgentDtos.EventPage(List.of(
                event(9, "tool_progress"), event(10, "run_completed")
            ), 2, true));

        MvcResult initial = mockMvc.perform(get("/v2/admin/agent/runs/run-1/events/stream")
                .header("Last-Event-ID", "8"))
            .andExpect(request().asyncStarted())
            .andReturn();
        MvcResult result = mockMvc.perform(asyncDispatch(initial))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("id:9"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:tool_progress"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("id:10"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:run_completed"));
    }

    @Test
    void explicitAfterSequenceTakesPrecedenceOverLastEventId() throws Exception {
        when(detailService.events(any(), eq("run-1"), eq(12), eq(false), eq(null), eq(null)))
            .thenReturn(new AdminAgentDtos.EventPage(List.of(event(13, "run_failed")), 1, true));

        mockMvc.perform(get("/v2/admin/agent/runs/run-1/events/stream")
                .header("Last-Event-ID", "8")
                .param("afterSequence", "12"))
            .andExpect(status().isOk());
    }

    @Test
    void malformedLastEventIdStartsFromBeginning() throws Exception {
        when(detailService.events(any(), eq("run-1"), eq(null), eq(false), eq(null), eq(null)))
            .thenReturn(new AdminAgentDtos.EventPage(List.of(event(1, "run_started")), 1, true));

        mockMvc.perform(get("/v2/admin/agent/runs/run-1/events/stream")
                .header("Last-Event-ID", "not-a-sequence"))
            .andExpect(status().isOk());
    }

    private AdminAgentDtos.Event event(long sequence, String type) {
        return new AdminAgentDtos.Event(
            "event-" + sequence, "run-1", sequence, type, "inventory", "call-1",
            Instant.parse("2026-08-29T00:00:00Z"), "COMPLETED", null, null, null,
            AdminAgentDtos.RedactionState.PARTIAL
        );
    }
}

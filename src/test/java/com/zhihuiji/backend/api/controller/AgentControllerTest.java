package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.application.service.LlmDrivenAgentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmDrivenAgentService agentService;

    @Test
    void workbenchReturnsSuccess() throws Exception {
        AgentDto.ReportInsightDto insight = new AgentDto.ReportInsightDto(
            "7d",
            10,
            8,
            25,
            "narrative",
            "S7",
            10,
            "customer-a",
            10,
            List.of("h1"),
            List.of("a1")
        );
        when(agentService.getWorkbench(anyInt(), anyInt(), anyInt())).thenReturn(
            new AgentDto.AgentWorkbenchDto(
                new AgentDto.ReconciliationFollowupDto(1, 2, 3, 4, -1, List.of(), List.of(), List.of()),
                insight,
                new AgentDto.AlertDashboardDto(List.of()),
                List.of("q1"),
                List.of("i1")
            )
        );

        mockMvc.perform(get("/v1/agent/workbench"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.reportInsight.leadingProductName").value("S7"));
    }

    @Test
    void queryReturnsSuccess() throws Exception {
        AgentDto.AgentAnswerDto answer = new AgentDto.AgentAnswerDto(
            "who owes the most",
            "receivables",
            "answer",
            List.of("h1"),
            List.of("customer"),
            List.of(List.of("customer-a")),
            List.of("a1")
        );
        when(agentService.answerQuestion(anyString())).thenReturn(answer);
        String body = objectMapper.writeValueAsString(new AgentController.QueryRequest("who owes the most"));

        mockMvc.perform(post("/v1/agent/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.intent").value("receivables"));
    }
}

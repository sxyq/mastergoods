package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AgentTaskDtos;
import com.zhihuiji.backend.api.dto.agent.AlertDtos;
import com.zhihuiji.backend.api.dto.agent.AnswerDtos;
import com.zhihuiji.backend.api.dto.agent.OperationDraftDtos;
import com.zhihuiji.backend.api.dto.agent.ReconciliationDtos;
import com.zhihuiji.backend.api.dto.agent.WorkbenchDtos;
import com.zhihuiji.backend.application.service.AgentTaskService;
import com.zhihuiji.backend.application.service.DemoDataService;
import com.zhihuiji.backend.application.service.LlmDrivenAgentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({ "test", "local" })
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DemoDataService demoDataService;

    @MockBean
    private LlmDrivenAgentService llmDrivenAgentService;
    @MockBean
    private AgentTaskService agentTaskService;

    @BeforeEach
    void setUp() {
        demoDataService.seed(true);
        when(llmDrivenAgentService.getWorkbench(anyInt(), anyInt(), anyInt())).thenReturn(
            new WorkbenchDtos.AgentWorkbenchDto(
                new ReconciliationDtos.ReconciliationFollowupDto(1, 2, 3, 4, 5, List.of(), List.of(), List.of()),
                new ReconciliationDtos.ReportInsightDto("7d", 10, 8, 25, "narrative", "S7", 10, "customer-a", 9, List.of(), List.of()),
                new AlertDtos.AlertDashboardDto(List.of()),
                List.of("q1"),
                List.of("i1")
            )
        );
        when(llmDrivenAgentService.answerQuestion(anyString())).thenReturn(
            new AnswerDtos.AgentAnswerDto("query", "intent", "answer", List.of(), List.of(), List.of(), List.of())
        );
        when(llmDrivenAgentService.draftOperation(anyString())).thenReturn(
            new OperationDraftDtos.OperationDraftDto("purchase", "draft summary", "supplier", 1L, "供应商A", List.of(), "", true, List.of(), List.of())
        );
        when(agentTaskService.submitTask(anyString(), anyString(), anyString())).thenReturn(
            new AgentTaskDtos.AgentTaskSummaryDto(99L, "sales_report_deep_dive", "后台报表复盘 smoke", "queued", "manual", 0, now(), now(), null)
        );
        when(agentTaskService.getTask(99L)).thenReturn(
            new AgentTaskDtos.AgentTaskDetailDto(
                new AgentTaskDtos.AgentTaskSummaryDto(99L, "sales_report_deep_dive", "后台报表复盘 smoke", "completed", "manual", 100, now(), now(), now()),
                "请复盘近 7 天销售趋势、客户贡献、利润驱动和补货机会。",
                new AgentTaskDtos.AgentTaskResultDto(
                    "title",
                    "subtitle",
                    "task summary",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of()
                )
            )
        );
    }

    @Test
    void summaryReturnsSeededCounts() throws Exception {
        mockMvc.perform(get("/v1/admin/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.userCount").value(4))
            .andExpect(jsonPath("$.data.productCount").value(5))
            .andExpect(jsonPath("$.data.customerCount").value(3))
            .andExpect(jsonPath("$.data.supplierCount").value(3))
            .andExpect(jsonPath("$.data.saleOrderCount").value(3))
            .andExpect(jsonPath("$.data.purchaseOrderCount").value(2))
            .andExpect(jsonPath("$.data.agentTaskCount").value(1))
            .andExpect(jsonPath("$.data.unreadNotificationCount").value(1));
    }

    @Test
    void seedResetRebuildsDemoData() throws Exception {
        mockMvc.perform(post("/v1/admin/demo/seed").param("reset", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.mutated").value(true))
            .andExpect(jsonPath("$.data.userCount").value(4))
            .andExpect(jsonPath("$.data.productCount").value(5))
            .andExpect(jsonPath("$.data.demoAccounts[0].phone").value("13800138111"));
    }

    @Test
    void createAndUpdateUserRoundTripWorks() throws Exception {
        String createBody = objectMapper.writeValueAsString(
            Map.of(
                "phone",
                "13800138991",
                "password",
                "123456",
                "nickname",
                "管理测试用户",
                "status",
                1
            )
        );

        String createResponse = mockMvc.perform(post("/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.phone").value("13800138991"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        String updateBody = objectMapper.writeValueAsString(
            Map.of(
                "nickname",
                "管理测试用户-已更新",
                "status",
                0,
                "password",
                "654321",
                "keepSessions",
                false
            )
        );

        mockMvc.perform(put("/v1/admin/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.nickname").value("管理测试用户-已更新"))
            .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(get("/v1/admin/users").param("keyword", "已更新"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].phone").value("13800138991"));
    }

    @Test
    void createUserRejectsEmptyNickname() throws Exception {
        String body = objectMapper.writeValueAsString(
            Map.of(
                "phone",
                "13800138992",
                "password",
                "123456",
                "nickname",
                " ",
                "status",
                1
            )
        );

        mockMvc.perform(post("/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(422))
            .andExpect(jsonPath("$.message").value("nickname is required"));
    }

    @Test
    void createUserRejectsDuplicatePhone() throws Exception {
        String body = objectMapper.writeValueAsString(
            Map.of(
                "phone",
                "13800138111",
                "password",
                "123456",
                "nickname",
                "重复账号",
                "status",
                1
            )
        );

        mockMvc.perform(post("/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(422))
            .andExpect(jsonPath("$.message").value("phone already registered"));
    }

    @Test
    void agentSmokeReturnsReadablePayload() throws Exception {
        mockMvc.perform(post("/v1/admin/agent/smoke"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.workbenchNarrative").value("narrative"))
            .andExpect(jsonPath("$.data.answerSummary").value("answer"))
            .andExpect(jsonPath("$.data.taskStatus").value("completed"))
            .andExpect(jsonPath("$.data.taskSummary").value("task summary"));
    }

    private long now() {
        return System.currentTimeMillis();
    }
}

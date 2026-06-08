package com.zhihuiji.backend.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.DemoDataService;
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

    @BeforeEach
    void setUp() {
        demoDataService.seed(true);
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
            .andExpect(jsonPath("$.data.agentTaskCount").value(0))
            .andExpect(jsonPath("$.data.unreadNotificationCount").value(0));
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
    void agentSmokeIsGoneBecauseItDoesNotExerciseRealAgentFlow() throws Exception {
        mockMvc.perform(post("/v1/admin/agent/smoke"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value(410))
            .andExpect(jsonPath("$.message").value("Use authenticated /v2/agent/chat or /v2/agent/chat/stream with real owner-scoped data."));
    }
}

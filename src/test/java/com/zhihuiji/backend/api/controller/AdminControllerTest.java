package com.zhihuiji.backend.api.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.DemoDataService;
import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgentTaskRepository agentTaskRepository;
    @Autowired
    private AgentNotificationRepository agentNotificationRepository;

    @BeforeEach
    void setUp() {
        agentNotificationRepository.deleteAll();
        agentTaskRepository.deleteAll();
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
    void seedResetDoesNotDeleteNonDemoOwnerAgentArtifacts() throws Exception {
        UserEntity realOwner = new UserEntity();
        realOwner.setPhone("13800138999");
        realOwner.setPasswordHash("hash");
        realOwner.setNickname("真实经营账号");
        realOwner.setStatus(1);
        realOwner.setCreatedAt(System.currentTimeMillis());
        realOwner.setUpdatedAt(System.currentTimeMillis());
        realOwner = userRepository.save(realOwner);
        AgentTaskEntity task = agentTask(realOwner.getId());
        task = agentTaskRepository.save(task);
        AgentNotificationEntity notification = agentNotification(realOwner.getId(), task.getId());
        notification = agentNotificationRepository.save(notification);

        mockMvc.perform(post("/v1/admin/demo/seed").param("reset", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/v1/admin/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agentTaskCount").value(1))
            .andExpect(jsonPath("$.data.unreadNotificationCount").value(1));
        assertTrue(agentTaskRepository.findById(task.getId()).isPresent());
        assertTrue(agentNotificationRepository.findById(notification.getId()).isPresent());
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

    private static AgentTaskEntity agentTask(Long ownerUserId) {
        AgentTaskEntity entity = new AgentTaskEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setTaskType("real_run");
        entity.setTitle("真实 run 任务");
        entity.setTriggerSource("agent_run");
        entity.setStatus("completed");
        entity.setProgress(100);
        entity.setInputText("真实查询");
        entity.setResultJson("{}");
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setUpdatedAt(System.currentTimeMillis());
        entity.setCompletedAt(System.currentTimeMillis());
        return entity;
    }

    private static AgentNotificationEntity agentNotification(Long ownerUserId, Long taskId) {
        AgentNotificationEntity entity = new AgentNotificationEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setTaskId(taskId);
        entity.setTitle("真实通知");
        entity.setBody("真实 run 完成");
        entity.setLevel("info");
        entity.setIsRead(false);
        entity.setIsDelivered(false);
        entity.setCreatedAt(System.currentTimeMillis());
        return entity;
    }
}

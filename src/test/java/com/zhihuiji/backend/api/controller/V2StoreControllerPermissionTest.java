package com.zhihuiji.backend.api.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2StoreController;
import com.zhihuiji.backend.api.dto.v2.store.V2StoreDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2StoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = V2StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
class V2StoreControllerPermissionTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2StoreService v2StoreService;
    @MockBean
    private CurrentOwnerService currentOwnerService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void currentStoreRemainsReadableForAuthenticatedMember() throws Exception {
        when(v2StoreService.getCurrentStore()).thenReturn(new V2StoreDtos.CurrentStoreResponse(
            1L,
            "智慧记总店",
            9L,
            11L,
            "销售小林",
            "13800000003",
            "SALES",
            "销售员工",
            1,
            java.util.List.of("dashboard:view", "sales:view"),
            6,
            5,
            1
        ));

        mockMvc.perform(get("/v2/stores/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("SALES"))
            .andExpect(jsonPath("$.data.store_name").value("智慧记总店"));
    }

    @Test
    void membersEndpointReturns403WhenPermissionMissing() throws Exception {
        doThrow(new AccessDeniedException("当前账号缺少权限: users:manage"))
            .when(currentOwnerService)
            .requirePermissions("users:manage");

        mockMvc.perform(get("/v2/stores/current/members").header("Authorization", "Bearer demo-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("当前账号缺少权限: users:manage"));
    }
}

package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2CustomerContactController;
import com.zhihuiji.backend.api.controller.v2.V2CustomerController;
import com.zhihuiji.backend.api.controller.v2.V2CustomerGroupController;
import com.zhihuiji.backend.api.controller.v2.V2SupplierContactController;
import com.zhihuiji.backend.api.controller.v2.V2SupplierController;
import com.zhihuiji.backend.api.controller.v2.V2SupplierGroupController;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2CustomerService;
import com.zhihuiji.backend.application.service.v2.V2PartnerContactService;
import com.zhihuiji.backend.application.service.v2.V2PartnerGroupService;
import com.zhihuiji.backend.application.service.v2.V2SupplierService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2CustomerController.class,
    V2SupplierController.class,
    V2CustomerGroupController.class,
    V2SupplierGroupController.class,
    V2CustomerContactController.class,
    V2SupplierContactController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2PartnerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2CustomerService v2CustomerService;
    @MockBean
    private V2SupplierService v2SupplierService;
    @MockBean
    private V2PartnerGroupService v2PartnerGroupService;
    @MockBean
    private V2PartnerContactService v2PartnerContactService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void customerListReturnsGroupAndPrimaryContactSummary() throws Exception {
        when(v2CustomerService.list(null, null, null)).thenReturn(List.of(
            new V2PartnerDtos.CustomerResponse(1L, "李老板", "13800000000", 3, 7L, "批发客户", "张三", "13900000000", "重庆", "备注", 120.0, 1, 1L, 2L)
        ));

        mockMvc.perform(get("/v2/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].group_name").value("批发客户"))
            .andExpect(jsonPath("$.data[0].primary_contact_name").value("张三"))
            .andExpect(jsonPath("$.data[0].primary_contact_phone").value("13900000000"));
    }

    @Test
    void customerGroupCreateUsesRequestDto() throws Exception {
        when(v2PartnerGroupService.create(any(), any())).thenReturn(
            new V2PartnerDtos.PartnerGroupResponse(7L, "customer", "批发客户", 1, 0, 1L, 2L)
        );

        mockMvc.perform(post("/v2/customer-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "批发客户",
                      "status": 1,
                      "sort_order": 0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.partner_type").value("customer"))
            .andExpect(jsonPath("$.data.name").value("批发客户"));
    }
}

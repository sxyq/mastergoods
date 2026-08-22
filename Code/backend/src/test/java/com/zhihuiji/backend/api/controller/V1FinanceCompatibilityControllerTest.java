package com.zhihuiji.backend.api.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.application.service.FinanceRecordService;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;

@WebMvcTest(controllers = FinanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class V1FinanceCompatibilityControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinanceRecordService financeRecordService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void v1FinanceRecordsDoNotExposeB04ExpansionFields() throws Exception {
        FinanceRecordEntity entity = new FinanceRecordEntity();
        entity.setId(1L);
        entity.setOwnerUserId(1L);
        entity.setRecordNo("FR001");
        entity.setType(1);
        entity.setCategory("销售收款");
        entity.setPartnerName("客户A");
        entity.setAmount(30.0);
        entity.setMethod(1);
        entity.setNotes("备注");
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        when(financeRecordService.list(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(List.of(entity));

        mockMvc.perform(get("/v1/finance-records"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].recordNo").value("FR001"))
            .andExpect(jsonPath("$.data[0].account_id").doesNotExist())
            .andExpect(jsonPath("$.data[0].account_name").doesNotExist())
            .andExpect(jsonPath("$.data[0].link_type").doesNotExist())
            .andExpect(jsonPath("$.data[0].balance").doesNotExist());
    }
}

package com.zhihuiji.backend.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.application.service.ReportService;
import com.zhihuiji.backend.application.service.SessionAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void cashflowSummaryReturnsFinanceRecordAggregateContract() throws Exception {
        when(reportService.cashflowSummary(1_000L, 2_000L)).thenReturn(
            new ReportDto.CashflowSummaryReportDto(1_000L, 2_000L, 200.0, 75.0, 125.0, 3L)
        );

        mockMvc.perform(get("/v1/reports/cashflow-summary")
                .param("start_at", "1000")
                .param("end_at", "2000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.start_at").value(1000))
            .andExpect(jsonPath("$.data.end_at").value(2000))
            .andExpect(jsonPath("$.data.total_income_amount").value(200.0))
            .andExpect(jsonPath("$.data.total_expense_amount").value(75.0))
            .andExpect(jsonPath("$.data.net_cash_flow").value(125.0))
            .andExpect(jsonPath("$.data.total_record_count").value(3));

        verify(reportService).cashflowSummary(1_000L, 2_000L);
    }
}

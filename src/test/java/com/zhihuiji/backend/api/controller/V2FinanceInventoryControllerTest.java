package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2AccountController;
import com.zhihuiji.backend.api.controller.v2.V2AccountTransferController;
import com.zhihuiji.backend.api.controller.v2.V2BillFundLinkController;
import com.zhihuiji.backend.api.controller.v2.V2CashChangeRecordController;
import com.zhihuiji.backend.api.controller.v2.V2InventoryController;
import com.zhihuiji.backend.api.dto.v2.finance.V2FinanceDtos;
import com.zhihuiji.backend.api.dto.v2.inventory.V2InventoryDtos;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2AccountService;
import com.zhihuiji.backend.application.service.v2.V2AccountTransferService;
import com.zhihuiji.backend.application.service.v2.V2BillFundLinkService;
import com.zhihuiji.backend.application.service.v2.V2CashChangeRecordService;
import com.zhihuiji.backend.application.service.v2.V2InventoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2AccountController.class,
    V2AccountTransferController.class,
    V2BillFundLinkController.class,
    V2CashChangeRecordController.class,
    V2InventoryController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2FinanceInventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2AccountService v2AccountService;
    @MockBean
    private V2AccountTransferService v2AccountTransferService;
    @MockBean
    private V2BillFundLinkService v2BillFundLinkService;
    @MockBean
    private V2CashChangeRecordService v2CashChangeRecordService;
    @MockBean
    private V2InventoryService v2InventoryService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void listAccountsReturnsSnakeCaseFields() throws Exception {
        when(v2AccountService.list()).thenReturn(List.of(
            new V2FinanceDtos.AccountResponse(1L, "CASH", "现金", 1, 12.5, true, 1, 0, "默认", 1L, 2L)
        ));

        mockMvc.perform(get("/v2/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].is_default").value(true))
            .andExpect(jsonPath("$.data[0].sort_order").value(0))
            .andExpect(jsonPath("$.data[0].created_at").value(1));
    }

    @Test
    void createAccountRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/v2/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "",
                      "name": "",
                      "type": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void inventorySnapshotEndpointsReturnSnakeCaseFields() throws Exception {
        when(v2InventoryService.listSnapshots(20240601L, null, null)).thenReturn(List.of(
            new V2InventoryDtos.SnapshotResponse(9L, 6L, "P001", "矿泉水", null, 10.0, 1.5, 15.0, 20240601L, 1L)
        ));

        mockMvc.perform(get("/v2/inventory/snapshots").param("snapshotDate", "20240601"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].product_id").value(6))
            .andExpect(jsonPath("$.data[0].snapshot_date").value(20240601))
            .andExpect(jsonPath("$.data[0].total_value").value(15.0));
    }

    @Test
    void inventoryLedgerBySourceEndpointReturnsSnakeCaseFields() throws Exception {
        when(v2InventoryService.listLedgerBySource("sale_return", 7L)).thenReturn(List.of(
            new V2InventoryDtos.LedgerEntryResponse(3L, 6L, "P001", "矿泉水", null, 8.0, 2.0, 10.0, 1.5, "sale_return", 7L, "SR-001", "退货入库", 1L)
        ));

        mockMvc.perform(get("/v2/inventory/ledger/by-source")
                .param("source_type", "sale_return")
                .param("source_id", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].source_type").value("sale_return"))
            .andExpect(jsonPath("$.data[0].source_id").value(7))
            .andExpect(jsonPath("$.data[0].source_no").value("SR-001"));
    }

    @Test
    void createBillFundLinkDelegatesToService() throws Exception {
        when(v2BillFundLinkService.create(any())).thenReturn(
            new V2FinanceDtos.BillFundLinkResponse(7L, "sale_order", 3L, 5L, "现金", 30.0, 1, "收款", 1L, 2L)
        );

        mockMvc.perform(post("/v2/bill-fund-links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bill_type": "sale_order",
                      "bill_id": 3,
                      "account_id": 5,
                      "amount": 30.0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.account_id").value(5))
            .andExpect(jsonPath("$.data.account_name").value("现金"))
            .andExpect(jsonPath("$.data.link_type").value(1));
    }

    @Test
    void createCashChangeRecordReturnsComputedFields() throws Exception {
        when(v2CashChangeRecordService.create(any())).thenReturn(
            new V2FinanceDtos.CashChangeRecordResponse(8L, "sale_order", 3L, 100.0, 120.0, 20.0, 5L, "现金", 1, "找零", 1L, 2L)
        );

        mockMvc.perform(post("/v2/cash-change-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "order_type": "sale_order",
                      "order_id": 3,
                      "receivable": 100.0,
                      "received": 120.0,
                      "account_id": 5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.change_amount").value(20.0))
            .andExpect(jsonPath("$.data.account_name").value("现金"))
            .andExpect(jsonPath("$.data.order_type").value("sale_order"));
    }
}

package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2PurchaseReceiptController;
import com.zhihuiji.backend.api.controller.v2.V2SaleOrderController;
import com.zhihuiji.backend.api.controller.v2.V2SalesReturnController;
import com.zhihuiji.backend.api.dto.v2.purchase.V2PurchaseReceiptDtos;
import com.zhihuiji.backend.api.dto.v2.sales.V2SaleOrderDtos;
import com.zhihuiji.backend.api.dto.v2.sales.V2SalesReturnDtos;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2PurchaseReceiptService;
import com.zhihuiji.backend.application.service.v2.V2SaleReceiptPdfService;
import com.zhihuiji.backend.application.service.v2.V2SaleOrderService;
import com.zhihuiji.backend.application.service.v2.V2SalesReturnService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2SaleOrderController.class,
    V2SalesReturnController.class,
    V2PurchaseReceiptController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2BillDomainControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2SaleOrderService v2SaleOrderService;
    @MockBean
    private V2SaleReceiptPdfService v2SaleReceiptPdfService;
    @MockBean
    private V2SalesReturnService v2SalesReturnService;
    @MockBean
    private V2PurchaseReceiptService v2PurchaseReceiptService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void saleReceiptPdfReturnsPdfResponse() throws Exception {
        byte[] pdf = "%PDF-1.7\nreceipt".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(v2SaleReceiptPdfService.export(9L)).thenReturn(pdf);

        mockMvc.perform(get("/v2/sale-orders/9/receipt.pdf"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store, private"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(pdf));

        verify(v2SaleReceiptPdfService).export(9L);
    }

    @Test
    void confirmSaleOrderReturnsSnakeCaseFields() throws Exception {
        when(v2SaleOrderService.confirm(any(), any())).thenReturn(
            new V2SaleOrderDtos.SaleOrderResponse(
                1L,
                "SO-001",
                2L,
                "客户A",
                List.of(),
                100.0,
                5.0,
                95.0,
                0.0,
                "已确认",
                3,
                1000L,
                2000L
            )
        );

        mockMvc.perform(put("/v2/sale-orders/1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notes": "已确认"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order_no").value("SO-001"))
            .andExpect(jsonPath("$.data.discount_amount").value(5.0))
            .andExpect(jsonPath("$.data.updated_at").value(2000));
    }

    @Test
    void saleOrderListPassesPaginationToServiceInsteadOfControllerSlice() throws Exception {
        when(v2SaleOrderService.list(
            "SO",
            3,
            10.0,
            200.0,
            1000L,
            2000L,
            "苹果",
            1,
            2,
            20
        )).thenReturn(List.of(
            new V2SaleOrderDtos.SaleOrderResponse(
                9L,
                "SO-009",
                2L,
                "客户A",
                List.of(),
                100.0,
                0.0,
                100.0,
                50.0,
                "分页",
                3,
                1000L,
                2000L
            )
        ));

        mockMvc.perform(get("/v2/sale-orders")
                .param("keyword", "SO")
                .param("status", "3")
                .param("min_total_amount", "10")
                .param("max_total_amount", "200")
                .param("created_after", "1000")
                .param("created_before", "2000")
                .param("product_keyword", "苹果")
                .param("payment_status", "1")
                .param("page", "2")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].order_no").value("SO-009"));

        verify(v2SaleOrderService).list(
            "SO",
            3,
            10.0,
            200.0,
            1000L,
            2000L,
            "苹果",
            1,
            2,
            20
        );
    }

    @Test
    void salesReturnCreateDelegatesToService() throws Exception {
        when(v2SalesReturnService.create(any())).thenReturn(
            new V2SalesReturnDtos.SalesReturnResponse(
                7L,
                "SR-001",
                3L,
                2L,
                "客户A",
                List.of(),
                30.0,
                0.0,
                0,
                "退货草稿",
                1000L,
                1000L
            )
        );

        mockMvc.perform(post("/v2/sales-returns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "original_order_id": 3,
                      "customer_id": 2,
                      "customer_name": "客户A",
                      "items": [
                        {
                          "product_id": 11,
                          "quantity": 2,
                          "unit_price": 15
                        }
                      ],
                      "notes": "退货草稿"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.return_no").value("SR-001"))
            .andExpect(jsonPath("$.data.original_order_id").value(3))
            .andExpect(jsonPath("$.data.total_amount").value(30.0));
    }

    @Test
    void purchaseReceiptListReturnsSnakeCaseFields() throws Exception {
        when(v2PurchaseReceiptService.list("PO", null)).thenReturn(List.of(
            new V2PurchaseReceiptDtos.PurchaseReceiptResponse(
                5L,
                "PR-001",
                9L,
                4L,
                "供应商A",
                List.of(),
                56.0,
                1,
                "收货完成",
                1000L,
                2000L
            )
        ));

        mockMvc.perform(get("/v2/purchase-receipts").param("keyword", "PO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].receipt_no").value("PR-001"))
            .andExpect(jsonPath("$.data[0].purchase_order_id").value(9))
            .andExpect(jsonPath("$.data[0].supplier_name").value("供应商A"))
            .andExpect(jsonPath("$.data[0].updated_at").value(2000));
    }
}

package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2ProductCategoryController;
import com.zhihuiji.backend.api.controller.v2.V2ProductController;
import com.zhihuiji.backend.api.controller.v2.V2ProductPriceLevelController;
import com.zhihuiji.backend.api.controller.v2.V2ProductSupplierRelationController;
import com.zhihuiji.backend.api.controller.v2.V2ProductUnitController;
import com.zhihuiji.backend.api.dto.v2.product.V2ProductDtos;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2ProductCategoryService;
import com.zhihuiji.backend.application.service.v2.V2ProductPriceLevelService;
import com.zhihuiji.backend.application.service.v2.V2ProductService;
import com.zhihuiji.backend.application.service.v2.V2ProductSupplierRelationService;
import com.zhihuiji.backend.application.service.v2.V2ProductUnitService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2ProductController.class,
    V2ProductCategoryController.class,
    V2ProductUnitController.class,
    V2ProductPriceLevelController.class,
    V2ProductSupplierRelationController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2ProductService v2ProductService;
    @MockBean
    private V2ProductCategoryService v2ProductCategoryService;
    @MockBean
    private V2ProductUnitService v2ProductUnitService;
    @MockBean
    private V2ProductPriceLevelService v2ProductPriceLevelService;
    @MockBean
    private V2ProductSupplierRelationService v2ProductSupplierRelationService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void listReturnsSnakeCaseFields() throws Exception {
        when(v2ProductService.list(null, null, null, null)).thenReturn(List.of(
            productResponse()
        ));

        mockMvc.perform(get("/v2/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].category_id").value(10))
            .andExpect(jsonPath("$.data[0].category_name").value("饮料"))
            .andExpect(jsonPath("$.data[0].unit_name").value("瓶"))
            .andExpect(jsonPath("$.data[0].price_levels[0].level_id").value(31))
            .andExpect(jsonPath("$.data[0].price_levels[0].price").value(3.8))
            .andExpect(jsonPath("$.data[0].default_supplier.supplier_name").value("供应商A"))
            .andExpect(jsonPath("$.data[0].supplier_relations[0].is_default").value(true))
            .andExpect(jsonPath("$.data[0].supplier_relations[0].purchase_priority").value(0));
    }

    @Test
    void createRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/v2/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "",
                      "name": "",
                      "sale_price": 2.5
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createDelegatesToService() throws Exception {
        when(v2ProductService.create(any())).thenReturn(productResponse());

        mockMvc.perform(post("/v2/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "P001",
                      "name": "矿泉水",
                      "category_id": 10,
                      "unit_id": 20,
                      "sale_price": 2.5,
                      "purchase_price": 1.5,
                      "price_levels": [
                        { "level_id": 31, "price": 3.8 }
                      ],
                      "supplier_relations": [
                        {
                          "product_id": 1,
                          "supplier_id": 41,
                          "is_default": true,
                          "purchase_priority": 0,
                          "last_purchase_price": 1.2,
                          "notes": "长期合作"
                        }
                      ],
                      "stock": 30,
                      "safe_stock": 5,
                      "status": 1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("P001"))
            .andExpect(jsonPath("$.data.category_name").value("饮料"))
            .andExpect(jsonPath("$.data.price_levels[0].name").value("批发价"))
            .andExpect(jsonPath("$.data.default_supplier.supplier_id").value(41));
    }

    @Test
    void priceLevelEndpointsReturnSnakeCaseFields() throws Exception {
        when(v2ProductPriceLevelService.list()).thenReturn(List.of(
            new V2ProductDtos.PriceLevelResponse(31L, "WHOLESALE", "批发价", 1, 0, 1L, 2L)
        ));

        mockMvc.perform(get("/v2/product-price-levels"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(31))
            .andExpect(jsonPath("$.data[0].code").value("WHOLESALE"))
            .andExpect(jsonPath("$.data[0].sort_order").value(0));
    }

    @Test
    void supplierRelationEndpointsReturnSnakeCaseFields() throws Exception {
        when(v2ProductSupplierRelationService.list(1L)).thenReturn(List.of(
            new V2ProductDtos.ProductSupplierRelationResponse(
                51L,
                1L,
                41L,
                "供应商A",
                "13600000000",
                true,
                0,
                1.2,
                "长期合作",
                1L,
                2L
            )
        ));

        mockMvc.perform(get("/v2/product-supplier-relations").param("product_id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].product_id").value(1))
            .andExpect(jsonPath("$.data[0].supplier_name").value("供应商A"))
            .andExpect(jsonPath("$.data[0].is_default").value(true))
            .andExpect(jsonPath("$.data[0].last_purchase_price").value(1.2));
    }

    private static V2ProductDtos.ProductResponse productResponse() {
        V2ProductDtos.ProductSupplierRelationResponse defaultSupplier = new V2ProductDtos.ProductSupplierRelationResponse(
            51L,
            1L,
            41L,
            "供应商A",
            "13600000000",
            true,
            0,
            1.2,
            "长期合作",
            1L,
            2L
        );
        return new V2ProductDtos.ProductResponse(
            1L,
            "P001",
            "矿泉水",
            10L,
            "饮料",
            20L,
            "瓶",
            2.5,
            1.5,
            List.of(new V2ProductDtos.ProductPriceValueResponse(31L, "WHOLESALE", "批发价", 3.8, 1, 0)),
            defaultSupplier,
            List.of(defaultSupplier),
            30.0,
            5.0,
            1,
            1L,
            2L
        );
    }
}

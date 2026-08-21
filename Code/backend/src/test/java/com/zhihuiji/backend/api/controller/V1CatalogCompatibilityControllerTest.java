package com.zhihuiji.backend.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.application.service.CustomerService;
import com.zhihuiji.backend.application.service.ProductService;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.SupplierService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = { ProductController.class, CustomerController.class, SupplierController.class })
@AutoConfigureMockMvc(addFilters = false)
class V1CatalogCompatibilityControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;
    @MockBean
    private CustomerService customerService;
    @MockBean
    private SupplierService supplierService;
    @MockBean
    private SessionAccessService sessionAccessService;

    @Test
    void v1ProductDoesNotExposeExpansionFields() throws Exception {
        ProductEntity entity = new ProductEntity();
        entity.setId(1L);
        entity.setOwnerUserId(1L);
        entity.setCode("P001");
        entity.setName("矿泉水");
        entity.setCategory("饮料");
        entity.setCategoryId(10L);
        entity.setUnit("瓶");
        entity.setUnitId(20L);
        entity.setSalePrice(2.5);
        entity.setPurchasePrice(1.5);
        entity.setPriceLevelValuesJson("[{\"levelId\":31,\"price\":3.8}]");
        entity.setStock(30.0);
        entity.setSafeStock(5.0);
        entity.setStatus(1);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        when(productService.get(1L)).thenReturn(entity);

        mockMvc.perform(get("/v1/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.categoryId").doesNotExist())
            .andExpect(jsonPath("$.data.unitId").doesNotExist())
            .andExpect(jsonPath("$.data.priceLevelValuesJson").doesNotExist())
            .andExpect(jsonPath("$.data.price_levels").doesNotExist())
            .andExpect(jsonPath("$.data.supplier_relations").doesNotExist());
    }

    @Test
    void v1CustomerAndSupplierDoNotExposeExpansionFields() throws Exception {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(1L);
        customer.setOwnerUserId(1L);
        customer.setName("客户A");
        customer.setPhone("13800000000");
        customer.setLevel(1);
        customer.setGroupId(7L);
        customer.setContactName("张三");
        customer.setContactPhone("13900000000");
        customer.setAddress("重庆");
        customer.setNotes("备注");
        customer.setBalance(10.0);
        customer.setStatus(1);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        customer.setCreatedAt(1L);
        customer.setUpdatedAt(2L);
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(2L);
        supplier.setOwnerUserId(1L);
        supplier.setName("供应商A");
        supplier.setPhone("13600000000");
        supplier.setGroupId(8L);
        supplier.setContactName("李四");
        supplier.setContactPhone("13700000000");
        supplier.setAddress("成都");
        supplier.setNotes("备注");
        supplier.setBalance(20.0);
        supplier.setStatus(1);
        supplier.setSyncStatus(0);
        supplier.setSyncVersion(1L);
        supplier.setCreatedAt(1L);
        supplier.setUpdatedAt(2L);
        when(customerService.get(1L)).thenReturn(customer);
        when(supplierService.get(2L)).thenReturn(supplier);

        mockMvc.perform(get("/v1/customers/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId").doesNotExist())
            .andExpect(jsonPath("$.data.contactName").doesNotExist())
            .andExpect(jsonPath("$.data.contactPhone").doesNotExist());

        mockMvc.perform(get("/v1/suppliers/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId").doesNotExist())
            .andExpect(jsonPath("$.data.contactName").doesNotExist())
            .andExpect(jsonPath("$.data.contactPhone").doesNotExist());
    }
}

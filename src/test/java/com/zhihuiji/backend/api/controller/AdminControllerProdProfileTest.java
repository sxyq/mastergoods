package com.zhihuiji.backend.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:prod-profile-admin-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({ "test", "prod" })
class AdminControllerProdProfileTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void localAdminDemoRoutesAreNotAvailableInProdProfile() throws Exception {
        mockMvc.perform(post("/v1/admin/demo/seed").param("reset", "true"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Resource not found"));

        mockMvc.perform(post("/v1/admin/agent/smoke"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void localAdminControllerBeanIsNotRegisteredInProdProfile() {
        org.junit.jupiter.api.Assertions.assertThrows(
            NoSuchBeanDefinitionException.class,
            () -> applicationContext.getBean(AdminController.class)
        );
    }
}

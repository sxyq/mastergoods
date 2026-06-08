package com.zhihuiji.backend.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhihuiji.backend.api.controller.AdminController;
import com.zhihuiji.backend.application.service.AdminService;
import com.zhihuiji.backend.application.service.DemoDataService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class LocalProfileGuardTest {

    @Test
    void localAdminAndDemoBeansAreNotRegisteredOutsideLocalProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(
                AdminController.class,
                AdminService.class,
                DemoDataService.class,
                LocalDemoDataInitializer.class
            );

            context.refresh();

            assertEquals(0, context.getBeanNamesForType(AdminController.class).length);
            assertEquals(0, context.getBeanNamesForType(AdminService.class).length);
            assertEquals(0, context.getBeanNamesForType(DemoDataService.class).length);
            assertEquals(0, context.getBeanNamesForType(LocalDemoDataInitializer.class).length);
        }
    }
}

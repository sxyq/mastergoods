package com.zhihuiji.backend.infrastructure.config;

import com.zhihuiji.backend.application.service.DemoDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "zhihuiji.demo", name = "auto-seed", havingValue = "true")
public class LocalDemoDataInitializer implements ApplicationRunner {
    private final DemoDataService demoDataService;

    public LocalDemoDataInitializer(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        demoDataService.seed(false);
    }
}

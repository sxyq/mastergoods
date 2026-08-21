package com.zhihuiji.backend.infrastructure.config;

import com.zhihuiji.backend.infrastructure.security.StorePermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final StorePermissionInterceptor storePermissionInterceptor;

    public WebMvcConfig(StorePermissionInterceptor storePermissionInterceptor) {
        this.storePermissionInterceptor = storePermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storePermissionInterceptor);
    }
}

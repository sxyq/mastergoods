package com.zhihuiji.backend.infrastructure.security;

import com.zhihuiji.backend.application.service.CurrentOwnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StorePermissionInterceptor implements HandlerInterceptor {
    private final ObjectProvider<CurrentOwnerService> currentOwnerServiceProvider;

    public StorePermissionInterceptor(ObjectProvider<CurrentOwnerService> currentOwnerServiceProvider) {
        this.currentOwnerServiceProvider = currentOwnerServiceProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireStorePermission annotation = resolveAnnotation(handlerMethod);
        if (annotation == null || (annotation.value().length == 0 && annotation.anyOf().length == 0)) {
            return true;
        }
        if (!StringUtils.hasText(request.getHeader("Authorization"))) {
            return true;
        }
        CurrentOwnerService currentOwnerService = currentOwnerServiceProvider.getIfAvailable();
        if (currentOwnerService == null) {
            return true;
        }
        if (annotation.value().length > 0) {
            currentOwnerService.requirePermissions(annotation.value());
        }
        if (annotation.anyOf().length > 0) {
            currentOwnerService.requireAnyPermission(annotation.anyOf());
        }
        return true;
    }

    private RequireStorePermission resolveAnnotation(HandlerMethod handlerMethod) {
        RequireStorePermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(),
            RequireStorePermission.class
        );
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireStorePermission.class);
    }
}

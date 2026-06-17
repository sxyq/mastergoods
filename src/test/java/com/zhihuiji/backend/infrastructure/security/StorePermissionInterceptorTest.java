package com.zhihuiji.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.application.service.CurrentOwnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class StorePermissionInterceptorTest {
    @Test
    void preHandleEnforcesAnyOfOnlyAnnotation() throws Exception {
        CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CurrentOwnerService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(currentOwnerService);
        StorePermissionInterceptor interceptor = new StorePermissionInterceptor(provider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(
            new AnyOfOnlyController(),
            AnyOfOnlyController.class.getDeclaredMethod("upload")
        );

        assertTrue(interceptor.preHandle(request, response, handler));

        verify(currentOwnerService, never()).requirePermissions();
        verify(currentOwnerService).requireAnyPermission("sales:write", "finance:write");
    }

    static class AnyOfOnlyController {
        @RequireStorePermission(anyOf = {"sales:write", "finance:write"})
        public void upload() {}
    }
}

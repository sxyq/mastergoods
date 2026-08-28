package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.application.service.admin.AdminScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {
    @Mock
    private AdminScopeService adminScopeService;

    private AdminAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AdminAuthorizationService(adminScopeService);
    }

    @Test
    void authorizeChecksActionBeforeResolvingScope() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(java.util.Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.authorize(principal, AdminPermission.USER_MANAGE, 101L, null)
        );
        org.mockito.Mockito.verifyNoInteractions(adminScopeService);
    }

    @Test
    void authorizeReturnsOnlyTheScopeServiceResult() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );
        AdminDataScope resolved = new AdminDataScope(java.util.Set.of(101L), java.util.Set.of(501L));
        when(adminScopeService.resolve(principal, 101L, 501L)).thenReturn(resolved);

        assertEquals(
            resolved,
            authorizationService.authorize(principal, AdminPermission.STORE_READ, 101L, 501L)
        );
        verify(adminScopeService).resolve(principal, 101L, 501L);
    }

    @Test
    void nullPrincipalIsDenied() {
        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requirePermission(null, AdminPermission.AUDIT_READ)
        );
    }
}

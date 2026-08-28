package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.zhihuiji.backend.application.service.admin.AdminScopeService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationBoundaryTest {
    @Mock
    private AdminScopeService adminScopeService;

    private AdminAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AdminAuthorizationService(adminScopeService);
    }

    @Test
    void nullPermissionIsDeniedBeforeScopeResolution() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.authorize(principal, null, 101L, 501L)
        );
        verifyNoInteractions(adminScopeService);
    }

    @Test
    void nullPrincipalIsDeniedBeforeScopeResolution() {
        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.authorize(null, AdminPermission.DASHBOARD_READ, null, null)
        );
        verifyNoInteractions(adminScopeService);
    }

    @Test
    void observerWriteAttemptIsDeniedBeforeScopeResolution() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.authorize(principal, AdminPermission.STORE_MANAGE, 101L, 501L)
        );
        verifyNoInteractions(adminScopeService);
    }
}

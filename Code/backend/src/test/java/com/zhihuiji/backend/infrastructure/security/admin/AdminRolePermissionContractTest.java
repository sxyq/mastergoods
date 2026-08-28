package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminRolePermissionContractTest {
    @Test
    void superAdminOwnsTheDocumentedPermissionSet() {
        assertEquals(Set.of(AdminPermission.values()), AdminPrincipal.AdminRole.SUPER_ADMIN.permissions());

        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.empty()
        );

        for (AdminPermission permission : AdminPermission.values()) {
            assertTrue(principal.can(permission), permission.code());
        }
    }

    @Test
    void auditObserverIsReadOnlyAndCannotReadContentByDefault() {
        Set<AdminPermission> expected = Set.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.USER_READ,
            AdminPermission.STORE_READ,
            AdminPermission.AGENT_RUN_READ,
            AdminPermission.AGENT_CONFIG_READ,
            AdminPermission.AUDIT_READ,
            AdminPermission.SYSTEM_READ
        );

        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.empty()
        );

        assertEquals(expected, principal.permissions());
        assertFalse(principal.can(AdminPermission.AGENT_CONTENT_READ));
        assertFalse(principal.can(AdminPermission.USER_MANAGE));
        assertFalse(principal.can(AdminPermission.STORE_MANAGE));
        assertFalse(principal.can(AdminPermission.AGENT_CONFIG_MANAGE));
        assertFalse(principal.can(AdminPermission.EXPORT));
    }

    @Test
    void rolePermissionCollectionsAreImmutable() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> AdminPrincipal.AdminRole.SUPER_ADMIN.permissions().clear()
        );
    }

    @Test
    void principalCopiesPermissionSetAndRejectsUnknownPermissionForRole() {
        Set<AdminPermission> supplied = Set.of(AdminPermission.DASHBOARD_READ);
        AdminPrincipal principal = new AdminPrincipal(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            supplied,
            AdminDataScope.empty()
        );

        assertEquals(supplied, principal.permissions());
        assertThrows(
            UnsupportedOperationException.class,
            () -> principal.permissions().add(AdminPermission.USER_READ)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AdminPrincipal(
                9L,
                AdminPrincipal.AdminRole.AUDIT_OBSERVER,
                Set.of(AdminPermission.USER_MANAGE),
                AdminDataScope.empty()
            )
        );
    }
}

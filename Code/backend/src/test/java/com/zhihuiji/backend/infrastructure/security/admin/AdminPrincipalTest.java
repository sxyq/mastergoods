package com.zhihuiji.backend.infrastructure.security.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminPrincipalTest {
    @Test
    void roleFactoryUsesTheServerDefinedPermissionSet() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.SUPER_ADMIN,
            AdminDataScope.allOwners(false, AdminDataScope.ContentMode.AUTHORIZED)
        );

        assertEquals(AdminPrincipal.AdminRole.SUPER_ADMIN.permissions(), principal.permissions());
        assertTrue(principal.can(AdminPermission.AGENT_CONTENT_READ));
        assertTrue(principal.scope().allOwners());
        assertEquals(AdminDataScope.ContentMode.AUTHORIZED, principal.scope().contentMode());
    }

    @Test
    void observerCannotBeGrantedWritePermission() {
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

    @Test
    void principalRejectsInvalidIdentity() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AdminPrincipal.forRole(0L, AdminPrincipal.AdminRole.SUPER_ADMIN, AdminDataScope.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AdminPrincipal(9L, null, Set.of(), AdminDataScope.empty())
        );
    }

    @Test
    void observerHasReadOnlyPermissionsAndNoContentAccessByDefault() {
        AdminPrincipal principal = AdminPrincipal.forRole(
            9L,
            AdminPrincipal.AdminRole.AUDIT_OBSERVER,
            AdminDataScope.owners(Set.of(101L), false, AdminDataScope.ContentMode.REDACTED)
        );

        assertTrue(principal.can(AdminPermission.DASHBOARD_READ));
        assertTrue(principal.can(AdminPermission.AGENT_RUN_READ));
        assertFalse(principal.can(AdminPermission.AGENT_CONTENT_READ));
        assertFalse(principal.can(AdminPermission.USER_MANAGE));
        assertEquals(Set.of(101L), principal.scope().ownerUserIds());
    }
}

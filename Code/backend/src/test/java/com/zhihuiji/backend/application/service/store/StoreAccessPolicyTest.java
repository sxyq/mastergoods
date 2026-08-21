package com.zhihuiji.backend.application.service.store;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StoreAccessPolicyTest {
    @Test
    void hasPermissionChecksRolePermissionSet() {
        assertTrue(StoreAccessPolicy.hasPermission("OWNER", 1, "database:manage"));
        assertTrue(StoreAccessPolicy.hasPermission(" SALES ", 1, "sales:write"));
        assertFalse(StoreAccessPolicy.hasPermission("SALES", 1, "users:manage"));
    }

    @Test
    void hasPermissionKeepsInactiveRoleSemantics() {
        assertFalse(StoreAccessPolicy.hasPermission("NOT_A_ROLE", 0, "dashboard:view"));
        assertTrue(StoreAccessPolicy.hasPermission("NOT_A_ROLE", 0, " "));
    }
}

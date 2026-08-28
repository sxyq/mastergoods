package com.zhihuiji.backend.infrastructure.security.admin;

import java.util.Set;

/**
 * Server-derived administrator identity used by admin application services.
 * Client supplied role and scope values must never construct this object.
 */
public record AdminPrincipal(
    Long userId,
    AdminRole role,
    Set<AdminPermission> permissions,
    AdminDataScope scope
) {
    public AdminPrincipal {
        permissions = Set.copyOf(permissions == null ? Set.of() : permissions);
        scope = scope == null ? AdminDataScope.empty() : scope;
    }

    public boolean can(AdminPermission permission) {
        return permissions.contains(permission);
    }

    public enum AdminRole {
        SUPER_ADMIN,
        AUDIT_OBSERVER
    }
}

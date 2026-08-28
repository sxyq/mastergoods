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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("administrator user id is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("administrator role is required");
        }
        permissions = Set.copyOf(permissions == null ? Set.of() : permissions);
        scope = scope == null ? AdminDataScope.empty() : scope;
        if (!role.permissions().containsAll(permissions)) {
            throw new IllegalArgumentException("administrator permission is not allowed for role");
        }
    }

    /** Creates the role's server-defined permission set. */
    public static AdminPrincipal forRole(Long userId, AdminRole role, AdminDataScope scope) {
        if (role == null) {
            throw new IllegalArgumentException("administrator role is required");
        }
        return new AdminPrincipal(userId, role, role.permissions(), scope);
    }

    public boolean can(AdminPermission permission) {
        return permission != null && permissions.contains(permission);
    }

    public enum AdminRole {
        SUPER_ADMIN(Set.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.USER_READ,
            AdminPermission.USER_MANAGE,
            AdminPermission.STORE_READ,
            AdminPermission.STORE_MANAGE,
            AdminPermission.PERMISSION_MANAGE,
            AdminPermission.AGENT_RUN_READ,
            AdminPermission.AGENT_CONTENT_READ,
            AdminPermission.AGENT_CONFIG_READ,
            AdminPermission.AGENT_CONFIG_MANAGE,
            AdminPermission.AUDIT_READ,
            AdminPermission.SYSTEM_READ,
            AdminPermission.SYSTEM_RETENTION_MANAGE,
            AdminPermission.EXPORT
        )),
        AUDIT_OBSERVER(Set.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.USER_READ,
            AdminPermission.STORE_READ,
            AdminPermission.AGENT_RUN_READ,
            AdminPermission.AGENT_CONFIG_READ,
            AdminPermission.AUDIT_READ,
            AdminPermission.SYSTEM_READ
        ));

        private final Set<AdminPermission> permissions;

        AdminRole(Set<AdminPermission> permissions) {
            this.permissions = Set.copyOf(permissions);
        }

        public Set<AdminPermission> permissions() {
            return permissions;
        }
    }
}

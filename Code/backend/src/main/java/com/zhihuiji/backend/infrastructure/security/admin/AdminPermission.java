package com.zhihuiji.backend.infrastructure.security.admin;

/**
 * Permission vocabulary for the administrator surface.
 *
 * <p>This is a contract-only skeleton. Resolution from the existing session
 * model is intentionally deferred until the administrator identity source is
 * confirmed.</p>
 */
public enum AdminPermission {
    SESSION_VIEW("admin:session:view"),
    OVERVIEW_VIEW("admin:overview:view"),
    USERS_VIEW("admin:users:view"),
    USERS_MANAGE("admin:users:manage"),
    STORES_VIEW("admin:stores:view"),
    STORES_MANAGE("admin:stores:manage"),
    AGENT_VIEW("admin:agent:view"),
    AGENT_CONTENT_VIEW("admin:agent:content:view"),
    CONFIG_MANAGE("admin:config:manage"),
    AUDIT_VIEW("admin:audit:view"),
    SYSTEM_VIEW("admin:system:view"),
    EXPORT_CREATE("admin:export:create"),
    RETENTION_MANAGE("admin:retention:manage");

    private final String code;

    AdminPermission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

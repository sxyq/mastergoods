package com.zhihuiji.backend.infrastructure.security.admin;

/** Permission vocabulary defined by the administrator authorization design. */
public enum AdminPermission {
    DASHBOARD_READ("admin.dashboard.read"),
    USER_READ("admin.user.read"),
    USER_MANAGE("admin.user.manage"),
    STORE_READ("admin.store.read"),
    STORE_MANAGE("admin.store.manage"),
    PERMISSION_MANAGE("admin.permission.manage"),
    AGENT_RUN_READ("admin.agent.run.read"),
    AGENT_CONTENT_READ("admin.agent.content.read"),
    AGENT_CONFIG_READ("admin.agent.config.read"),
    AGENT_CONFIG_MANAGE("admin.agent.config.manage"),
    AUDIT_READ("admin.audit.read"),
    SYSTEM_READ("admin.system.read"),
    SYSTEM_RETENTION_MANAGE("admin.system.retention.manage"),
    EXPORT("admin.export");

    private final String code;

    AdminPermission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

package com.zhihuiji.backend.application.service.store;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class StoreAccessPolicy {
    private StoreAccessPolicy() {}

    public static StoreRole requireRole(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("店员角色不能为空");
        }
        return Arrays.stream(StoreRole.values())
            .filter(role -> role.name().equals(value.trim().toUpperCase(Locale.ROOT)))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("店员角色不合法"));
    }

    public static List<String> permissionCodes(String roleCode, Integer status) {
        if (status == null || status != 1) {
            return List.of();
        }
        return requireRole(roleCode).permissions().stream()
            .map(StorePermission::code)
            .toList();
    }

    public static boolean hasPermission(String roleCode, Integer status, String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return true;
        }
        return permissionCodes(roleCode, status).contains(permissionCode.trim());
    }

    public enum StoreRole {
        OWNER("店长（总）", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.SALES_WRITE,
            StorePermission.PURCHASE_VIEW,
            StorePermission.PURCHASE_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.ARCHIVES_WRITE,
            StorePermission.INVENTORY_VIEW,
            StorePermission.INVENTORY_WRITE,
            StorePermission.FINANCE_VIEW,
            StorePermission.FINANCE_WRITE,
            StorePermission.REPORTS_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.AGENT_WRITE,
            StorePermission.DATABASE_MANAGE,
            StorePermission.SETTINGS_MANAGE,
            StorePermission.USERS_MANAGE
        )),
        MANAGER("店长助理", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.SALES_WRITE,
            StorePermission.PURCHASE_VIEW,
            StorePermission.PURCHASE_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.ARCHIVES_WRITE,
            StorePermission.INVENTORY_VIEW,
            StorePermission.INVENTORY_WRITE,
            StorePermission.FINANCE_VIEW,
            StorePermission.REPORTS_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.AGENT_WRITE,
            StorePermission.USERS_MANAGE
        )),
        SALES("销售员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.SALES_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.AGENT_VIEW
        )),
        PURCHASING("采购员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.PURCHASE_VIEW,
            StorePermission.PURCHASE_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.AGENT_VIEW
        )),
        WAREHOUSE("仓库员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.INVENTORY_VIEW,
            StorePermission.INVENTORY_WRITE,
            StorePermission.AGENT_VIEW
        )),
        FINANCE("财务员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.FINANCE_WRITE,
            StorePermission.REPORTS_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.PURCHASE_VIEW,
            StorePermission.AGENT_VIEW
        )),
        ASSISTANT("AI/只读助理", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.REPORTS_VIEW,
            StorePermission.AGENT_VIEW
        ));

        private final String defaultTitle;
        private final Set<StorePermission> permissions;

        StoreRole(String defaultTitle, Set<StorePermission> permissions) {
            this.defaultTitle = defaultTitle;
            this.permissions = permissions;
        }

        public String defaultTitle() {
            return defaultTitle;
        }

        public Set<StorePermission> permissions() {
            return permissions;
        }
    }

    public enum StorePermission {
        DASHBOARD_VIEW("dashboard:view"),
        SALES_VIEW("sales:view"),
        SALES_WRITE("sales:write"),
        PURCHASE_VIEW("purchase:view"),
        PURCHASE_WRITE("purchase:write"),
        ARCHIVES_VIEW("archives:view"),
        ARCHIVES_WRITE("archives:write"),
        INVENTORY_VIEW("inventory:view"),
        INVENTORY_WRITE("inventory:write"),
        FINANCE_VIEW("finance:view"),
        FINANCE_WRITE("finance:write"),
        REPORTS_VIEW("reports:view"),
        AGENT_VIEW("agent:view"),
        AGENT_WRITE("agent:write"),
        DATABASE_MANAGE("database:manage"),
        SETTINGS_MANAGE("settings:manage"),
        USERS_MANAGE("users:manage");

        private final String code;

        StorePermission(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}

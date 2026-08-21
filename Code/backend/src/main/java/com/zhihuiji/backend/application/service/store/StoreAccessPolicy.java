package com.zhihuiji.backend.application.service.store;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class StoreAccessPolicy {
    private static final Map<String, StoreRole> ROLE_BY_NAME;
    private static final Map<String, StorePermission> PERMISSION_BY_CODE;
    private static final Map<StoreRole, List<String>> PERMISSION_CODES_BY_ROLE;

    static {
        Map<String, StoreRole> rolesByName = new HashMap<>(StoreRole.values().length);
        for (StoreRole role : StoreRole.values()) {
            rolesByName.put(role.name(), role);
        }
        ROLE_BY_NAME = Map.copyOf(rolesByName);

        Map<String, StorePermission> permissionsByCode = new HashMap<>(StorePermission.values().length);
        for (StorePermission permission : StorePermission.values()) {
            permissionsByCode.put(permission.code(), permission);
        }
        PERMISSION_BY_CODE = Map.copyOf(permissionsByCode);

        Map<StoreRole, List<String>> codesByRole = new EnumMap<>(StoreRole.class);
        for (StoreRole role : StoreRole.values()) {
            List<String> codes = new ArrayList<>(role.permissions().size());
            for (StorePermission permission : role.permissions()) {
                codes.add(permission.code());
            }
            codesByRole.put(role, List.copyOf(codes));
        }
        PERMISSION_CODES_BY_ROLE = Map.copyOf(codesByRole);
    }

    private StoreAccessPolicy() {}

    public static StoreRole requireRole(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("店员角色不能为空");
        }
        StoreRole role = ROLE_BY_NAME.get(value.trim().toUpperCase(Locale.ROOT));
        if (role == null) {
            throw new IllegalArgumentException("店员角色不合法");
        }
        return role;
    }

    public static List<String> permissionCodes(String roleCode, Integer status) {
        if (status == null || status != 1) {
            return List.of();
        }
        return PERMISSION_CODES_BY_ROLE.get(requireRole(roleCode));
    }

    public static boolean hasPermission(String roleCode, Integer status, String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return true;
        }
        if (status == null || status != 1) {
            return false;
        }
        StorePermission permission = permissionOrNull(permissionCode);
        if (permission == null) {
            return false;
        }
        return requireRole(roleCode).permissions().contains(permission);
    }

    public static StorePermission requirePermission(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("权限不能为空");
        }
        StorePermission permission = PERMISSION_BY_CODE.get(value.trim());
        if (permission == null) {
            throw new IllegalArgumentException("权限不合法");
        }
        return permission;
    }

    public static StorePermission permissionOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return PERMISSION_BY_CODE.get(value.trim());
    }

    public static boolean containsPermission(Set<StorePermission> permissions, Integer status, String permissionCode) {
        if (status == null || status != 1) {
            return false;
        }
        StorePermission permission = permissionOrNull(permissionCode);
        if (permission == null) {
            return false;
        }
        return permissions.contains(permission);
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
            StorePermission.USERS_MANAGE,
            StorePermission.CREDITS_VIEW,
            StorePermission.POSTERS_VIEW,
            StorePermission.POSTERS_WRITE
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
            StorePermission.USERS_MANAGE,
            StorePermission.CREDITS_VIEW,
            StorePermission.POSTERS_VIEW,
            StorePermission.POSTERS_WRITE
        )),
        SALES("销售员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.SALES_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.POSTERS_VIEW
        )),
        PURCHASING("采购员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.PURCHASE_VIEW,
            StorePermission.PURCHASE_WRITE,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.POSTERS_VIEW
        )),
        WAREHOUSE("仓库员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.ARCHIVES_VIEW,
            StorePermission.INVENTORY_VIEW,
            StorePermission.INVENTORY_WRITE,
            StorePermission.AGENT_VIEW,
            StorePermission.POSTERS_VIEW
        )),
        FINANCE("财务员工", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.FINANCE_VIEW,
            StorePermission.FINANCE_WRITE,
            StorePermission.REPORTS_VIEW,
            StorePermission.SALES_VIEW,
            StorePermission.PURCHASE_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.CREDITS_VIEW,
            StorePermission.POSTERS_VIEW
        )),
        ASSISTANT("AI/只读助理", Set.of(
            StorePermission.DASHBOARD_VIEW,
            StorePermission.REPORTS_VIEW,
            StorePermission.AGENT_VIEW,
            StorePermission.POSTERS_VIEW
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
        USERS_MANAGE("users:manage"),
        CREDITS_VIEW("credits:view"),
        POSTERS_VIEW("posters:view"),
        POSTERS_WRITE("posters:write");

        private final String code;

        StorePermission(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}

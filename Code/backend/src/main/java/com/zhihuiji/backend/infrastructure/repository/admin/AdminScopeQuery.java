package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import java.util.Set;

/** JPA-safe parameters derived from a server-owned administrator scope. */
public record AdminScopeQuery(
    boolean allOwners,
    Set<Long> ownerUserIds,
    Set<Long> storeIds,
    boolean allStores
) {
    private static final long EMPTY_SCOPE_SENTINEL = Long.MIN_VALUE;

    public AdminScopeQuery {
        ownerUserIds = normalize(ownerUserIds);
        storeIds = normalize(storeIds);
    }

    public static AdminScopeQuery from(AdminDataScope scope) {
        return new AdminScopeQuery(
            scope.allOwners(),
            scope.ownerUserIds(),
            scope.storeIds(),
            scope.allOwners() || scope.storeIds().isEmpty()
        );
    }

    private static Set<Long> normalize(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? Set.of(EMPTY_SCOPE_SENTINEL) : Set.copyOf(ids);
    }
}

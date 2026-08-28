package com.zhihuiji.backend.infrastructure.security.admin;

import java.util.Set;

/** Immutable owner/store visibility calculated by the server. */
public record AdminDataScope(Set<Long> ownerUserIds, Set<Long> storeIds) {
    public AdminDataScope {
        ownerUserIds = Set.copyOf(ownerUserIds == null ? Set.of() : ownerUserIds);
        storeIds = Set.copyOf(storeIds == null ? Set.of() : storeIds);
    }

    public static AdminDataScope empty() {
        return new AdminDataScope(Set.of(), Set.of());
    }
}

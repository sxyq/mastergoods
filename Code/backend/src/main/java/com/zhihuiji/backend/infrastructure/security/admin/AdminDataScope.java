package com.zhihuiji.backend.infrastructure.security.admin;

import java.util.Set;

/** Immutable owner/store visibility calculated by the server. */
public record AdminDataScope(
    boolean allOwners,
    Set<Long> ownerUserIds,
    Set<Long> storeIds,
    boolean includeInactive,
    ContentMode contentMode
) {
    public AdminDataScope {
        ownerUserIds = Set.copyOf(ownerUserIds == null ? Set.of() : ownerUserIds);
        storeIds = Set.copyOf(storeIds == null ? Set.of() : storeIds);
        contentMode = contentMode == null ? ContentMode.METADATA_ONLY : contentMode;
    }

    /** Compatibility constructor for the original two-set contract. */
    public AdminDataScope(Set<Long> ownerUserIds, Set<Long> storeIds) {
        this(false, ownerUserIds, storeIds, false, ContentMode.METADATA_ONLY);
    }

    public static AdminDataScope empty() {
        return new AdminDataScope(false, Set.of(), Set.of(), false, ContentMode.METADATA_ONLY);
    }

    public static AdminDataScope allOwners(boolean includeInactive, ContentMode contentMode) {
        return new AdminDataScope(true, Set.of(), Set.of(), includeInactive, contentMode);
    }

    public static AdminDataScope owners(Set<Long> ownerUserIds, boolean includeInactive, ContentMode contentMode) {
        return new AdminDataScope(false, ownerUserIds, Set.of(), includeInactive, contentMode);
    }

    public boolean allowsOwner(Long ownerUserId) {
        return ownerUserId != null && (allOwners || ownerUserIds.contains(ownerUserId));
    }

    public boolean allowsStore(Long storeId) {
        return storeId != null && (allOwners || storeIds.contains(storeId));
    }

    public enum ContentMode {
        METADATA_ONLY,
        REDACTED,
        AUTHORIZED
    }
}

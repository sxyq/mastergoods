package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zhihuiji.backend.infrastructure.security.admin.AdminDataScope;
import java.util.Set;

/** JSON-safe representation of the server-calculated administrator range. */
public final class AdminScopeDtos {
    private AdminScopeDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Scope(
        boolean allOwners,
        Set<String> ownerUserIds,
        Set<String> storeIds,
        boolean includeInactive,
        String contentMode
    ) {
        public Scope {
            ownerUserIds = Set.copyOf(ownerUserIds == null ? Set.of() : ownerUserIds);
            storeIds = Set.copyOf(storeIds == null ? Set.of() : storeIds);
        }

        public static Scope from(AdminDataScope scope) {
            return new Scope(
                scope.allOwners(),
                scope.ownerUserIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                scope.storeIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                scope.includeInactive(),
                scope.contentMode().name()
            );
        }
    }
}

package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

/** Planned projections for users, stores and memberships. */
public final class AdminOrganizationDtos {
    private AdminOrganizationDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserSummary(
        String userId,
        String phoneMasked,
        String nickname,
        String status,
        Instant createdAt,
        Instant updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StoreSummary(
        String storeId,
        String ownerUserId,
        String name,
        String status,
        int memberCount,
        Instant createdAt,
        Instant updatedAt
    ) {}
}

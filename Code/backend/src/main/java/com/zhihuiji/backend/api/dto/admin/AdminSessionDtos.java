package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Set;

/** DTOs for the planned GET /v2/admin/session endpoint. */
public final class AdminSessionDtos {
    private AdminSessionDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SessionResponse(
        String adminUserId,
        String role,
        Set<String> permissions,
        Set<String> ownerUserIds,
        Set<String> storeIds,
        boolean contentAccess,
        String scopeCompleteness
    ) {}
}

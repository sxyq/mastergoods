package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

/** Write-once administrator audit projection. */
public final class AdminAuditDtos {
    private AdminAuditDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Event(
        String eventId,
        String action,
        String actorAdminUserId,
        String resourceType,
        String resourceId,
        String result,
        String reason,
        Instant occurredAt
    ) {}
}

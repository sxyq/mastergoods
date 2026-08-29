package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

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
        Instant occurredAt,
        String role,
        String ownerUserId,
        String storeId,
        String sourceIp,
        String userAgentSummary,
        String requestId,
        String summary
    ) {
        public Event(
            String eventId,
            String action,
            String actorAdminUserId,
            String resourceType,
            String resourceId,
            String result,
            String reason,
            Instant occurredAt
        ) {
            this(eventId, action, actorAdminUserId, resourceType, resourceId, result, reason, occurredAt,
                null, null, null, null, null, null, null);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Query(
        String action,
        String resourceType,
        String result,
        Instant from,
        Instant to,
        Integer page,
        Integer size,
        String eventId
    ) {
        public Query(String action, String resourceType, String result, Instant from, Instant to, Integer page, Integer size) {
            this(action, resourceType, result, from, to, page, size, null);
        }
    }
}

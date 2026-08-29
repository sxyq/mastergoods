package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

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
        Instant updatedAt,
        long version
    ) {
        public UserSummary(String userId, String phoneMasked, String nickname, String status, Instant createdAt, Instant updatedAt) {
            this(userId, phoneMasked, nickname, status, createdAt, updatedAt, 0L);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StoreSummary(
        String storeId,
        String ownerUserId,
        String name,
        String status,
        int memberCount,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        public StoreSummary(String storeId, String ownerUserId, String name, String status, int memberCount, Instant createdAt, Instant updatedAt) {
            this(storeId, ownerUserId, name, status, memberCount, createdAt, updatedAt, 0L);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MemberSummary(
        String userId,
        String storeId,
        String nickname,
        String phoneMasked,
        String role,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserPatchRequest(
        String nickname,
        Integer status,
        Boolean keepSessions,
        Long expectedVersion,
        String idempotencyKey,
        String reason,
        Boolean confirmed,
        Long ownerUserId,
        Long storeId
    ) {
        public UserPatchRequest(String nickname, Integer status, Boolean keepSessions, Long expectedVersion,
                                String idempotencyKey, String reason, Boolean confirmed) {
            this(nickname, status, keepSessions, expectedVersion, idempotencyKey, reason, confirmed, null, null);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StorePatchRequest(
        String name,
        Integer status,
        Long expectedVersion,
        String idempotencyKey,
        String reason,
        Boolean confirmed,
        Long ownerUserId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MemberPatchRequest(
        String nickname,
        String role,
        String title,
        Integer status,
        Boolean keepSessions,
        Long expectedVersion,
        String idempotencyKey,
        String reason,
        Boolean confirmed
    ) {}
}

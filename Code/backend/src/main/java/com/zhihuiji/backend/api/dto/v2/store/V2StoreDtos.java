package com.zhihuiji.backend.api.dto.v2.store;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class V2StoreDtos {
    private V2StoreDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CurrentStoreResponse(
        Long storeId,
        String storeName,
        Long ownerUserId,
        Long currentUserId,
        String currentUserName,
        String currentUserPhone,
        String role,
        String title,
        Integer status,
        List<String> permissions,
        Integer memberCount,
        Integer enabledMemberCount,
        Integer disabledMemberCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MemberResponse(
        Long userId,
        String phone,
        String nickname,
        String role,
        String title,
        Integer status,
        List<String> permissions,
        Long createdAt,
        Long updatedAt,
        Long activeSessions,
        Long storeId,
        String storeName
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MemberCreateRequest(
        @NotBlank String phone,
        @NotBlank String password,
        @NotBlank String nickname,
        @NotBlank String role,
        String title,
        Integer status
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MemberUpdateRequest(
        String nickname,
        String password,
        String role,
        String title,
        Integer status,
        @NotNull Boolean keepSessions
    ) {}
}

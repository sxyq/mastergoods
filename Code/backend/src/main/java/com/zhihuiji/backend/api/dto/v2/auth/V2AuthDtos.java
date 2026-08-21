package com.zhihuiji.backend.api.dto.v2.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

public final class V2AuthDtos {
    private V2AuthDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RegisterRequest(
        @NotBlank String phone,
        @NotBlank String password,
        @NotBlank String verifyCode
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LoginRequest(
        @NotBlank String phone,
        @NotBlank String password
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RefreshRequest(@NotBlank String refreshToken) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record VerifyCodeRequest(@NotBlank String phone, @NotBlank String type) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record VerifyCodeResponse(boolean success, int expireSeconds) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AuthResponse(
        Long userId,
        String token,
        String refreshToken,
        int expiresIn
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserProfileResponse(
        Long id,
        String phone,
        String nickname,
        Integer status
    ) {}
}

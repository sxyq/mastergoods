package com.zhihuiji.backend.api.dto.v2.sync;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

public final class V2ImportJobDtos {
    private V2ImportJobDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportJobResponse(
        Long id,
        String clientId,
        String sourceType,
        String sourceUri,
        String sourceChecksum,
        String idempotencyKey,
        String status,
        String stage,
        Integer retryCount,
        String replayCursor,
        String summaryJson,
        String optionsJson,
        String failureCode,
        String failureMessage,
        Long createdAt,
        Long updatedAt,
        Long startedAt,
        Long finishedAt,
        Long lastHeartbeatAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportJobCreateRequest(
        @NotBlank String clientId,
        @NotBlank String sourceType,
        String sourceUri,
        String sourceChecksum,
        String idempotencyKey,
        String replayCursor,
        String optionsJson
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportJobRetryRequest(
        String replayCursor
    ) {}
}

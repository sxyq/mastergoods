package com.zhihuiji.backend.api.dto.v2.sync;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class V2SyncDtos {
    private V2SyncDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncHealthResponse(
        String status,
        String message,
        Boolean ownerScoped,
        Long serverTime,
        List<String> supportedEntityTypes,
        List<String> uploadableEntityTypes
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncCursorResponse(
        String clientId,
        String lastCursor,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncCursorAckRequest(
        @NotBlank String clientId,
        @NotBlank String cursor
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncChangeDto(
        String operationId,
        @NotBlank String entityType,
        @NotBlank String entityId,
        @NotBlank String operation,
        String payload,
        Long updatedAt,
        Long baseVersion
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncUploadRequest(
        @NotBlank String clientId,
        @NotNull List<SyncChangeDto> changes,
        String lastSyncCursor
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncUploadResponse(
        Integer acceptedCount,
        Integer failedCount,
        String status,
            String nextCursor,
            List<String> acceptedOperationIds,
            List<String> failedOperationIds,
            List<SyncOperationFailureResponse> failures,
            List<SyncOperationResultResponse> operationResults
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncOperationFailureResponse(String operationId, String code, String message) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncOperationResultResponse(
        String operationId,
        String status,
        String code,
        String message,
        Long serverVersion,
        List<String> conflictFields,
        String serverPayload
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncPullRequest(
        @NotBlank String clientId,
        String sinceCursor,
        Integer limit
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncPullResponse(
        List<SyncChangeDto> changes,
        String effectiveCursor,
        String nextCursor,
        Boolean hasMore
    ) {}
}

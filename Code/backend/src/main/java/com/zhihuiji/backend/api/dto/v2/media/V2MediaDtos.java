package com.zhihuiji.backend.api.dto.v2.media;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class V2MediaDtos {
    private V2MediaDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MediaAssetResponse(
        Long id,
        String assetType,
        String storageProvider,
        String bucketName,
        String objectKey,
        String originalFileName,
        String mimeType,
        Long sizeBytes,
        String checksum,
        Integer width,
        Integer height,
        String metadataJson,
        Long createdAt,
        Long updatedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MediaAssetCreateRequest(
        @NotBlank String assetType,
        @NotBlank String storageProvider,
        String bucketName,
        @NotBlank String objectKey,
        @NotBlank String originalFileName,
        @NotBlank String mimeType,
        @NotNull Long sizeBytes,
        String checksum,
        Integer width,
        Integer height,
        String metadataJson
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MediaBindingResponse(
        Long id,
        Long assetId,
        String targetType,
        Long targetId,
        Integer sortOrder,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MediaBindingCreateRequest(
        @NotNull Long assetId,
        @NotBlank String targetType,
        @NotNull Long targetId,
        Integer sortOrder
    ) {}

}

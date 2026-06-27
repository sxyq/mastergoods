package com.zhihuiji.backend.api.dto.v2.poster;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class V2PosterDtos {
    private V2PosterDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PosterGenerateRequest(
        @NotNull Long productId,
        @NotBlank String prompt,
        List<String> referenceAssetIds
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PosterGenerationResponse(
        Long id,
        Long productId,
        String promptText,
        String resultImageUrl,
        String status,
        BigDecimal creditsCost,
        Integer iteration,
        Long createdAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreditBalanceResponse(
        BigDecimal balance,
        BigDecimal totalRecharged,
        BigDecimal totalConsumed
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreditTransactionResponse(
        Long id,
        BigDecimal amount,
        String type,
        String refType,
        Long refId,
        String note,
        Long createdAt
    ) {}
}

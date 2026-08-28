package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class AdminRetentionDtos {
    private AdminRetentionDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Policy(
        long version,
        int auditDays,
        int messageDays,
        int toolResultDays,
        int metricsDays,
        String contentMode,
        Instant effectiveAt,
        String updatedBy
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateRequest(
        Integer auditDays,
        Integer messageDays,
        Integer toolResultDays,
        Integer metricsDays,
        String contentMode,
        @NotNull Long expectedVersion,
        @NotBlank String idempotencyKey,
        @NotBlank String reason,
        @NotNull Boolean confirmed
    ) {}
}

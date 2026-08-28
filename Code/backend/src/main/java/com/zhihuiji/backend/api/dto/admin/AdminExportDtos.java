package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public final class AdminExportDtos {
    private AdminExportDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Job(
        String exportId,
        String exportType,
        List<String> fields,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Instant completedAt,
        String downloadUrl,
        boolean contentRedacted,
        String errorSummary,
        int downloadCount
    ) {
        public Job {
            fields = List.copyOf(fields == null ? List.of() : fields);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRequest(
        @NotBlank String exportType,
        @NotEmpty List<String> fields,
        Instant from,
        Instant to,
        Long ownerUserId,
        Long storeId,
        @NotBlank String reason,
        @NotBlank String idempotencyKey
    ) {}
}

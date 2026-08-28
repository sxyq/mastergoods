package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Configuration metadata only; provider secrets are intentionally absent. */
public final class AdminConfigDtos {
    private AdminConfigDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConfigResponse(
        String modelId,
        boolean agentEnabled,
        List<String> enabledTools,
        long version,
        String effectiveState,
        java.time.Instant effectiveAt,
        String updatedBy
    ) {
        public ConfigResponse(String modelId, boolean agentEnabled, long version, String effectiveState) {
            this(modelId, agentEnabled, List.of(), version, effectiveState, null, null);
        }
        public ConfigResponse {
            enabledTools = List.copyOf(enabledTools == null ? List.of() : enabledTools);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateRequest(
        String modelId,
        Boolean agentEnabled,
        List<String> enabledTools,
        @NotNull Long expectedVersion,
        @NotBlank String idempotencyKey,
        @NotBlank String reason,
        @NotNull Boolean confirmed,
        Long ownerUserId,
        Long storeId
    ) {}
}

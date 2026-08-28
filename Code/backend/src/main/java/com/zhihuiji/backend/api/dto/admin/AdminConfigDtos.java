package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Configuration metadata only; provider secrets are intentionally absent. */
public final class AdminConfigDtos {
    private AdminConfigDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConfigResponse(
        String modelId,
        boolean agentEnabled,
        long version,
        String effectiveState
    ) {}
}

package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

public final class AdminSystemDtos {
    private AdminSystemDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HealthResponse(
        String status,
        String version,
        Instant generatedAt,
        List<Component> components
    ) {
        public HealthResponse {
            components = List.copyOf(components == null ? List.of() : components);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Component(String name, String status, String detail) {}
}

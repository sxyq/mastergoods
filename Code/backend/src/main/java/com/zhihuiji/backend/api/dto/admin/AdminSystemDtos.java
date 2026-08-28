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
        List<Component> components,
        List<ErrorSummary> errors
    ) {
        public HealthResponse(String status, String version, Instant generatedAt, List<Component> components) {
            this(status, version, generatedAt, components, List.of());
        }
        public HealthResponse {
            components = List.copyOf(components == null ? List.of() : components);
            errors = List.copyOf(errors == null ? List.of() : errors);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Component(String serviceName, String status, String version, Instant checkedAt, String errorSummary, Long queueDepth) {
        public Component(String serviceName, String status, String version, Instant checkedAt, String errorSummary) {
            this(serviceName, status, version, checkedAt, errorSummary, null);
        }
        public Component(String name, String status, String detail) {
            this(name, status, null, Instant.now(), detail, null);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ErrorSummary(String component, String category, String summary, Instant occurredAt) {}
}

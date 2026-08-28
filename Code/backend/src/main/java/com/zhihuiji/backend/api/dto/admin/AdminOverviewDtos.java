package com.zhihuiji.backend.api.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

/** Read-only overview projections; no JPA entity is exposed. */
public final class AdminOverviewDtos {
    private AdminOverviewDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record OverviewResponse(
        Instant from,
        Instant to,
        List<Metric> metrics,
        List<TrendPoint> trend,
        boolean estimated,
        String scopeCompleteness,
        Instant generatedAt,
        AdminScopeDtos.Scope scope
    ) {
        public OverviewResponse {
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
            trend = List.copyOf(trend == null ? List.of() : trend);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Metric(String key, long value, String unit) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TrendPoint(Instant at, long value) {}
}

package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class AlertDtos {
    private AlertDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AlertDashboardDto(
        List<AlertDto> alerts
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AlertDto(
        String id,
        String type,
        String severity,
        String title,
        String description,
        String recommendedAction,
        String entityName,
        Long entityId,
        Double metric
    ) {}
}

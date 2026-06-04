package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class AgentTaskDtos {
    private AgentTaskDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskSummaryDto(
        Long id,
        String taskType,
        String title,
        String status,
        String triggerSource,
        int progress,
        long createdAt,
        long updatedAt,
        Long completedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskMetricDto(
        String label,
        String value,
        String delta,
        String emphasis
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskSectionDto(
        String title,
        String narrative,
        List<String> bullets
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskTableDto(
        String title,
        List<String> columns,
        List<List<String>> rows
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskChartSeriesDto(
        String name,
        List<Double> values
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskChartDto(
        String title,
        String chartType,
        List<String> categories,
        List<AgentTaskChartSeriesDto> series
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentRenderBlockDto(
        String type,
        String title,
        String subtitle,
        String tone,
        String text,
        List<String> bullets,
        List<AgentTaskMetricDto> metrics,
        AgentTaskTableDto table,
        AgentTaskChartDto chart,
        OperationDraftDtos.OperationDraftDto draft
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskResultDto(
        String title,
        String subtitle,
        String summary,
        List<AgentTaskMetricDto> metrics,
        List<AgentTaskSectionDto> sections,
        List<AgentTaskTableDto> tables,
        List<AgentTaskChartDto> charts,
        List<String> suggestedActions,
        OperationDraftDtos.OperationDraftDto draft,
        List<AgentRenderBlockDto> renderBlocks
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentTaskDetailDto(
        AgentTaskSummaryDto task,
        String input,
        AgentTaskResultDto result
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentNotificationDto(
        Long id,
        String title,
        String body,
        String level,
        Long taskId,
        boolean isRead,
        boolean isDelivered,
        long createdAt
    ) {}
}

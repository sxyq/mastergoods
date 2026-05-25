package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class AgentDto {
    private AgentDto() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentWorkbenchDto(
        ReconciliationFollowupDto reconciliation,
        ReportInsightDto reportInsight,
        AlertDashboardDto alerts,
        List<String> suggestedQuestions,
        List<String> suggestedInstructions,
        List<AgentRenderBlockDto> overviewBlocks,
        List<AgentRenderBlockDto> instantBlocks,
        List<AgentAnswerDto> proactiveAnswers,
        List<OperationDraftDto> proactiveDrafts
    ) {
        public AgentWorkbenchDto(
            ReconciliationFollowupDto reconciliation,
            ReportInsightDto reportInsight,
            AlertDashboardDto alerts,
            List<String> suggestedQuestions,
            List<String> suggestedInstructions
        ) {
            this(
                reconciliation,
                reportInsight,
                alerts,
                suggestedQuestions,
                suggestedInstructions,
                List.of(),
                List.of(),
                List.of(),
                List.of()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record ReconciliationFollowupDto(
        double totalReceivable,
        double totalPayable,
        double totalReceived,
        double totalPaid,
        double netCashFlow,
        List<FollowupPartyDto> receivableCustomers,
        List<FollowupPartyDto> payableSuppliers,
        List<AgingRiskDto> agingRisks
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record FollowupPartyDto(
        Long entityId,
        String entityType,
        String name,
        String phone,
        double amount,
        String actionLabel
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgingRiskDto(
        String entityType,
        Long entityId,
        String name,
        String orderNo,
        long createdAt,
        long ageDays,
        double amount,
        String summary,
        String suggestedAction
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record ReportInsightDto(
        String periodLabel,
        double currentSales,
        double previousSales,
        double salesChangeRate,
        String narrative,
        String leadingProductName,
        double leadingProductAmount,
        String leadingCustomerName,
        double leadingCustomerAmount,
        List<String> highlights,
        List<String> suggestedActions
    ) {}

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

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentAnswerDto(
        String query,
        String intent,
        String answer,
        List<String> highlights,
        List<String> columns,
        List<List<String>> rows,
        List<String> suggestedActions
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationDraftDto(
        String operationType,
        String summary,
        String partnerRole,
        Long partnerId,
        String partnerName,
        List<OperationDraftItemDto> items,
        String notes,
        boolean canSubmit,
        List<String> warnings,
        List<String> suggestedActions
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationDraftItemDto(
        Long productId,
        String productCode,
        String productName,
        double quantity,
        double unitPrice,
        double amount,
        double currentStock
    ) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record OperationSubmitResultDto(
        String operationType,
        Long orderId,
        String orderNo,
        String message,
        String nextAction
    ) {}

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
        OperationDraftDto draft
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
        OperationDraftDto draft,
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

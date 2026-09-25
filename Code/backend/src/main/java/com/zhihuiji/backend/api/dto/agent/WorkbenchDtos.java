// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.api.dto.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public final class WorkbenchDtos {
    private WorkbenchDtos() {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AgentWorkbenchDto(
        ReconciliationDtos.ReconciliationFollowupDto reconciliation,
        ReconciliationDtos.ReportInsightDto reportInsight,
        AlertDtos.AlertDashboardDto alerts,
        List<String> suggestedQuestions,
        List<String> suggestedInstructions,
        List<String> overviewBlocks,
        List<String> instantBlocks,
        List<AnswerDtos.AgentAnswerDto> proactiveAnswers,
        List<OperationDraftDtos.OperationDraftDto> proactiveDrafts
    ) {
        public AgentWorkbenchDto(
            ReconciliationDtos.ReconciliationFollowupDto reconciliation,
            ReconciliationDtos.ReportInsightDto reportInsight,
            AlertDtos.AlertDashboardDto alerts,
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
}

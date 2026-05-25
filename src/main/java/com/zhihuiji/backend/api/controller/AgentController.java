package com.zhihuiji.backend.api.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.application.service.LlmDrivenAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/agent")
public class AgentController {
    private final LlmDrivenAgentService agentService;

    public AgentController(LlmDrivenAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/workbench")
    public ApiResponse<AgentDto.AgentWorkbenchDto> workbench(
        @RequestParam(value = "window_days", defaultValue = "7") int windowDays,
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        return ApiResponse.success(agentService.getWorkbench(windowDays, limit, agingDays));
    }

    @GetMapping("/reconciliation-followup")
    public ApiResponse<AgentDto.ReconciliationFollowupDto> reconciliationFollowup(
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        return ApiResponse.success(agentService.getReconciliationFollowup(limit, agingDays));
    }

    @GetMapping("/report-insight")
    public ApiResponse<AgentDto.ReportInsightDto> reportInsight(
        @RequestParam(value = "window_days", defaultValue = "7") int windowDays
    ) {
        return ApiResponse.success(agentService.getReportInsight(windowDays));
    }

    @GetMapping("/alerts")
    public ApiResponse<AgentDto.AlertDashboardDto> alerts(
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        return ApiResponse.success(agentService.getAlerts(limit, agingDays));
    }

    @PostMapping("/query")
    public ApiResponse<AgentDto.AgentAnswerDto> query(@RequestBody QueryRequest request) {
        return ApiResponse.success(agentService.answerQuestion(request.query()));
    }

    @PostMapping("/operation-draft")
    public ApiResponse<AgentDto.OperationDraftDto> operationDraft(@RequestBody DraftRequest request) {
        return ApiResponse.success(agentService.draftOperation(request.instruction()));
    }

    @PostMapping("/operation-submit")
    public ApiResponse<AgentDto.OperationSubmitResultDto> operationSubmit(@RequestBody SubmitRequest request) {
        return ApiResponse.success(agentService.submitDraft(request.draft()));
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record QueryRequest(String query) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record DraftRequest(String instruction) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record SubmitRequest(AgentDto.OperationDraftDto draft) {}
}

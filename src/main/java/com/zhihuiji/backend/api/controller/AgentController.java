package com.zhihuiji.backend.api.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.agent.*;
import com.zhihuiji.backend.application.service.LlmDrivenAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    public ApiResponse<WorkbenchDtos.AgentWorkbenchDto> workbench(
        @RequestParam(value = "window_days", defaultValue = "7") int windowDays,
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        if (windowDays < 1 || windowDays > 365) windowDays = 7;
        if (limit < 1 || limit > 50) limit = 6;
        if (agingDays < 1 || agingDays > 365) agingDays = 15;
        return ApiResponse.success(agentService.getWorkbench(windowDays, limit, agingDays));
    }

    @GetMapping("/reconciliation-followup")
    public ApiResponse<ReconciliationDtos.ReconciliationFollowupDto> reconciliationFollowup(
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        if (limit < 1 || limit > 50) limit = 6;
        if (agingDays < 1 || agingDays > 365) agingDays = 15;
        return ApiResponse.success(agentService.getReconciliationFollowup(limit, agingDays));
    }

    @GetMapping("/report-insight")
    public ApiResponse<ReconciliationDtos.ReportInsightDto> reportInsight(
        @RequestParam(value = "window_days", defaultValue = "7") int windowDays
    ) {
        if (windowDays < 1 || windowDays > 365) windowDays = 7;
        return ApiResponse.success(agentService.getReportInsight(windowDays));
    }

    @GetMapping("/alerts")
    public ApiResponse<AlertDtos.AlertDashboardDto> alerts(
        @RequestParam(value = "limit", defaultValue = "6") int limit,
        @RequestParam(value = "aging_days", defaultValue = "15") int agingDays
    ) {
        if (limit < 1 || limit > 50) limit = 6;
        if (agingDays < 1 || agingDays > 365) agingDays = 15;
        return ApiResponse.success(agentService.getAlerts(limit, agingDays));
    }

    @PostMapping("/query")
    public ApiResponse<AnswerDtos.AgentAnswerDto> query(@Valid @RequestBody QueryRequest request) {
        return ApiResponse.success(agentService.answerQuestion(request.query()));
    }

    @PostMapping("/operation-draft")
    public ApiResponse<OperationDraftDtos.OperationDraftDto> operationDraft(@Valid @RequestBody DraftRequest request) {
        return ApiResponse.success(agentService.draftOperation(request.instruction()));
    }

    @PostMapping("/operation-submit")
    public ApiResponse<OperationDraftDtos.OperationSubmitResultDto> operationSubmit(@Valid @RequestBody SubmitRequest request) {
        return ApiResponse.success(agentService.submitDraft(request.draft(), request.idempotencyKey()));
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record QueryRequest(@NotBlank String query) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record DraftRequest(@NotBlank String instruction) {}

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record SubmitRequest(@NotNull OperationDraftDtos.OperationDraftDto draft, @NotBlank String idempotencyKey) {}
}

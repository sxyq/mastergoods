package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminAgentDtos;
import com.zhihuiji.backend.api.dto.admin.AdminPageDtos;
import com.zhihuiji.backend.application.service.admin.AdminAgentObservabilityService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-05 Agent run list and detail endpoints. */
@RestController
@RequestMapping("/v2/admin/agent")
public class AdminAgentController {
    private final AdminAgentObservabilityService observabilityService;
    private final AdminPrincipalResolver principalResolver;

    public AdminAgentController(
        AdminAgentObservabilityService observabilityService,
        AdminPrincipalResolver principalResolver
    ) {
        this.observabilityService = observabilityService;
        this.principalResolver = principalResolver;
    }

    @GetMapping("/runs")
    public ApiResponse<AdminPageDtos.PageResponse<AdminAgentDtos.RunSummary>> runs(
        @RequestParam(value = "runId", required = false) String runId,
        @RequestParam(value = "conversationId", required = false) Long conversationId,
        @RequestParam(value = "terminalStatus", required = false) String terminalStatus,
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        return ApiResponse.success(
            observabilityService.listRuns(
                principalResolver.requireCurrent(), runId, conversationId, terminalStatus,
                from, to, ownerUserId, storeId, page, size
            )
        );
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AdminAgentDtos.RunSummary> run(
        @PathVariable String runId,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(
            observabilityService.getRun(
                principalResolver.requireCurrent(), runId, ownerUserId, storeId
            )
        );
    }
}

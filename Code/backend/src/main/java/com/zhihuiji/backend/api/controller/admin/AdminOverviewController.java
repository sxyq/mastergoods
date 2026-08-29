package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminOverviewDtos;
import com.zhihuiji.backend.application.service.admin.AdminOverviewService;
import com.zhihuiji.backend.application.service.admin.AdminAuditService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

/** API-ADM-02 platform metrics and trend endpoint. */
@RestController
@RequestMapping("/v2/admin")
public class AdminOverviewController {
    private final AdminOverviewService overviewService;
    private final AdminPrincipalResolver principalResolver;
    private final AdminAuditService auditService;

    @Autowired
    public AdminOverviewController(
        AdminOverviewService overviewService,
        AdminPrincipalResolver principalResolver,
        AdminAuditService auditService
    ) {
        this.overviewService = overviewService;
        this.principalResolver = principalResolver;
        this.auditService = auditService;
    }

    public AdminOverviewController(AdminOverviewService overviewService, AdminPrincipalResolver principalResolver) {
        this(overviewService, principalResolver, null);
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewDtos.OverviewResponse> overview(
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        var principal = principalResolver.requireCurrent();
        var response = overviewService.overview(principal, from, to, ownerUserId, storeId);
        if (auditService != null) auditService.recordRead(principal, "admin.overview.read", "OVERVIEW", null, ownerUserId, storeId, "overview");
        return ApiResponse.success(response);
    }
}

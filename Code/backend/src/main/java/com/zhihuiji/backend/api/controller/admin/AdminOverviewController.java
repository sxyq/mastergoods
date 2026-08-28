package com.zhihuiji.backend.api.controller.admin;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.dto.admin.AdminOverviewDtos;
import com.zhihuiji.backend.application.service.admin.AdminOverviewService;
import com.zhihuiji.backend.infrastructure.security.admin.AdminPrincipalResolver;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-ADM-02 platform metrics and trend endpoint. */
@RestController
@RequestMapping("/v2/admin")
public class AdminOverviewController {
    private final AdminOverviewService overviewService;
    private final AdminPrincipalResolver principalResolver;

    public AdminOverviewController(
        AdminOverviewService overviewService,
        AdminPrincipalResolver principalResolver
    ) {
        this.overviewService = overviewService;
        this.principalResolver = principalResolver;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewDtos.OverviewResponse> overview(
        @RequestParam(value = "from", required = false) Instant from,
        @RequestParam(value = "to", required = false) Instant to,
        @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
        @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ApiResponse.success(
            overviewService.overview(principalResolver.requireCurrent(), from, to, ownerUserId, storeId)
        );
    }
}
